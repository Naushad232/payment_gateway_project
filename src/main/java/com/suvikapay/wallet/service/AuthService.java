// src/main/java/com/suvikapay/wallet/service/AuthService.java
package com.suvikapay.wallet.service;

import com.suvikapay.wallet.dto.request.AuthRequest;
import com.suvikapay.wallet.dto.request.CreateUserRequest;
import com.suvikapay.wallet.dto.request.RefreshTokenRequest;
import com.suvikapay.wallet.dto.response.AuthResponse;
import com.suvikapay.wallet.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse authenticate(AuthRequest request, HttpServletRequest httpRequest);
    AuthResponse refreshToken(RefreshTokenRequest request);
    UserResponse register(CreateUserRequest request, HttpServletRequest httpRequest);
    void logout(HttpServletRequest request);
    void changePassword(String currentPassword, String newPassword, String email);
}