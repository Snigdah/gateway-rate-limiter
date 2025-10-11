package com.api.license.repository;

import com.api.license.entity.ClientLicenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientLicenseRepository extends JpaRepository<ClientLicenseEntity, String> {
}
