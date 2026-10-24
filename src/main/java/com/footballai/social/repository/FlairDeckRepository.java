package com.footballai.social.repository;

import com.footballai.social.model.entity.FlairDeck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FlairDeckRepository extends JpaRepository<FlairDeck, UUID> { }
