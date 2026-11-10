package com.footballai.social.repository;
import com.footballai.social.model.entity.ClubMembership;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ClubMembershipRepository extends JpaRepository<ClubMembership,UUID>, JpaSpecificationExecutor<ClubMembership> {
    List<ClubMembership> findAllByClubId(UUID clubId);
}
