package com.footballai.social.controller;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ClubDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.service.ClubService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/v1")
public class ClubController {
 private final ClubService service;
 public ClubController(ClubService service){this.service=service;}
 @GetMapping("/clubs/{id}") public Profile profile(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id){return service.profile(p,id);}
 @PutMapping("/clubs/{id}") public Summary update(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@Valid @RequestBody Identity r){return service.update(p,id,r);}
 @PostMapping("/clubs/{club}/teams") public ClubTeam addClubTeam(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@Valid @RequestBody ClubTeam r){return service.team(p,club,null,r);}
 @PutMapping("/clubs/{club}/teams/{id}") public ClubTeam updateClubTeam(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@PathVariable UUID id,@Valid @RequestBody ClubTeam r){return service.team(p,club,id,r);}
 @PostMapping("/clubs/{club}/managers") public ClubManagerTenure addClubManagerTenure(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@Valid @RequestBody ClubManagerTenure r){return service.manager(p,club,null,r);}
 @PutMapping("/clubs/{club}/managers/{id}") public ClubManagerTenure updateClubManagerTenure(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@PathVariable UUID id,@Valid @RequestBody ClubManagerTenure r){return service.manager(p,club,id,r);}
 @PostMapping("/clubs/{club}/competitions") public ClubCompetitionEntry addClubCompetitionEntry(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@Valid @RequestBody ClubCompetitionEntry r){return service.competition(p,club,null,r);}
 @PutMapping("/clubs/{club}/competitions/{id}") public ClubCompetitionEntry updateClubCompetitionEntry(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@PathVariable UUID id,@Valid @RequestBody ClubCompetitionEntry r){return service.competition(p,club,id,r);}
 @PostMapping("/clubs/{club}/finances") public ClubFinanceSnapshot addClubFinanceSnapshot(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@Valid @RequestBody ClubFinanceSnapshot r){return service.finance(p,club,null,r);}
 @PutMapping("/clubs/{club}/finances/{id}") public ClubFinanceSnapshot updateClubFinanceSnapshot(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@PathVariable UUID id,@Valid @RequestBody ClubFinanceSnapshot r){return service.finance(p,club,id,r);}
 @PostMapping("/clubs/{club}/venues") public ClubVenue addClubVenue(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@Valid @RequestBody ClubVenue r){return service.venue(p,club,null,r);}
 @PutMapping("/clubs/{club}/venues/{id}") public ClubVenue updateClubVenue(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@PathVariable UUID id,@Valid @RequestBody ClubVenue r){return service.venue(p,club,id,r);}

 @PostMapping("/clubs/{club}/memberships") public ClubMembership invite(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@Valid @RequestBody ClubMembership r){return service.invite(p,club,r);}
 @GetMapping("/me/club-invitations") public List<ClubMembership> invitations(@AuthenticationPrincipal AuthPrincipal p){return service.invitations(p);}
 @PostMapping("/me/club-invitations/{id}/accept") public ClubMembership accept(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id){return service.accept(p,id,true);}
 @PostMapping("/me/club-invitations/{id}/reject") public ClubMembership reject(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id){return service.accept(p,id,false);}
 @PostMapping("/clubs/{club}/memberships/{id}/end") public ClubMembership end(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID club,@PathVariable UUID id){return service.end(p,club,id);}
 @GetMapping("/me/market") public Map<String,Object> market(@AuthenticationPrincipal AuthPrincipal p){return service.market(p);}
 @PutMapping("/me/market") public Map<String,Object> market(@AuthenticationPrincipal AuthPrincipal p,@Valid @RequestBody Market r){return service.market(p,r);}
 @GetMapping("/athletes/{id}/vitrine/compatibility") public Compatibility fit(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@RequestParam UUID clubId,@RequestParam UUID teamId){return service.compatibility(p,id,clubId,teamId);}
}
