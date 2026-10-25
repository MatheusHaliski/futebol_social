package com.footballai.social.repository;

import com.footballai.social.model.entity.TermsVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TermsVersionRepository extends JpaRepository<TermsVersion, UUID> {
    Optional<TermsVersion> findFirstByCurrentTrueOrderByEffectiveFromDesc();
    Optional<TermsVersion> findByVersion(String version);
}
