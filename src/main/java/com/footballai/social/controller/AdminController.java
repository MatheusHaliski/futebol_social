package com.footballai.social.controller;

import com.footballai.social.model.entity.DigitalTwin;
import com.footballai.social.model.entity.User;
import com.footballai.social.model.enums.TwinStatus;
import com.footballai.social.repository.DigitalTwinRepository;
import com.footballai.social.repository.UserRepository;
import com.footballai.social.service.ApiException;
import com.footballai.social.service.IsoWeeks;
import com.footballai.social.service.RarityEngineService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** Gestão (só OFICIAL_LIGA): recálculo do Rarity sob demanda, verificação de contas. */
@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('OFICIAL_LIGA')")
public class AdminController {
    private final RarityEngineService rarity;
    private final DigitalTwinRepository twins;
    private final UserRepository users;
    public AdminController(RarityEngineService rarity, DigitalTwinRepository twins, UserRepository users) { this.rarity = rarity; this.twins = twins; this.users = users; }

    @PostMapping("/rarity/recalculate")
    public Map<String, Object> recalculate(@RequestParam(required = false) String week) {
        String w = week == null ? IsoWeeks.previous(LocalDate.now()) : week;
        int n = 0;
        for (DigitalTwin t : twins.findAllByStatus(TwinStatus.ACTIVE)) if (rarity.recalculate(t.getId(), w)) n++;
        return Map.of("week", w, "recalculated", n);
    }

    @PostMapping("/users/{id}/verify")
    public Map<String, Object> verify(@PathVariable UUID id, @RequestParam(defaultValue = "true") boolean verified) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("Usuário"));
        u.setVerified(verified); users.save(u);
        return Map.of("id", id, "verified", verified);
    }
}
