package com.auction.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Quản lý nơi lưu ảnh sản phẩm. Mỗi file được copy vào thư mục
 * ~/.auctionx/images/ với tên ngẫu nhiên để tránh trùng/lộ tên gốc.
 * Đường dẫn tuyệt đối này được lưu trong DB (cột items.image_path).
 */
public final class ImageStorage {
    private static final Path STORAGE_DIR = Paths.get(System.getProperty("user.home"), ".auctionx", "images");

    private ImageStorage() { }

    public static Path getStorageDir() {
        return STORAGE_DIR;
    }

    /**
     * Copy file ảnh vào thư mục lưu trữ. Trả về đường dẫn tuyệt đối để lưu trong DB.
     */
    public static String copyIntoStorage(Path sourceFile) throws IOException {
        if (sourceFile == null || !Files.exists(sourceFile)) {
            throw new IOException("File ảnh không tồn tại");
        }
        Files.createDirectories(STORAGE_DIR);

        String fileName = sourceFile.getFileName().toString();
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            ext = fileName.substring(dot).toLowerCase(Locale.ROOT);
        }

        String target = UUID.randomUUID().toString().replace("-", "") + ext;
        Path targetPath = STORAGE_DIR.resolve(target);
        Files.copy(sourceFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath.toAbsolutePath().toString();
    }

    /** Kiểm tra đường dẫn có trỏ tới file ảnh tồn tại không. */
    public static boolean exists(String path) {
        if (path == null || path.isBlank()) return false;
        try {
            return Files.exists(Paths.get(path));
        } catch (Exception e) {
            return false;
        }
    }
}
