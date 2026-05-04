package org.connecthub.backend.service;

import org.connecthub.backend.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

/**
 * Stores files on the local filesystem under the configured upload directory.
 * Files are served as static resources via /uploads/** URL path.
 */

@Service
public class LocalStorageService implements StorageService {

    // Allowed MIME types for profile and cover photos
    private static final List<String> ALLOWED_MIME_TYPES =
            List.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    private final Path uploadRoot;

    public LocalStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDir) {
        validateFile(file);

        try {
            Path targetDir = uploadRoot.resolve(subDir);
            Files.createDirectories(targetDir);

            // Generate a unique filename with the original extension
            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;

            // Save the file to the target location
            Path target = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Return the public URL path
            return "/uploads/" + subDir + "/" + filename;

        } catch (IOException e) {
            throw new FileStorageException("Failed to store file", e);
        }
    }

    @Override
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            // Strip leading /uploads/ to resolve against the upload root
            String relative = filePath.replaceFirst("^/uploads/", "");
            Path target = uploadRoot.resolve(relative).normalize();
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // Log but don't throw — deletion failure is non-fatal
            System.err.println("Warning: could not delete file: " + filePath + " — " + e.getMessage());
        }
    }

    // Validate file size and type before storing
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File is empty or null");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileStorageException("File size exceeds the 5 MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new FileStorageException(
                    "Invalid file type. Allowed: JPEG, PNG, GIF, WEBP");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
