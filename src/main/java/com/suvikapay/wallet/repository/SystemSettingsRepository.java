package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Short> {
}
