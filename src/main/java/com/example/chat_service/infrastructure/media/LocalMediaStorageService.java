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
 * Service for handling local file storage of media files.
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
    private static final String POST_UPLOAD_DIR = "uploads/posts";

    /**
     * Relative path where message images are stored.
     */
    private static final String MESSAGE_UPLOAD_DIR = "uploads/messages";

    /**
     * Relative path where group profile images are stored.
     */
    private static final String GROUP_PROFILE_UPLOAD_DIR = "uploads/groups/profile";

    /**
     * Relative path where group cover/background images are stored.
     */
    private static final String GROUP_COVER_UPLOAD_DIR = "uploads/groups/cover";

    // ─────────────────────────────────────────────────────
    // POST IMAGE METHODS (unchanged)
    // ─────────────────────────────────────────────────────

    /**
     * Save an uploaded image file to local storage for posts.
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
            Path uploadPath = Paths.get(POST_UPLOAD_DIR);

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
     * Delete a previously saved post image by its public path.
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

    // ─────────────────────────────────────────────────────
    // MESSAGE IMAGE METHODS (new)
    // ─────────────────────────────────────────────────────

    /**
     * Save an uploaded image file to local storage for messages.
     *
     * @param file the MultipartFile from the HTTP request
     * @return the public URL path to access the saved image, e.g. "/uploads/messages/abc123.jpg"
     * @throws RuntimeException if file saving fails
     */
    public String saveMessageImage(MultipartFile file) {

        try {
            // ─────────────────────────────────────────────
            // 1. Ensure upload directory exists
            // ─────────────────────────────────────────────
            Path uploadPath = Paths.get(MESSAGE_UPLOAD_DIR);

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
            return "/uploads/messages/" + filename;

        } catch (IOException e) {
            // Wrap checked exception in runtime exception for handler layer
            throw new RuntimeException(
                    "Failed to save image: " + file.getOriginalFilename(),
                    e
            );
        }
    }

    /**
     * Delete a previously saved message image by its public path.
     *
     * @param publicPath the path returned by saveMessageImage(), e.g. "/uploads/messages/abc.jpg"
     * @return true if file was deleted, false if not found
     */
    public boolean deleteMessageImage(String publicPath) {
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

    // ─────────────────────────────────────────────────────
    // GROUP PROFILE IMAGE METHODS (new)
    // ─────────────────────────────────────────────────────

    /**
     * Save an uploaded group profile image file to local storage.
     *
     * @param file the MultipartFile from the HTTP request
     * @return the public URL path to access the saved image, e.g. "/uploads/groups/profile/abc123.jpg"
     * @throws RuntimeException if file saving fails
     */
    public String saveGroupProfileImage(MultipartFile file) {
        return saveImageToDirectory(file, GROUP_PROFILE_UPLOAD_DIR, "/uploads/groups/profile/");
    }

    /**
     * Delete a previously saved group profile image by its public path.
     *
     * @param publicPath the path returned by saveGroupProfileImage(), e.g. "/uploads/groups/profile/abc.jpg"
     * @return true if file was deleted, false if not found
     */
    public boolean deleteGroupProfileImage(String publicPath) {
        return deleteImageFromPath(publicPath);
    }

    // ─────────────────────────────────────────────────────
    // GROUP COVER/BACKGROUND IMAGE METHODS (new)
    // ─────────────────────────────────────────────────────

    /**
     * Save an uploaded group cover/background image file to local storage.
     *
     * @param file the MultipartFile from the HTTP request
     * @return the public URL path to access the saved image, e.g. "/uploads/groups/cover/abc123.jpg"
     * @throws RuntimeException if file saving fails
     */
    public String saveGroupCoverImage(MultipartFile file) {
        return saveImageToDirectory(file, GROUP_COVER_UPLOAD_DIR, "/uploads/groups/cover/");
    }

    /**
     * Delete a previously saved group cover/background image by its public path.
     *
     * @param publicPath the path returned by saveGroupCoverImage(), e.g. "/uploads/groups/cover/abc.jpg"
     * @return true if file was deleted, false if not found
     */
    public boolean deleteGroupCoverImage(String publicPath) {
        return deleteImageFromPath(publicPath);
    }

    // ─────────────────────────────────────────────────────
    // INTERNAL HELPER METHODS
    // ─────────────────────────────────────────────────────

    /**
     * Internal helper to save an image to a specific directory.
     *
     * @param file the MultipartFile to save
     * @param uploadDir the filesystem directory path
     * @param publicPrefix the public URL prefix to return
     * @return the public URL path for the saved image
     */
    private String saveImageToDirectory(MultipartFile file, String uploadDir, String publicPrefix) {
        try {
            // ─────────────────────────────────────────────
            // 1. Ensure upload directory exists
            // ─────────────────────────────────────────────
            Path uploadPath = Paths.get(uploadDir);

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
            return publicPrefix + filename;

        } catch (IOException e) {
            // Wrap checked exception in runtime exception for handler layer
            throw new RuntimeException(
                    "Failed to save image: " + file.getOriginalFilename(),
                    e
            );
        }
    }

    /**
     * Internal helper to delete an image by its public path.
     *
     * @param publicPath the public URL path to delete
     * @return true if file was deleted, false if not found
     */
    private boolean deleteImageFromPath(String publicPath) {
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