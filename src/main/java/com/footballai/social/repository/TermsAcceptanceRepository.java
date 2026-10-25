package com.footballai.social.repository;

import com.footballai.social.model.entity.TermsAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, UUID> {
    boolean existsByUserIdAndTermsVersion(UUID userId, String termsVersion);
    List<TermsAcceptance> findAllByUserIdOrderByAcceptedAtDesc(UUID userId);
}
