package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.storage.StoredMedia;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Ảnh/file trên MinIO, xác thực bằng chữ ký trên URL để thẻ img tải được. */
@RestController
public class MediaResource {

    private final StoredMedia storedMedia;

    public MediaResource(StoredMedia storedMedia) {
        this.storedMedia = storedMedia;
    }

    @GetMapping("/api/media/**")
    public ResponseEntity<byte[]> media(
        HttpServletRequest request,
        @RequestParam(name = "e", required = false) String exp,
        @RequestParam(name = "s", required = false) String sig
    ) {
        String uri = request.getRequestURI();
        String marker = "/api/media/";
        int at = uri.indexOf(marker);
        if (at < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String key = uri.substring(at + marker.length());
        byte[] body;
        try {
            body = storedMedia.readChecked(key, exp, sig);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return ResponseEntity.ok()
            .contentType(contentType(key))
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
            .body(body);
    }

    private static MediaType contentType(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
