package com.footballai.social.repository;

import com.footballai.social.model.entity.UserLgpdPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserLgpdPreferencesRepository extends JpaRepository<UserLgpdPreferences, UUID> { }
