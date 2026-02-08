package com.suvikapay.wallet.repo;

import com.suvikapay.wallet.entity.UserStatementView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface UserStatementViewRepository extends JpaRepository<UserStatementView, String> {

    List<UserStatementView> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<UserStatementView> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Integer userId, OffsetDateTime from, OffsetDateTime to
    );

    List<UserStatementView> findByTxnTypeAndUserIdOrderByCreatedAtDesc(String txnType, Integer userId);
}
