// src/main/java/com/suvikapay/wallet/service/impl/AuthServiceImpl.java
package com.suvikapay.wallet.service.impl;

import com.suvikapay.wallet.dto.request.AuthRequest;
import com.suvikapay.wallet.dto.request.CreateAdminRequest;
import com.suvikapay.wallet.dto.request.CreateUserRequest;
import com.suvikapay.wallet.dto.request.RefreshTokenRequest;
import com.suvikapay.wallet.dto.response.AuthResponse;
import com.suvikapay.wallet.dto.response.UserResponse;
import com.suvikapay.wallet.entity.AppUser;
import com.suvikapay.wallet.entity.Merchant;
import com.suvikapay.wallet.entity.Wallet;
import com.suvikapay.wallet.exception.ResourceAlreadyExistsException;
import com.suvikapay.wallet.exception.ResourceNotFoundException;
import com.suvikapay.wallet.exception.UnauthorizedException;
import com.suvikapay.wallet.repository.AppUserRepository;
import com.suvikapay.wallet.repository.MerchantRepository;
import com.suvikapay.wallet.repository.WalletRepository;
import com.suvikapay.wallet.service.AuthService;
import com.suvikapay.wallet.service.JwtService;
import com.suvikapay.wallet.service.UserDetailsServiceImpl;
import com.suvikapay.wallet.util.IPUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final WalletRepository walletRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse authenticate(AuthRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            AppUser user = userDetailsService.loadUserEntityByUsername(request.getEmail());
            user.setLastLogin(OffsetDateTime.now());
            userRepository.save(user);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            log.info("User {} logged in successfully from IP: {}",
                    user.getEmailAddress(), IPUtils.getClientIP(httpRequest));

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtService instanceof JwtServiceImpl ?
                            ((JwtServiceImpl) jwtService).getJwtExpiration() / 1000 : 86400)
                    .user(mapToUserResponse(user))
                    .build();

        } catch (Exception e) {
            log.error("Authentication failed for user: {}", request.getEmail(), e);
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        try {
            // In a real implementation, you would validate the refresh token
            // For now, we'll generate a new access token based on current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new UnauthorizedException("User not authenticated");
            }

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String newAccessToken = jwtService.generateToken(userDetails);

            AppUser user = userDetailsService.loadUserEntityByUsername(userDetails.getUsername());

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(request.getRefreshToken()) // Return same refresh token
                    .expiresIn(jwtService instanceof JwtServiceImpl ?
                            ((JwtServiceImpl) jwtService).getJwtExpiration() / 1000 : 86400)
                    .user(mapToUserResponse(user))
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new UnauthorizedException("Token refresh failed");
        }
    }

    @Override
    @Transactional
    public UserResponse register(CreateUserRequest request, HttpServletRequest httpRequest) {
        // Check if email already exists
        if (userRepository.existsByEmailAddress(request.getEmail())) {
            throw new ResourceAlreadyExistsException("User", "email", request.getEmail());
        }

        // Check if username already exists
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new ResourceAlreadyExistsException("User", "username", request.getUserName());
        }

        // Validate role
        validateRole(request.getRole());

        // Create user entity
        AppUser user = AppUser.builder()
                .emailAddress(request.getEmail())
                .userName(request.getUserName())
                .name(request.getName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole().toUpperCase())
                .userType(request.getUserType())
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Set merchants if provided
        if (request.getPayingMerchantId() != null) {
            Merchant payingMerchant = merchantRepository.findById(request.getPayingMerchantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", request.getPayingMerchantId()));
            user.setPayingMerchant(payingMerchant);
        }

        if (request.getPayoutMerchantId() != null) {
            Merchant payoutMerchant = merchantRepository.findById(request.getPayoutMerchantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", request.getPayoutMerchantId()));
            user.setPayoutMerchant(payoutMerchant);
        }

        // Save user
        AppUser savedUser = userRepository.save(user);

        // Create wallet for the user
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .currentBalance(java.math.BigDecimal.ZERO)
                .updatedAt(OffsetDateTime.now())
                .build();
        walletRepository.save(wallet);

        log.info("User {} registered successfully from IP: {}",
                user.getEmailAddress(), IPUtils.getClientIP(httpRequest));

        return mapToUserResponse(savedUser);
    }

    @Override
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        log.info("User logged out from IP: {}", IPUtils.getClientIP(request));
    }

    @Override
    @Transactional
    public UserResponse registerAdmin(CreateAdminRequest request, HttpServletRequest httpRequest) {

        if (userRepository.existsByEmailAddress(request.getEmail())) {
            throw new ResourceAlreadyExistsException("User", "email", request.getEmail());
        }

        if (userRepository.existsByUserName(request.getUserName())) {
            throw new ResourceAlreadyExistsException("User", "username", request.getUserName());
        }

        AppUser admin = AppUser.builder()
                .emailAddress(request.getEmail())
                .userName(request.getUserName())
                .name(request.getName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ADMIN")
                .userType(request.getUserType())
                .isActive(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        AppUser savedAdmin = userRepository.save(admin);

        // Optional: create wallet (safe to keep consistency)
        Wallet wallet = Wallet.builder()
                .user(savedAdmin)
                .currentBalance(java.math.BigDecimal.ZERO)
                .updatedAt(OffsetDateTime.now())
                .build();
        walletRepository.save(wallet);

        log.info("ADMIN {} registered from IP {}",
                savedAdmin.getEmailAddress(),
                IPUtils.getClientIP(httpRequest));

        return mapToUserResponse(savedAdmin);
    }


    @Override
    @Transactional
    public void changePassword(String currentPassword, String newPassword, String email) {
        AppUser user = userDetailsService.loadUserEntityByUsername(email);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", email);
    }

    private void validateRole(String role) {
        if (!role.equalsIgnoreCase("ADMIN") &&
                !role.equalsIgnoreCase("AGENT") &&
                !role.equalsIgnoreCase("USER")) {
            throw new IllegalArgumentException("Invalid role. Allowed roles: ADMIN, AGENT, USER");
        }
    }

    private UserResponse mapToUserResponse(AppUser user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .emailAddress(user.getEmailAddress())
                .userName(user.getUserName())
                .name(user.getName())
                .role(user.getRole())
                .userType(user.getUserType())
                .isActive(user.getIsActive())
                .payingApiStatus(user.getPayingApiStatus())
                .payoutApiStatus(user.getPayoutApiStatus())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .payingMerchant(user.getPayingMerchant() != null ?
                        UserResponse.MerchantResponse.builder()
                                .merchantId(user.getPayingMerchant().getMerchantId())
                                .merchantName(user.getPayingMerchant().getMerchantName())
                                .build() : null)
                .payoutMerchant(user.getPayoutMerchant() != null ?
                        UserResponse.MerchantResponse.builder()
                                .merchantId(user.getPayoutMerchant().getMerchantId())
                                .merchantName(user.getPayoutMerchant().getMerchantName())
                                .build() : null)
                .build();
    }
}