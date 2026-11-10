package com.footballai.social.controller.dto;

import jakarta.validation.constraints.*;
import com.footballai.social.model.enums.TransferSituation;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ManagerDtos {
    private ManagerDtos() { }
    public record EditManager(
        @NotBlank @Pattern(regexp="HEAD_COACH|SPORTING_DIRECTOR|ASSISTANT") String function,
        @Size(max=120) @Pattern(regexp="([A-Za-z]{2}(,[A-Za-z]{2})*)?") String nationalities,
        @Size(max=120) @Pattern(regexp="([A-Za-z]{2,3}(,[A-Za-z]{2,3})*)?") String languages,
        @Size(max=120) String licenseName, @Size(max=40) String licenseLevel,
        @Size(max=40) String preferredFormation, @Size(max=120) String playingStyle,
        @PastOrPresent LocalDate careerStartedOn,
        Boolean hasCurrentClub, @Size(max=240) String clubStatusSource,
        @PastOrPresent LocalDate clubStatusAsOf, TransferSituation transferSituation) { }
    public record CareerTitleRequest(
        UUID clubId, @NotBlank @Size(max=120) String clubName,
        @NotBlank @Size(max=120) String competition,
        @NotBlank @Size(max=30) String season,
        @Size(max=40) String category,
        @NotNull @PastOrPresent LocalDate awardedOn,
        @NotBlank @Size(max=240) String source) { }
    public record CareerTitle(UUID id, UUID clubId, String clubName, String competition, String season,
                              String category, LocalDate awardedOn, String source, String evidenceStatus) { }
    public record Tenure(UUID id, UUID clubId, String clubName, UUID teamId, String function,
                         LocalDate startDate, LocalDate endDate, LocalDate contractUntil, boolean interim,
                         BigDecimal transferFee, String currency, String source) { }
    public record ClubCareerMetric(UUID clubId, String clubName, int spells, long serviceDays) { }
    public record LeaguePosition(UUID clubId, UUID teamId, String competition, String season,
                                 String stage, String groupName, Integer position, LocalDate asOf) { }
    public record Profile(UUID managerId, String name, String handle, String bio, String country,
                          String state, String city, String function, String nationalities, String languages,
                          String licenseName, String licenseLevel, String preferredFormation, String playingStyle,
                          LocalDate careerStartedOn, Boolean hasCurrentClub, String clubStatusSource,
                          LocalDate clubStatusAsOf, List<Tenure> currentClubs, List<Tenure> history,
                          List<CareerTitle> titles, List<ClubCareerMetric> mostSpellsClubs,
                          List<ClubCareerMetric> longestServiceClubs, List<LeaguePosition> leaguePositions,
                          String matchCountStatus, boolean editable, TransferSituation transferSituation) { }
}
