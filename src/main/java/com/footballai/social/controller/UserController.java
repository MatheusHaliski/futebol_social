package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.AuthDtos.UpdateMeRequest;
import com.footballai.social.controller.dto.AuthDtos.UserSummaryDTO;
import com.footballai.social.controller.dto.CardDtos.AthleteCardDTO;
import com.footballai.social.controller.dto.CardDtos.BadgeDTO;
import com.footballai.social.model.entity.Athlete;
import com.footballai.social.model.entity.UserLgpdPreferences;
import com.footballai.social.model.enums.BadgeType;
import com.footballai.social.repository.AthleteRepository;
import com.footballai.social.repository.UserLgpdPreferencesRepository;
import com.footballai.social.service.AccessPolicy;
import com.footballai.social.service.ApiException;
import com.footballai.social.service.BadgeAwardService;
import com.footballai.social.service.CardAssembler;
import com.footballai.social.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.footballai.social.model.enums.AccountStatus;
import com.footballai.social.model.enums.ProfileVisibility;
import com.footballai.social.service.FollowService;

/** Perfil do usuário, card completo do atleta e histórico de badges (tap to expand). */
@RestController
@RequestMapping("/v1")
public class UserController {
    private final UserService users;
    private final AthleteRepository athletes;
    private final CardAssembler assembler;
    private final BadgeAwardService badges;
    private final UserLgpdPreferencesRepository prefs;
    private final FollowService followService;
    private final AccessPolicy access;

    public UserController(UserService users, AthleteRepository athletes, CardAssembler assembler, BadgeAwardService badges, UserLgpdPreferencesRepository prefs, FollowService followService,
                          AccessPolicy access) { this.followService = followService; this.access = access;
        this.users = users; this.athletes = athletes; this.assembler = assembler; this.badges = badges; this.prefs = prefs;
    }

    @GetMapping("/me")
    public UserSummaryDTO me(@AuthenticationPrincipal AuthPrincipal p) { return users.me(p); }

    @PatchMapping("/me")
    public UserSummaryDTO update(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody UpdateMeRequest r) { return users.update(p, r); }

    @GetMapping("/athletes/{id}/card")
    public AthleteCardDTO card(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) {
        Athlete a = athletes.findById(id).orElseThrow(() -> ApiException.notFound("Atleta"));
        if (!access.canViewProfile(p, a)) throw ApiException.notFound("Atleta");   // mesma regra da descoberta; 404 para não revelar existência
        return assembler.athleteCard(a, p);
    }

    /** Bottom sheet "tap to expand": série histórica do badge; dados de saúde só com shareMedicalData. */
    @GetMapping("/athletes/{id}/badges/{type}/history")
    public List<BadgeDTO> badgeHistory(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id, @PathVariable BadgeType type) {
        access.requireProfile(p, id, "Atleta");
        boolean canSeeMedical = access.canSeeMedical(p, id);
        return badges.history(id, type, canSeeMedical);
    }
}
