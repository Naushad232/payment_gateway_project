// src/main/java/com/suvikapay/wallet/repository/AppUserRepository.java
package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Integer> {

    Optional<AppUser> findByEmailAddress(String emailAddress);
    Optional<AppUser> findByUserName(String userName);

    boolean existsByEmailAddress(String emailAddress);
    boolean existsByUserName(String userName);

    List<AppUser> findByRole(String role);
    Page<AppUser> findByRole(String role, Pageable pageable);

    @Query("SELECT u FROM AppUser u WHERE " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.emailAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<AppUser> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT u FROM AppUser u WHERE u.role = :role AND " +
            "(LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.emailAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<AppUser> searchUsersByRole(@Param("searchTerm") String searchTerm,
                                    @Param("role") String role,
                                    Pageable pageable);

    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.role = :role")
    long countByRole(@Param("role") String role);

    @Query("SELECT u FROM AppUser u WHERE u.isActive = :isActive")
    Page<AppUser> findByActiveStatus(@Param("isActive") Boolean isActive, Pageable pageable);

    @Query("SELECT u FROM AppUser u WHERE u.payingMerchant.merchantId = :merchantId")
    List<AppUser> findByPayingMerchantId(@Param("merchantId") Long merchantId);

    @Query("SELECT u FROM AppUser u WHERE u.payoutMerchant.merchantId = :merchantId")
    List<AppUser> findByPayoutMerchantId(@Param("merchantId") Long merchantId);
}