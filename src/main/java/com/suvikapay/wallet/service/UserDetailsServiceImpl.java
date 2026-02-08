// src/main/java/com/suvikapay/wallet/service/UserDetailsServiceImpl.java
package com.suvikapay.wallet.service;

import com.suvikapay.wallet.entity.AppUser;
import com.suvikapay.wallet.exception.ResourceNotFoundException;
import com.suvikapay.wallet.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmailAddress(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User account is disabled");
        }

        String role = "ROLE_" + user.getRole().toUpperCase();
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        return new User(
                user.getEmailAddress(),
                user.getPasswordHash(),
                authorities
        );
    }

    @Transactional(readOnly = true)
    public AppUser loadUserEntityByUsername(String username) {
        return userRepository.findByEmailAddress(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", username));
    }
}