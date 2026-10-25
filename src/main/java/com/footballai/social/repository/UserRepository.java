package com.footballai.social.repository;

import com.footballai.social.model.entity.User;
import com.footballai.social.model.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailHash(String emailHash);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmailHash(String emailHash);
    List<User> findAllByStatusAndScheduledPurgeAtBefore(AccountStatus status, Instant now);
}
