package com.bakouan.app.controller;

import com.bakouan.app.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequiredArgsConstructor @RequestMapping("/api/me/signature")
public class SignatureController {
    private final SignatureService signatures;
    @GetMapping public ResponseEntity<SignatureService.Status> status() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(signatures.status()); }
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SignatureService.Status upload(@RequestPart("file") MultipartFile file) { return signatures.upload(file); }
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> preview(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff").body(signatures.preview(file));
    }
    @GetMapping("/image") public ResponseEntity<byte[]> image() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff").body(signatures.image());
    }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) public void remove() { signatures.remove(); }
}
