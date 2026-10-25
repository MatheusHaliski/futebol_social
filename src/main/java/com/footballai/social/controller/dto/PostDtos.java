package com.footballai.social.controller.dto;

import com.footballai.social.model.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class PostDtos {
    private PostDtos() { }

    /** Criação via FAB/Copilot: título obrigatório (5–12 palavras), teaser opcional, conteúdo só em PostView. */
    public record CreatePostRequest(@NotNull PostContentType contentType, FeedWindow window,
                                    @NotBlank @Size(max = 90) String title, @Size(max = 160) String subtitle, @Size(max = 5000) String content,
                                    List<String> mediaKeys, AgeRating ageRating, AlertSeverity severity, AlertType alertType, UUID clubId,
                                    @Size(max = 2) String stateCode, GenerationMode generationMode, ArtRequest art, Boolean draft) { }

    /** Etapa "Arte" do criador: cor hex, material, layout e estilo. */
    public record ArtRequest(@Size(max = 7) String color, CardMaterial material, CardLayout layout, CardStyle style) { }

    /** Edição no estúdio (só o autor): conteúdo, fotografia e arte de fundo; tipo/borda não muda. */
    public record UpdatePostRequest(@Size(max = 90) String title, @Size(max = 160) String subtitle, @Size(max = 5000) String content, List<String> mediaKeys,
                                    AgeRating ageRating, ArtRequest art, Boolean clearMedia) { }

    public record ReactionDTO(UUID postId, boolean liked, boolean saved, int likes, int saves, int shares) { }
}
