package com.footballai.social.repository;
import com.footballai.social.model.entity.ClubManagerTenure;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ClubManagerTenureRepository extends JpaRepository<ClubManagerTenure,UUID>, JpaSpecificationExecutor<ClubManagerTenure> {
    List<ClubManagerTenure> findAllByClubId(UUID clubId);
}
