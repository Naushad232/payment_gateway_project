package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.UserIp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;

public interface UserIpRepository extends JpaRepository<UserIp, Long> {
    List<UserIp> findByUserUserId(Integer userId);
    Optional<UserIp> findByUserUserIdAndIpAddress(Integer userId, InetAddress ipAddress);
}
