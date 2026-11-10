package com.footballai.social.repository;
import com.footballai.social.model.entity.ClubTeam;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ClubTeamRepository extends JpaRepository<ClubTeam,UUID>, JpaSpecificationExecutor<ClubTeam> {
    List<ClubTeam> findAllByClubId(UUID clubId);
}
