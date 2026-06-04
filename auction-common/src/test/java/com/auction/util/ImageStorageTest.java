package com.auction.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void toPortableReferenceStoresImageBytesInline() throws Exception {
        byte[] bytes = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 1, 2, 3};
        Path image = tempDir.resolve("item.png");
        Files.write(image, bytes);

        String reference = ImageStorage.toPortableReference(image);

        assertTrue(ImageStorage.isInlineImage(reference));
        assertTrue(ImageStorage.exists(reference));
        assertFalse(reference.contains(";"));
        assertArrayEquals(bytes, ImageStorage.decodeInlineImage(reference));
    }
}
