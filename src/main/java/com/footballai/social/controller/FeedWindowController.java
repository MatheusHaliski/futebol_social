package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.FeedPageDTO;
import com.footballai.social.controller.dto.NavigationDtos.NavItemDTO;
import com.footballai.social.controller.dto.NavigationDtos.NavigationDTO;
import com.footballai.social.model.enums.FeedWindow;
import com.footballai.social.model.enums.PostContentType;
import com.footballai.social.model.enums.RarityTier;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.service.FeedQuery;
import com.footballai.social.service.FeedWindowService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * RF3 — {@code GET /v1/feeds/{window}} com o painel de filtros segmentado de cada janela:
 * PERFIL (tier, position, ageMin/ageMax, clubId, contractStatus) · GEMEO (period, metricType, eventType, athleteId)
 * · SCOUTING (attr[velocidade]=gt:80, compare, source) · ALERTA (severity, alertType, source);
 * comuns: sort, role, state, q, cursor, size. {@code GET /v1/ui/navigation} expõe a bottom bar e suas regras.
 */
@RestController
@RequestMapping("/v1")
public class FeedWindowController {
    private final FeedWindowService feeds;
    public FeedWindowController(FeedWindowService feeds) { this.feeds = feeds; }

    @GetMapping("/feeds/{window}")
    public FeedPageDTO feed(@AuthenticationPrincipal AuthPrincipal p, @PathVariable FeedWindow window, @RequestParam MultiValueMap<String, String> params) {
        return feeds.page(FeedQuery.parse(window, params), p);
    }

    @GetMapping("/ui/navigation")
    public NavigationDTO navigation() {
        List<NavItemDTO> items = Arrays.stream(FeedWindow.values()).sorted(Comparator.comparingInt(FeedWindow::order))
                .map(w -> new NavItemDTO(w.name(), w.order(), w.icon(), w.route(), w.label(), w.description(), "/v1/feeds/" + w.name())).toList();
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("maxPrimaryTabs", 4);
        rules.put("thumbZone", true);
        rules.put("createOnlyViaFab", "POST /v1/posts");
        rules.put("hideOnScrollDown", true);
        rules.put("swipeBetweenWindows", true);
        rules.put("minContrast", "3:1 (WCAG 2.1 AA)");
        rules.put("minTargetPt", 44);
        rules.put("labelsNeverTruncated", true);
        rules.put("filtersPanelPerWindow", "<aside class=\"filters-panel\">");
        rules.put("rarityNeverAsText", "use crestIcon (ic-tier-*)");
        return new NavigationDTO(items, rules, tokens());
    }

    /** Tokens de design + cores de papéis/bordas/tiers para a SPA (fonte única de verdade). */
    @GetMapping("/ui/tokens")
    public Map<String, String> tokens() {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("--ink", "#0a0f0c"); t.put("--ink-2", "#0f1613"); t.put("--panel", "#131b17"); t.put("--panel-2", "#19231d");
        t.put("--line", "#26332b"); t.put("--line-soft", "#1e2921"); t.put("--text", "#eaf2ee"); t.put("--text-2", "#95a89d"); t.put("--text-3", "#63756a");
        t.put("--green", "#00d47a"); t.put("--green-hi", "#6cffc0"); t.put("--green-deep", "#00874e");
        t.put("--gold", "#f5c542"); t.put("--gold-hi", "#ffe08a"); t.put("--gold-deep", "#b8902c");
        for (UserRole r : UserRole.values()) t.put("role." + r.name(), r.ringColor());
        for (PostContentType c : PostContentType.values()) t.put("border." + c.name(), c.borderColor());
        for (RarityTier r : RarityTier.values()) { t.put("tier." + r.name() + ".color", r.color()); t.put("tier." + r.name() + ".crest", r.crestIcon()); }
        return t;
    }
}
