// src/main/java/com/suvikapay/wallet/service/impl/UserServiceImpl.java
package com.suvikapay.wallet.service.impl;

import com.suvikapay.wallet.dto.request.CreateUserRequest;
import com.suvikapay.wallet.dto.response.UserResponse;
import com.suvikapay.wallet.entity.AppUser;
import com.suvikapay.wallet.entity.Merchant;
import com.suvikapay.wallet.entity.Wallet;
import com.suvikapay.wallet.exception.ResourceAlreadyExistsException;
import com.suvikapay.wallet.exception.ResourceNotFoundException;
import com.suvikapay.wallet.exception.ServiceException;
import com.suvikapay.wallet.exception.UnauthorizedException;
import com.suvikapay.wallet.repository.AppUserRepository;
import com.suvikapay.wallet.repository.MerchantRepository;
import com.suvikapay.wallet.repository.WalletRepository;
import com.suvikapay.wallet.service.UserService;
import com.suvikapay.wallet.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final AppUserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        try {
            validateCurrentUserPermission(request.getRole());

            // Check if email already exists
            if (userRepository.existsByEmailAddress(request.getEmail())) {
                throw new ResourceAlreadyExistsException("User", "email", request.getEmail());
            }

            // Check if username already exists
            if (userRepository.existsByUserName(request.getUserName())) {
                throw new ResourceAlreadyExistsException("User", "username", request.getUserName());
            }

            // Validate role based on current user's role
            validateUserCreationRole(request.getRole());

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

            // Set merchants if provided and authorized
            if (request.getPayingMerchantId() != null) {
                Merchant payingMerchant = merchantRepository.findById(request.getPayingMerchantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", request.getPayingMerchantId()));

                // Validate if current user can assign this merchant
                validateMerchantAssignment(payingMerchant);
                user.setPayingMerchant(payingMerchant);
            }

            if (request.getPayoutMerchantId() != null) {
                Merchant payoutMerchant = merchantRepository.findById(request.getPayoutMerchantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", request.getPayoutMerchantId()));

                // Validate if current user can assign this merchant
                validateMerchantAssignment(payoutMerchant);
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

            log.info("User created successfully: {} with role: {}",
                    savedUser.getEmailAddress(), savedUser.getRole());

            return mapToUserResponse(savedUser);

        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            throw new ServiceException("Failed to create user: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer userId) {
        try {
            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            // Check if current user has permission to view this user
            validateUserAccess(user);

            return mapToUserResponse(user);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching user by ID: {}", userId, e);
            throw new ServiceException("Failed to fetch user: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        try {
            AppUser user = userRepository.findByEmailAddress(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

            // Check if current user has permission to view this user
            validateUserAccess(user);

            return mapToUserResponse(user);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching user by email: {}", email, e);
            throw new ServiceException("Failed to fetch user: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserRole = getCurrentUserRole();

            List<AppUser> users;

            if (currentUserRole.equals(AppConstants.ROLE_ADMIN)) {
                // Admin sees all users
                users = userRepository.findAll();

            } else if (currentUserRole.equals(AppConstants.ROLE_AGENT)) {
                // Agent sees only USER role
                users = userRepository.findByRole(AppConstants.ROLE_USER);

            } else {
                // Regular user sees only self
                String email = authentication.getName();
                AppUser currentUser = userRepository.findByEmailAddress(email)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
                users = List.of(currentUser);
            }

            return users.stream()
                    .map(this::mapToUserResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching users", e);
            throw new ServiceException("Failed to fetch users: " + e.getMessage());
        }
    }


    @Override
    @Transactional
    public UserResponse updateUser(Integer userId, CreateUserRequest request) {
        try {
            AppUser existingUser = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            // Check if current user has permission to update this user
            validateUserModification(existingUser);

            // Validate role if being changed
            if (request.getRole() != null && !request.getRole().equals(existingUser.getRole())) {
                validateCurrentUserPermission(request.getRole());
                validateUserCreationRole(request.getRole());
                existingUser.setRole(request.getRole().toUpperCase());
            }

            // Update fields if provided
            if (request.getName() != null) {
                existingUser.setName(request.getName());
            }

            if (request.getUserType() != null) {
                existingUser.setUserType(request.getUserType());
            }

            // Update merchants if provided
            if (request.getPayingMerchantId() != null) {
                Merchant payingMerchant = merchantRepository.findById(request.getPayingMerchantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", request.getPayingMerchantId()));

                validateMerchantAssignment(payingMerchant);
                existingUser.setPayingMerchant(payingMerchant);
            }

            if (request.getPayoutMerchantId() != null) {
                Merchant payoutMerchant = merchantRepository.findById(request.getPayoutMerchantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Merchant", "id", request.getPayoutMerchantId()));

                validateMerchantAssignment(payoutMerchant);
                existingUser.setPayoutMerchant(payoutMerchant);
            }

            // Update password if provided
            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                existingUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            }

            existingUser.setUpdatedAt(OffsetDateTime.now());

            AppUser updatedUser = userRepository.save(existingUser);
            log.info("User updated successfully: {}", updatedUser.getEmailAddress());

            return mapToUserResponse(updatedUser);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating user: {}", userId, e);
            throw new ServiceException("Failed to update user: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        try {
            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            // Check if current user has permission to delete this user
            validateUserModification(user);

            // Check if user has any active transactions
            // You can add transaction checks here

            // Instead of hard delete, mark as inactive
            user.setIsActive(false);
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);

            log.info("User marked as inactive: {}", user.getEmailAddress());

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting user: {}", userId, e);
            throw new ServiceException("Failed to delete user: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Integer userId, Boolean isActive) {
        try {
            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            // Check if current user has permission to update status
            validateUserModification(user);

            // Cannot deactivate yourself
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();

            if (user.getEmailAddress().equals(currentUserEmail) && Boolean.FALSE.equals(isActive)) {
                throw new UnauthorizedException("You cannot deactivate your own account");
            }

            user.setIsActive(isActive);
            user.setUpdatedAt(OffsetDateTime.now());

            AppUser updatedUser = userRepository.save(user);

            String action = isActive ? "activated" : "deactivated";
            log.info("User {} successfully: {}", action, updatedUser.getEmailAddress());

            return mapToUserResponse(updatedUser);

        } catch (ResourceNotFoundException | UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating user status: {}", userId, e);
            throw new ServiceException("Failed to update user status: " + e.getMessage());
        }
    }

    private void validateCurrentUserPermission(String targetRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserRole = getCurrentUserRole();

        if (currentUserRole.equals(AppConstants.ROLE_ADMIN)) {
            // Admin can create any role
            return;
        } else if (currentUserRole.equals(AppConstants.ROLE_AGENT)) {
            // Agent can only create USER role
            if (!targetRole.equalsIgnoreCase(AppConstants.ROLE_USER)) {
                throw new UnauthorizedException("Agents can only create users with USER role");
            }
        } else {
            // Regular users cannot create other users
            throw new UnauthorizedException("You don't have permission to create users");
        }
    }

    private void validateUserCreationRole(String targetRole) {
        String currentUserRole = getCurrentUserRole();

        // Regular users cannot create anyone
        if (currentUserRole.equals(AppConstants.ROLE_USER)) {
            throw new UnauthorizedException("Regular users cannot create other users");
        }

        // Agents can only create USER roles
        if (currentUserRole.equals(AppConstants.ROLE_AGENT) &&
                !targetRole.equalsIgnoreCase(AppConstants.ROLE_USER)) {
            throw new UnauthorizedException("Agents can only create USER roles");
        }

        // Validate role format
        if (!targetRole.equalsIgnoreCase(AppConstants.ROLE_ADMIN) &&
                !targetRole.equalsIgnoreCase(AppConstants.ROLE_AGENT) &&
                !targetRole.equalsIgnoreCase(AppConstants.ROLE_USER)) {
            throw new IllegalArgumentException("Invalid role. Allowed roles: ADMIN, AGENT, USER");
        }
    }

    private void validateUserAccess(AppUser targetUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        String currentUserRole = getCurrentUserRole();

        // Users can always view their own profile
        if (targetUser.getEmailAddress().equals(currentUserEmail)) {
            return;
        }

        // Admin can view anyone
        if (currentUserRole.equals(AppConstants.ROLE_ADMIN)) {
            return;
        }

        // Agent can view their created users (USER role) and themselves
        if (currentUserRole.equals(AppConstants.ROLE_AGENT) &&
                targetUser.getRole().equals(AppConstants.ROLE_USER)) {
            return;
        }

        throw new UnauthorizedException("You don't have permission to view this user");
    }

    private void validateUserModification(AppUser targetUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        String currentUserRole = getCurrentUserRole();

        // Users can modify their own profile (except role)
        if (targetUser.getEmailAddress().equals(currentUserEmail)) {
            return;
        }

        // Admin can modify anyone
        if (currentUserRole.equals(AppConstants.ROLE_ADMIN)) {
            return;
        }

        // Agent can only modify USER roles they created
        if (currentUserRole.equals(AppConstants.ROLE_AGENT) &&
                targetUser.getRole().equals(AppConstants.ROLE_USER)) {
            // Additional check: Agent can only modify users they created
            // You might want to add a createdBy field in AppUser entity for this
            return;
        }

        throw new UnauthorizedException("You don't have permission to modify this user");
    }

    private void validateMerchantAssignment(Merchant merchant) {
        // Add merchant assignment validation logic here
        // For example, check if current user is authorized to assign this merchant
        // This depends on your business rules
    }

    private String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(authority -> authority.replace("ROLE_", ""))
                .orElseThrow(() -> new UnauthorizedException("User role not found"));
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