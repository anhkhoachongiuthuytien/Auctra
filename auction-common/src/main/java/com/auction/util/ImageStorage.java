package com.auction.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

/**
 * Handles product image references stored in items.image_path.
 *
 * New images are stored as portable Base64 references so they can travel through
 * the socket API and be rendered on another client machine. Old absolute file
 * paths are still supported as a fallback for existing local data.
 */
public final class ImageStorage {
    private static final String INLINE_PREFIX = "auction-image:";
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_IMAGE_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/gif"
    );
    private static final Path STORAGE_DIR = Paths.get(System.getProperty("user.home"), ".auctionx", "images");

    private ImageStorage() { }

    public static Path getStorageDir() {
        return STORAGE_DIR;
    }

    /**
     * Converts an image file into a portable reference for storing in DB/socket DTOs.
     *
     * Kept under the old method name so existing callers continue to compile.
     */
    public static String copyIntoStorage(Path sourceFile) throws IOException {
        return toPortableReference(sourceFile);
    }

    public static String toPortableReference(Path sourceFile) throws IOException {
        if (sourceFile == null || !Files.exists(sourceFile) || !Files.isRegularFile(sourceFile)) {
            throw new IOException("File anh khong ton tai");
        }
        long size = Files.size(sourceFile);
        if (size > MAX_IMAGE_BYTES) {
            throw new IOException("File anh qua lon, toi da " + (MAX_IMAGE_BYTES / 1024 / 1024) + " MB");
        }
        String mimeType = detectMimeType(sourceFile);
        byte[] bytes = Files.readAllBytes(sourceFile);
        return INLINE_PREFIX + mimeType + ":" + Base64.getEncoder().encodeToString(bytes);
    }

    public static boolean isInlineImage(String reference) {
        return reference != null && reference.startsWith(INLINE_PREFIX);
    }

    public static byte[] decodeInlineImage(String reference) throws IOException {
        if (!isInlineImage(reference)) {
            throw new IOException("Khong phai anh portable");
        }
        int mimeStart = INLINE_PREFIX.length();
        int dataStart = reference.indexOf(':', mimeStart);
        if (dataStart <= mimeStart || dataStart == reference.length() - 1) {
            throw new IOException("Du lieu anh portable khong hop le");
        }
        String mimeType = reference.substring(mimeStart, dataStart);
        if (!SUPPORTED_IMAGE_MIME_TYPES.contains(mimeType)) {
            throw new IOException("Dinh dang anh khong duoc ho tro");
        }
        try {
            return Base64.getDecoder().decode(reference.substring(dataStart + 1));
        } catch (IllegalArgumentException e) {
            throw new IOException("Du lieu anh base64 khong hop le", e);
        }
    }

    /** Checks whether a value points to a loadable image reference. */
    public static boolean exists(String reference) {
        if (reference == null || reference.isBlank()) {
            return false;
        }
        if (isInlineImage(reference)) {
            try {
                decodeInlineImage(reference);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        try {
            return Files.exists(Paths.get(reference));
        } catch (Exception e) {
            return false;
        }
    }

    private static String detectMimeType(Path sourceFile) throws IOException {
        String probed = Files.probeContentType(sourceFile);
        if (probed != null && SUPPORTED_IMAGE_MIME_TYPES.contains(probed)) {
            return probed;
        }
        String name = sourceFile.getFileName() == null
                ? ""
                : sourceFile.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        throw new IOException("Chi ho tro anh PNG, JPG/JPEG hoac GIF");
    }
}
