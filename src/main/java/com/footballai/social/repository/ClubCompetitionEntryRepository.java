package com.footballai.social.repository;
import com.footballai.social.model.entity.ClubCompetitionEntry;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ClubCompetitionEntryRepository extends JpaRepository<ClubCompetitionEntry,UUID>, JpaSpecificationExecutor<ClubCompetitionEntry> {
    List<ClubCompetitionEntry> findAllByClubId(UUID clubId);
}
