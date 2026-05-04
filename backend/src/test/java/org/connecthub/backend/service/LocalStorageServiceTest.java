package org.connecthub.backend.service;

import org.connecthub.backend.exception.FileStorageException;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for LocalStorageService.
 * Uses a real temporary directory
 * Tests: 
 * - Valid JPEG and PNG uploads
 * - File size limit enforcement
 * - MIME type validation
 * - Empty file handling
 * - File deletion behavior
 */
@DisplayName("LocalStorageService Unit Tests")
class LocalStorageServiceTest {

    private static Path tempDir;
    private LocalStorageService storageService;

    @BeforeAll
    static void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("connecthub-test-uploads");
    }

    @AfterAll
    static void deleteTempDir() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
    }

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("Valid JPEG — stored and path returned")
    void store_validJpeg_returnsPublicPath() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[512]);

        String path = storageService.store(file, "profiles");

        assertThat(path).startsWith("/uploads/profiles/");
        assertThat(path).endsWith(".jpg");
    }

    @Test
    @DisplayName("Valid PNG — stored successfully")
    void store_validPng_returnsPublicPath() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "img.png", "image/png", new byte[256]);

        String path = storageService.store(file, "posts");

        assertThat(path).startsWith("/uploads/posts/");
    }

    @Test
    @DisplayName("File too large — throws FileStorageException")
    void store_fileTooLarge_throwsException() {
        byte[] bigFile = new byte[6 * 1024 * 1024]; // 6 MB — over limit
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", bigFile);

        assertThatThrownBy(() -> storageService.store(file, "profiles"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    @DisplayName("Invalid MIME type — throws FileStorageException")
    void store_invalidMimeType_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.js", "application/javascript", new byte[100]);

        assertThatThrownBy(() -> storageService.store(file, "profiles"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    @DisplayName("Empty file — throws FileStorageException")
    void store_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storageService.store(file, "profiles"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("Delete existing file — file is removed from disk")
    void delete_existingFile_fileRemovedFromDisk() throws IOException {
        // First store a file
        MockMultipartFile file = new MockMultipartFile(
                "file", "todelete.jpg", "image/jpeg", new byte[128]);
        String path = storageService.store(file, "profiles");

        // Verify it exists
        String relative = path.replaceFirst("^/uploads/", "");
        Path diskPath = tempDir.resolve(relative);
        assertThat(diskPath).exists();

        // Now delete it
        storageService.delete(path);

        assertThat(diskPath).doesNotExist();
    }

    @Test
    @DisplayName("Delete non-existent file — no exception thrown")
    void delete_nonExistentFile_doesNotThrow() {
        // Should silently ignore missing files
        assertThatCode(() -> storageService.delete("/uploads/profiles/ghost.jpg"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Delete null path — no exception thrown")
    void delete_nullPath_doesNotThrow() {
        assertThatCode(() -> storageService.delete(null))
                .doesNotThrowAnyException();
    }
}