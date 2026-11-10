package com.footballai.social.repository;
import com.footballai.social.model.entity.ClubVenue;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ClubVenueRepository extends JpaRepository<ClubVenue,UUID>, JpaSpecificationExecutor<ClubVenue> {
    List<ClubVenue> findAllByClubId(UUID clubId);
}
