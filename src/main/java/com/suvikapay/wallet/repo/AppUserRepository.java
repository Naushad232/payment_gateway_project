package com.suvikapay.wallet.repo;

import com.suvikapay.wallet.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    Optional<AppUser> findByEmailAddress(String emailAddress);
    Optional<AppUser> findByUserName(String userName);
}
