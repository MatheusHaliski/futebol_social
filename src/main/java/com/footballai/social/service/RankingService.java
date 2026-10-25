package com.footballai.social.service;

import com.footballai.social.model.entity.Athlete;
import com.footballai.social.model.entity.Club;
import com.footballai.social.model.entity.DigitalTwin;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.AthleteRepository;
import com.footballai.social.repository.ClubRepository;
import com.footballai.social.repository.DigitalTwinRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/** Rankings semanais por Score_Total (atletas e clubes) e catálogo de clubes para o globo Explorar. */
@Service
public class RankingService {
    public record AthleteRow(int position, UUID athleteId, String handle, String name, String positionLabel, Integer age, String club, String state,
                             String tier, String crestIcon, int scoreTotal, int delta) { }
    public record ClubRow(int position, UUID clubId, String name, String country, Integer division, int athletes, int avgScore, String avgTier, String crestIcon, Double lat, Double lng) { }

    private final DigitalTwinRepository twins;
    private final AthleteRepository athletes;
    private final ClubRepository clubs;

    public RankingService(DigitalTwinRepository twins, AthleteRepository athletes, ClubRepository clubs) { this.twins = twins; this.athletes = athletes; this.clubs = clubs; }

    public List<AthleteRow> athletes(Position position, String state, RarityTier minTier, int limit) {
        List<DigitalTwin> top = twins.ranking(PageRequest.of(0, Math.max(limit * 3, 50)));
        Map<UUID, Athlete> byId = athletes.findAllById(top.stream().map(DigitalTwin::getUserId).toList()).stream().collect(Collectors.toMap(Athlete::getId, a -> a));
        List<AthleteRow> out = new ArrayList<>();
        int pos = 0;
        for (DigitalTwin t : top) {
            Athlete a = byId.get(t.getUserId());
            if (a == null || a.getVisibility() != ProfileVisibility.PUBLICO || a.getStatus() != AccountStatus.ACTIVE) continue;
            if (position != null && a.getPosition() != position) continue;
            if (state != null && !state.equalsIgnoreCase(a.getStateCode())) continue;
            if (minTier != null && t.getRarityTier().ordinal() < minTier.ordinal()) continue;
            pos++;
            out.add(new AthleteRow(pos, a.getId(), "@" + a.getUsername(), a.getDisplayName(), a.getPosition() == null ? null : a.getPosition().name(), a.age(), a.getCurrentClubName(),
                    a.getStateCode(), t.getRarityTier().name(), t.getRarityTier().crestIcon(), t.getScoreTotal(), 0));
            if (out.size() >= limit) break;
        }
        return out;
    }

    /** Clube = média de Score_Total dos atletas públicos vinculados (nunca nominal). */
    public List<ClubRow> clubs(String country, Integer division, RarityTier minTier) {
        Map<UUID, Club> byId = clubs.findAll().stream().collect(Collectors.toMap(Club::getId, c -> c));
        Map<UUID, List<DigitalTwin>> byClub = new HashMap<>();
        Map<UUID, Athlete> ath = athletes.findAll().stream().filter(a -> a.getCurrentClubId() != null && a.getVisibility() == ProfileVisibility.PUBLICO).collect(Collectors.toMap(Athlete::getId, a -> a));
        for (DigitalTwin t : twins.findAllByUserIdIn(ath.keySet())) if (t.getStatus().countsForRanking()) byClub.computeIfAbsent(ath.get(t.getUserId()).getCurrentClubId(), k -> new ArrayList<>()).add(t);
        List<ClubRow> rows = new ArrayList<>();
        for (Club c : byId.values()) {
            if (country != null && !country.equalsIgnoreCase(c.getCountryCode())) continue;
            if (division != null && !division.equals(c.getDivision())) continue;
            List<DigitalTwin> ts = byClub.getOrDefault(c.getId(), List.of());
            int avg = ts.isEmpty() ? 0 : (int) ts.stream().mapToInt(DigitalTwin::getScoreTotal).average().orElse(0);
            RarityTier tier = RarityTier.fromScore(avg);
            if (minTier != null && tier.ordinal() < minTier.ordinal()) continue;
            rows.add(new ClubRow(0, c.getId(), c.getDisplayName(), c.getCountryCode(), c.getDivision(), ts.size(), avg, tier.name(), tier.crestIcon(),
                    c.getLat() == null ? null : c.getLat().doubleValue(), c.getLng() == null ? null : c.getLng().doubleValue()));
        }
        rows.sort(Comparator.comparingInt(ClubRow::avgScore).reversed().thenComparing(ClubRow::name));
        List<ClubRow> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) { ClubRow r = rows.get(i); out.add(new ClubRow(i + 1, r.clubId(), r.name(), r.country(), r.division(), r.athletes(), r.avgScore(), r.avgTier(), r.crestIcon(), r.lat(), r.lng())); }
        return out;
    }
}
