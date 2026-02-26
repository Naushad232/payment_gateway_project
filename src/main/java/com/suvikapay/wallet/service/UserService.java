// src/main/java/com/suvikapay/wallet/service/UserService.java
package com.suvikapay.wallet.service;

import com.suvikapay.wallet.dto.request.CreateUserRequest;
import com.suvikapay.wallet.dto.request.UpdateUserPartialRequest;
import com.suvikapay.wallet.dto.response.UserResponse;
import com.suvikapay.wallet.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);
    UserResponse getUserById(Integer userId);
    AppUser getAppUserById(Integer userId);  // Add this method

    UserResponse getUserByEmail(String email);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Integer userId, CreateUserRequest request);
    UserResponse updateUserPartial(Integer userId, UpdateUserPartialRequest request);
    void deleteUser(Integer userId);
    UserResponse updateUserStatus(Integer userId, Boolean isActive);
}
