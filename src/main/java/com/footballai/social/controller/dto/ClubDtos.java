package com.footballai.social.controller.dto;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.*;
import com.footballai.social.model.enums.ClubeTipo;
import com.footballai.social.controller.dto.CardDtos.AthleteCardDTO;
public final class ClubDtos {
 private ClubDtos() {}
 public record Identity(@NotBlank @Size(max=80) String name, @Size(max=160) String legalName,
   @Size(max=40) String shortName, @Size(max=40) String federationCode, @Size(max=120) String federation,
   ClubeTipo type, @Min(1) @Max(100) Integer division, @Min(1800) @Max(2200) Integer foundedYear,
   @Pattern(regexp="[A-Z]{2}") String country, @Size(max=2) String state, @Size(max=80) String city,
   @Pattern(regexp="https?://[^\\s]+|^$") @Size(max=240) String website, @Size(max=160) String publicContact,
   @Size(max=80) String colors, @Size(max=40) String ownershipType, @Size(max=120) String president,
   @Size(max=2000) String history, @Size(max=2000) String honours) {}
 public record Summary(UUID id, String handle, Identity identity, boolean verified) {}
 public record Profile(Summary club, boolean editable, List<?> teams, List<?> managers, List<?> competitions,
   List<?> finances, List<?> venues, List<?> memberships, List<AthleteCardDTO> squad) {}
 public record Market(@NotBlank @Pattern(regexp="UNKNOWN|ATTACHED|WITHOUT_CLUB") String affiliationStatus,
   boolean freeTransferConfirmed, @Size(max=240) String source,
   @Size(max=120) String externalClubName, @Size(max=120) String lastClubName,
   @Pattern(regexp="AGENTE_LIVRE|CONTRATADO") String contractStatus, LocalDate contractUntil,
   com.footballai.social.model.enums.Position position, @Size(max=5) String dominantFoot,
   @Min(80) @Max(250) Integer heightCm, @Size(max=20) String category,
   @Size(max=120) String nationalities, @Size(max=120) String languages, @Size(max=80) String playingStyle,
   @Pattern(regexp="[A-Z]{2}") String country, @Size(max=2) String state, @Size(max=80) String city, com.footballai.social.model.enums.TransferSituation transferSituation) {}
 public record Factor(String field, String label, int weight, Integer match, String explanation) {}
 public record Compatibility(UUID athleteId, UUID clubId, UUID teamId, Integer score, int coverage,
   String algorithm, String status, List<Factor> factors, Instant calculatedAt) {}
}
