package com.footballai.social.repository;

import com.footballai.social.model.entity.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AthleteRepository extends JpaRepository<Athlete, UUID>, JpaSpecificationExecutor<Athlete> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select a from Athlete a where a.id = :id")
 java.util.Optional<Athlete> locked(@org.springframework.data.repository.query.Param("id") UUID id);
}
