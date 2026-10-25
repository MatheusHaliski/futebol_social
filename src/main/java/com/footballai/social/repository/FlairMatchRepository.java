package com.footballai.social.repository;

import com.footballai.social.model.entity.FlairMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface FlairMatchRepository extends JpaRepository<FlairMatch, UUID> {
    long countByChallengerIdAndCreatedAtAfter(UUID challengerId, Instant after);
    List<FlairMatch> findTop10ByChallengerIdOrderByCreatedAtDesc(UUID challengerId);

    /** Lance de partida: lê com lock de escrita para serializar lances concorrentes (sem lance perdido nem recompensa duplicada). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from FlairMatch m where m.id = :id")
    Optional<FlairMatch> findByIdForUpdate(@Param("id") UUID id);

    /** Ids das partidas cujo deck guarda cópia do atleta (anonimização LGPD); só ids, para a entidade ser carregada já com lock. */
    @Query("select m.id from FlairMatch m where m.deckJson like concat('%', :fragment, '%')")
    List<UUID> findIdsByDeckJsonContaining(@Param("fragment") String fragment);
}
