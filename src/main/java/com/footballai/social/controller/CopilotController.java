package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CopilotDtos.SuggestRequest;
import com.footballai.social.controller.dto.CopilotDtos.SuggestionDTO;
import com.footballai.social.model.enums.CardLayout;
import com.footballai.social.model.entity.CardArt;
import com.footballai.social.model.enums.CardGradient;
import com.footballai.social.model.enums.CardMaterial;
import com.footballai.social.model.enums.CardStyle;
import com.footballai.social.service.CopilotService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** FAB → Copilot (sugestão revisável) · Autopilot (rascunhos opt-in) · catálogo de arte do criador de cards. */
@RestController
@RequestMapping("/v1")
public class CopilotController {
    private final CopilotService copilot;
    public CopilotController(CopilotService copilot) { this.copilot = copilot; }

    @PostMapping("/copilot/suggest")
    public SuggestionDTO suggest(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody(required = false) SuggestRequest r) {
        return copilot.suggest(p, r == null ? new SuggestRequest(null, null, null) : r);
    }

    @GetMapping("/autopilot/drafts")
    public List<SuggestionDTO> drafts(@AuthenticationPrincipal AuthPrincipal p) { return copilot.autopilotDrafts(p); }

    /** Opções da etapa "Arte" do criador de cards (fonte única para a SPA). */
    @GetMapping("/cards/art-options")
    public Map<String, Object> artOptions() {
        return Map.of(
                "colors", List.of("#00d47a", "#3b82f6", "#f7c948", "#a06bff", "#ff9f43", "#ff5d5d", "#f5c542", "#2dd4bf", "#f472b6", "#22d3ee", "#5c6b62", "#eaf2ee"),
                "gradients", Arrays.stream(CardGradient.values()).map(g -> Map.of("value", g.name(), "label", g.label(), "needsColor2", g.needsColor2())).toList(),
                "materials", Arrays.stream(CardMaterial.values()).map(m -> Map.of("value", m.name(), "label", m.label())).toList(),
                "layouts", Arrays.stream(CardLayout.values()).map(l -> Map.of("value", l.name(), "label", l.label())).toList(),
                "styles", Arrays.stream(CardStyle.values()).map(s -> Map.of("value", s.name(), "label", s.label(), "needsColor2", s.needsColor2())).toList(),
                "palettes", List.of(
                        Map.of("label", "Alvinegro", "color", "#eaf2ee", "color2", "#16181a"),
                        Map.of("label", "Alviverde", "color", "#00d47a", "color2", "#eaf2ee"),
                        Map.of("label", "Rubro-negro", "color", "#ff5d5d", "color2", "#16181a"),
                        Map.of("label", "Tricolor", "color", "#ff5d5d", "color2", "#3b82f6"),
                        Map.of("label", "Celeste", "color", "#22d3ee", "color2", "#eaf2ee"),
                        Map.of("label", "Canarinho", "color", "#f7c948", "color2", "#00d47a")),
                // Alertas (lesão, institucional) só aceitam arte sóbria — a SPA filtra as opções; a API rejeita o resto (ART_INVALID)
                "sober", Map.of("types", List.of("LESAO_ALERTA", "INSTITUCIONAL"),
                        "gradients", CardArt.SOBER_GRADIENTS.stream().map(Enum::name).sorted().toList(),
                        "materials", CardArt.SOBER_MATERIALS.stream().map(Enum::name).sorted().toList(),
                        "styles", CardArt.SOBER_STYLES.stream().map(Enum::name).sorted().toList()),
                "steps", List.of("MODO", "CONTEUDO", "ARTE", "REVISAR"));
    }
}
