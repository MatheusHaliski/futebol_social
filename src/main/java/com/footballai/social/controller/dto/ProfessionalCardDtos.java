package com.footballai.social.controller.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A bounded, public projection for scanning profiles. No health, credentials or unpublished finances. */
public final class ProfessionalCardDtos {
    private ProfessionalCardDtos() {}
    public record Fact(String key, String label, String value, String note) {}
    public record Context(String competition, String season, String stage, Integer position,
                          LocalDate asOf, String source, String teamName) {}
    public record Card(UUID id, String role, String name, String handle, boolean verified, String photoUrl,
                       String headline, String affiliation, String membership, String transferSituation,
                       List<Fact> facts, List<Fact> metrics, Context context, String evidence, String href) {}
}
