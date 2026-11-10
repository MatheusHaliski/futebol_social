package com.footballai.social.controller;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.ManagerDtos.*;
import com.footballai.social.service.ManagerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/v1/managers/{id}")
public class ManagerController {
    private final ManagerService managers;
    public ManagerController(ManagerService managers) { this.managers=managers; }
    @GetMapping public Profile profile(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id) { return managers.profile(p,id); }
    @PutMapping public Profile update(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@Valid @RequestBody EditManager r) { return managers.update(p,id,r); }
    @PostMapping("/titles") public CareerTitle title(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@Valid @RequestBody CareerTitleRequest r) { return managers.addTitle(p,id,r); }
    @DeleteMapping("/titles/{titleId}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteTitle(@AuthenticationPrincipal AuthPrincipal p,@PathVariable UUID id,@PathVariable UUID titleId) { managers.removeTitle(p,id,titleId); }
}
