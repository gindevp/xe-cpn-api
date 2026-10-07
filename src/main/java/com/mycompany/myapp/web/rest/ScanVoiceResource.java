package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.ScanVoice;
import com.mycompany.myapp.service.config.ScanVoiceService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/scan-voices")
public class ScanVoiceResource {

    private final ScanVoiceService scanVoiceService;

    public ScanVoiceResource(ScanVoiceService scanVoiceService) {
        this.scanVoiceService = scanVoiceService;
    }

    /** Metadata cho web + app so etag (không kèm file). */
    @GetMapping("")
    public Map<String, Object> list() {
        List<Map<String, Object>> voices = scanVoiceService.list();
        return Map.of("voices", voices);
    }

    @GetMapping("/{type}/file")
    public ResponseEntity<byte[]> download(
        @PathVariable String type,
        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        ScanVoice row = scanVoiceService
            .find(type)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa cấu hình giọng này"));
        String etag = "\"" + row.getEtag() + "\"";
        if (ifNoneMatch != null && (ifNoneMatch.equals(etag) || ifNoneMatch.equals(row.getEtag()))) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(row.getEtag()).build();
        }
        MediaType media = MediaType.parseMediaType(row.getContentType() != null ? row.getContentType() : "audio/mpeg");
        return ResponseEntity.ok()
            .contentType(media)
            .eTag(row.getEtag())
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=0, must-revalidate")
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + (row.getFileName() != null ? row.getFileName() : type + ".mp3") + "\""
            )
            .body(row.getContent());
    }

    @PutMapping(value = "/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@PathVariable String type, @RequestParam("file") MultipartFile file) {
        return scanVoiceService.upload(type, file);
    }

    @DeleteMapping("/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String type) {
        scanVoiceService.delete(type);
    }
}
