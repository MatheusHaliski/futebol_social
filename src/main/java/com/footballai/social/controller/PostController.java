package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.CardDtos.PostCardDTO;
import com.footballai.social.controller.dto.CardDtos.PostViewDTO;
import com.footballai.social.controller.dto.PostDtos.CreatePostRequest;
import com.footballai.social.controller.dto.PostDtos.ReactionDTO;
import com.footballai.social.model.entity.PostReaction;
import com.footballai.social.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Posts: criação via FAB (único ponto de criação), PostView e ações sociais Curtir · Salvar · Compartilhar. */
@RestController
@RequestMapping("/v1/posts")
public class PostController {
    private final PostService posts;
    public PostController(PostService posts) { this.posts = posts; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostCardDTO create(@AuthenticationPrincipal AuthPrincipal p, @Valid @RequestBody CreatePostRequest r) { return posts.create(p, r); }

    @GetMapping("/{id}")
    public PostViewDTO view(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return posts.view(p, id); }

    @PostMapping("/{id}/like")
    public ReactionDTO like(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return posts.toggle(p, id, PostReaction.Kind.LIKE); }

    @PostMapping("/{id}/save")
    public ReactionDTO save(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return posts.toggle(p, id, PostReaction.Kind.SAVE); }

    @PostMapping("/{id}/share")
    public ReactionDTO share(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) { return posts.share(p, id); }
}
