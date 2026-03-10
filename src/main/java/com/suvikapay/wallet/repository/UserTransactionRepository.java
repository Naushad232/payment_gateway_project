// src/main/java/com/suvikapay/wallet/repository/UserTransactionRepository.java
package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.UserTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTransactionRepository extends JpaRepository<UserTransaction, Long> {

    Optional<UserTransaction> findByOrderId(String orderId);

    List<UserTransaction> findByUserId(Integer userId);

    List<UserTransaction> findByUserIdAndStatus(Integer userId, String status);

    List<UserTransaction> findByUserIdAndCreatedAtBetween(Integer userId, OffsetDateTime start, OffsetDateTime end);

    List<UserTransaction> findByUserIdAndType(Integer userId, String type);




    @Query("SELECT u FROM UserTransaction u WHERE u.userId = :userId AND u.orderId = :orderId")
    Optional<UserTransaction> findByUserIdAndOrderId(@Param("userId") Integer userId,
                                                     @Param("orderId") String orderId);
}