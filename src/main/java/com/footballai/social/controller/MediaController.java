package com.footballai.social.controller;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.service.ApiException;
import com.footballai.social.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Upload da fotografia do card (campo do usuário) — separado da arte de fundo, que só tinge o card. */
@RestController
@RequestMapping("/v1/media")
public class MediaController {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "video/mp4");
    private static final long MAX_BYTES = 25L * 1024 * 1024;
    private final StorageService storage;
    public MediaController(StorageService storage) { this.storage = storage; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> upload(@AuthenticationPrincipal AuthPrincipal p, @RequestParam("file") MultipartFile file) throws IOException {
        String ct = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED.contains(ct)) throw ApiException.badRequest("MEDIA_TYPE", "Formatos aceitos: JPEG, PNG, WebP, GIF ou MP4");
        if (file.getSize() > MAX_BYTES) throw ApiException.badRequest("MEDIA_SIZE", "Arquivo acima de 25 MB");
        String ext = switch (ct) { case "image/jpeg" -> "jpg"; case "image/png" -> "png"; case "image/webp" -> "webp"; case "image/gif" -> "gif"; default -> "mp4"; };
        String key = "media/" + p.userId() + "/" + UUID.randomUUID() + "." + ext;
        try { storage.putMedia(key, file.getBytes(), ct); }
        catch (RuntimeException e) { throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_DOWN", "Armazenamento de mídia indisponível (" + e.getClass().getSimpleName() + ")"); }
        return Map.of("key", key, "url", storage.mediaUrl(key), "kind", ct.startsWith("video") ? "video" : "image", "contentType", ct, "sizeBytes", file.getSize());
    }
}
