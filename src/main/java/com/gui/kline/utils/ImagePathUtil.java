package com.gui.kline.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper for resolving, storing and deleting product image files in a way that
 * is independent of the current working directory.
 *
 * Images are stored under a "product_images" directory. Because the application
 * may be launched from different working directories (IDE, packaged app, etc.),
 * paths are stored as absolute on-disk paths and resolved against several
 * candidate locations so that both newly stored absolute paths and older
 * relative paths continue to load.
 */
public final class ImagePathUtil {

    private static final String IMAGE_DIR_NAME = "product_images";

    private ImagePathUtil() {
    }

    /**
     * Returns the absolute path of the product_images directory, creating it if
     * necessary. The directory is located by walking upward from the current
     * working directory so it works regardless of where the app is launched.
     */
    public static String getImageDirectory() {
        Path cwd = Paths.get("").toAbsolutePath();
        Path candidate = cwd;
        // Walk upward looking for an existing product_images directory.
        while (candidate != null) {
            Path dir = candidate.resolve(IMAGE_DIR_NAME);
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                return dir.toString();
            }
            candidate = candidate.getParent();
        }
        // Not found: create it under the current working directory.
        Path dir = cwd.resolve(IMAGE_DIR_NAME);
        try {
            Files.createDirectories(dir);
        } catch (Exception ex) {
            System.err.println("Failed to create image directory: " + ex.getMessage());
        }
        return dir.toString();
    }

    /**
     * Returns the absolute on-disk path that should be stored for a file with
     * the given name.
     */
    public static String storePath(String filename) {
        return Paths.get(getImageDirectory(), filename).toString();
    }

    /**
     * Resolves a stored image path to an existing File, trying several candidate
     * locations. Returns null if the file cannot be found.
     */
    public static File resolve(String storedPath) {
        if (storedPath == null || storedPath.isEmpty()) {
            return null;
        }
        // 1. As-is (works for absolute paths and correct relative paths).
        File direct = new File(storedPath);
        if (direct.isAbsolute() && direct.exists()) {
            return direct;
        }
        if (direct.exists()) {
            return direct;
        }
        // 2. Inside the located image directory (handles old relative paths).
        String filename = extractFilename(storedPath);
        File inDir = new File(getImageDirectory(), filename);
        if (inDir.exists()) {
            return inDir;
        }
        // 3. Relative to the current working directory.
        File relative = Paths.get("").toAbsolutePath().resolve(storedPath).toFile();
        if (relative.exists()) {
            return relative;
        }
        return null;
    }

    /**
     * Deletes the image file referenced by the stored path from all candidate
     * locations. Failures are logged but not thrown.
     */
    public static void deleteImageFile(String storedPath) {
        if (storedPath == null || storedPath.isEmpty()) {
            return;
        }
        // Delete the directly referenced file.
        deleteQuietly(new File(storedPath));
        // Delete inside the located image directory (handles old relative paths).
        String filename = extractFilename(storedPath);
        deleteQuietly(new File(getImageDirectory(), filename));
        // Delete relative to the current working directory.
        deleteQuietly(Paths.get("").toAbsolutePath().resolve(storedPath).toFile());
    }

    /**
     * Deletes every image file in the given list from disk.
     */
    public static void deleteImageFiles(java.util.List<String> storedPaths) {
        if (storedPaths == null) return;
        for (String path : storedPaths) {
            deleteImageFile(path);
        }
    }

    private static void deleteQuietly(File file) {
        try {
            if (file.exists() && !file.delete()) {
                System.err.println("Failed to delete image file: " + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            System.err.println("Error deleting image file " + file.getAbsolutePath() + ": " + ex.getMessage());
        }
    }

    /**
     * Extracts the file name (last path segment) from a stored path.
     */
    public static String extractFilename(String storedPath) {
        if (storedPath == null) return "";
        int idx = Math.max(storedPath.lastIndexOf('/'), storedPath.lastIndexOf('\\'));
        return idx >= 0 ? storedPath.substring(idx + 1) : storedPath;
    }
}