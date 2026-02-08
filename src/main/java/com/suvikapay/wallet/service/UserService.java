// src/main/java/com/suvikapay/wallet/service/UserService.java
package com.suvikapay.wallet.service;

import com.suvikapay.wallet.dto.request.CreateUserRequest;
import com.suvikapay.wallet.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(Integer userId);
    UserResponse getUserByEmail(String email);
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserResponse updateUser(Integer userId, CreateUserRequest request);
    void deleteUser(Integer userId);
    UserResponse updateUserStatus(Integer userId, Boolean isActive);
}