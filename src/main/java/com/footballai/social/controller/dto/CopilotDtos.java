package com.footballai.social.controller.dto;

import com.footballai.social.model.enums.FeedWindow;
import com.footballai.social.model.enums.PostContentType;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Copilot (FAB → sugestão) e Autopilot (rascunhos opt-in). Tudo rotulado "GERADO POR IA" e revisado por humano (Art. 20). */
public final class CopilotDtos {
    private CopilotDtos() { }
    public record SuggestRequest(@Size(max = 500) String draft, PostContentType contentType, FeedWindow window) { }
    public record SuggestionDTO(String contentType, String window, String title, String subtitle, String content, List<String> badges,
                                String ageRating, CardDtos.CardArtDTO art, String rationale, String source, boolean aiGenerated) { }
}
