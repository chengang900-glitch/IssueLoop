package com.rnd.app.repository;

import com.rnd.app.entity.ExternalIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {
    Optional<ExternalIdentity> findByProviderAndProviderInstanceAndSubject(String provider, String providerInstance, String subject);
}
