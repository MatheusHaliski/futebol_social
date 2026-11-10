package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ProfessionalCardDtos.Card;
import com.footballai.social.service.ProfessionalCardService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.*;

@RestController
@RequestMapping("/v1/discovery/cards")
public class ProfessionalCardController {
    private final ProfessionalCardService cards;
    public ProfessionalCardController(ProfessionalCardService cards) {this.cards=cards;}
    @GetMapping public List<Card> cards(@AuthenticationPrincipal AuthPrincipal viewer,@RequestParam List<UUID> ids) {return cards.cards(viewer,ids);}
}
