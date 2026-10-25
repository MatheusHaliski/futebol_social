package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.FlairDtos.*;
import com.footballai.social.service.FlairService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** RF6 — Modo FLAIR: coleção, pacote diário (odds visíveis), escalação, duelo 1×1 em 3 lances e pontos. */
@RestController
@RequestMapping("/v1/flair")
public class FlairController {
    private final FlairService flair;
    public FlairController(FlairService flair) { this.flair = flair; }

    @GetMapping("/collection")
    public CollectionDTO collection(@AuthenticationPrincipal AuthPrincipal p) { return flair.collection(p); }

    @PostMapping("/packs/open")
    @ResponseStatus(HttpStatus.CREATED)
    public PackDTO open(@AuthenticationPrincipal AuthPrincipal p) { return flair.openPack(p); }

    @PutMapping("/deck")
    public DeckDTO deck(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody SetDeckRequest r) { return flair.setDeck(p, r); }

    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchDTO start(@AuthenticationPrincipal AuthPrincipal p, @RequestBody(required = false) StartMatchRequest r) { return flair.startMatch(p, r); }

    @PostMapping("/matches/{id}/rounds")
    public MatchDTO round(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id, @Valid @RequestBody RoundRequest r) { return flair.playRound(p, id, r); }

    @GetMapping("/matches/{id}")
    public MatchDTO match(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return flair.match(p, id); }

    @GetMapping("/points")
    public PointsDTO points(@AuthenticationPrincipal AuthPrincipal p) { return flair.points(p); }
}
