package com.footballai.social.repository;

import com.footballai.social.model.entity.InjuryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InjuryRecordRepository extends JpaRepository<InjuryRecord, UUID> {
    List<InjuryRecord> findAllByAthleteIdAndClearedAtIsNull(UUID athleteId);
    List<InjuryRecord> findAllByAthleteId(UUID athleteId);
    void deleteAllByAthleteId(UUID athleteId);
}
