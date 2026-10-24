package com.footballai.social.repository.cassandra;

import com.footballai.social.model.cassandra.TelemetryData;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public interface TelemetryDataRepository extends CassandraRepository<TelemetryData, TelemetryData.Key> {
    List<TelemetryData> findByKeyAthleteIdAndKeyYearMonth(UUID athleteId, String yearMonth);
}
