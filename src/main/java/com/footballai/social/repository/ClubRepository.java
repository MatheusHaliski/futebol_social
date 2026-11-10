package com.footballai.social.repository;

import com.footballai.social.model.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClubRepository extends JpaRepository<Club, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Club> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select c from Club c where c.id = :id")
 java.util.Optional<Club> locked(@org.springframework.data.repository.query.Param("id") UUID id);
}
