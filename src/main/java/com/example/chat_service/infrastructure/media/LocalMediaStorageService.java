// src/main/java/com/example/chat_service/infrastructure/media/LocalMediaStorageService.java

package com.example.chat_service.infrastructure.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for handling local file storage of post images.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Validate and sanitize uploaded files</li>
 *   <li>Generate unique filenames to prevent collisions</li>
 *   <li>Store files in configured local directory</li>
 *   <li>Return public URL path for stored file</li>
 * </ul>
 * </p>
 *
 * <p><strong>Security note:</strong> This implementation trusts the file extension
 * from the original filename. In production, add MIME type validation, size limits,
 * and extension whitelisting.</p>
 */
@Service
public class LocalMediaStorageService {

    /**
     * Relative path where post images are stored.
     * Resolved against application working directory.
     */
    private static final String UPLOAD_DIR = "uploads/posts";

    /**
     * Save an uploaded image file to local storage.
     *
     * @param file the MultipartFile from the HTTP request
     * @return the public URL path to access the saved image, e.g. "/uploads/posts/abc123.jpg"
     * @throws RuntimeException if file saving fails
     */
    public String savePostImage(MultipartFile file) {

        try {
            // ─────────────────────────────────────────────
            // 1. Ensure upload directory exists
            // ─────────────────────────────────────────────
            Path uploadPath = Paths.get(UPLOAD_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // ─────────────────────────────────────────────
            // 2. Generate unique, safe filename
            // ─────────────────────────────────────────────
            String originalName = file.getOriginalFilename();
            String extension = "";

            if (originalName != null && originalName.contains(".")) {
                // Extract extension including the dot, e.g. ".jpg"
                extension = originalName.substring(
                        originalName.lastIndexOf(".")
                ).toLowerCase();
            }

            // Use UUID to guarantee uniqueness + prevent path traversal
            String filename = UUID.randomUUID() + extension;

            Path destination = uploadPath.resolve(filename);

            // ─────────────────────────────────────────────
            // 3. Save file to disk
            // ─────────────────────────────────────────────
            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            // ─────────────────────────────────────────────
            // 4. Return public URL path (relative to web root)
            // ─────────────────────────────────────────────
            return "/uploads/posts/" + filename;

        } catch (IOException e) {
            // Wrap checked exception in runtime exception for handler layer
            throw new RuntimeException(
                    "Failed to save image: " + file.getOriginalFilename(),
                    e
            );
        }
    }

    /**
     * Optional: Delete a previously saved image by its public path.
     *
     * @param publicPath the path returned by savePostImage(), e.g. "/uploads/posts/abc.jpg"
     * @return true if file was deleted, false if not found
     */
    public boolean deletePostImage(String publicPath) {
        try {
            // Remove leading slash and convert to filesystem path
            String relativePath = publicPath.replaceFirst("^/", "");
            Path filePath = Paths.get(relativePath);

            if (Files.exists(filePath)) {
                return Files.deleteIfExists(filePath);
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to delete image: " + publicPath,
                    e
            );
        }
    }
}