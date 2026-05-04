package org.connecthub.backend.service;
import org.springframework.web.multipart.MultipartFile;


public interface StorageService {

    /**
     * Persist a file and return its public-accessible path.
     * @param file     the uploaded file
     * @param subDir   subdirectory under the upload root (e.g. "profiles", "posts")
     * @return         the relative URL path the frontend can use to display the file
     */
    String store(MultipartFile file, String subDir);

    /**
     * Delete a previously stored file by its path.
     * Silently ignores if the file does not exist.
     */
    void delete(String filePath);
}
