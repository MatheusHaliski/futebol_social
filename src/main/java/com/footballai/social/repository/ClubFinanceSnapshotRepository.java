package com.footballai.social.repository;
import com.footballai.social.model.entity.ClubFinanceSnapshot;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ClubFinanceSnapshotRepository extends JpaRepository<ClubFinanceSnapshot,UUID>, JpaSpecificationExecutor<ClubFinanceSnapshot> {
    List<ClubFinanceSnapshot> findAllByClubId(UUID clubId);
}
