package com.waveterm.demo.terminal.upload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final UploadProperties properties;

    public UploadService(UploadProperties properties) {
        this.properties = properties;
    }

    @Transactional
    public StoredUpload store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new UploadValidationException("file is required");
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw new UploadValidationException("file exceeds configured size limit");
        }
        List<String> allowedTypes = properties.getAllowedContentTypes();
        if (!allowedTypes.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || allowedTypes.stream().noneMatch(type -> type.equalsIgnoreCase(contentType))) {
                throw new UploadValidationException("unsupported content type: " + contentType);
            }
        }
        Path baseDir = properties.getBaseDir();
        Files.createDirectories(baseDir);
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file";
        String sanitizedName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uploadId = UUID.randomUUID().toString();
        String fileName = uploadId + "_" + sanitizedName;
        Path target = baseDir.resolve(fileName);
        Files.copy(file.getInputStream(), target);
        Files.setLastModifiedTime(target, FileTime.from(Instant.now()));
        log.debug("Stored upload {} at {}", uploadId, target);
        return new StoredUpload(uploadId, target.toUri().toString());
    }

    public record StoredUpload(String uploadId, String url) {
    }
}
