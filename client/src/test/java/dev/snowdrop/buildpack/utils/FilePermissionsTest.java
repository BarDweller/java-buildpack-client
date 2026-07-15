package dev.snowdrop.buildpack.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FilePermissionsTest {

    @Test
    void testGetPermissions(@TempDir Path tempDir) throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "test-perms", ".tmp");
        File file = tempFile.toFile();

        FilePermissions filePermissions = new FilePermissions();
        Integer mode = filePermissions.getPermissions(file);
        assertNotNull(mode);

        if (tempFile.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            // Test POSIX conversion
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(tempFile, permissions);

            Integer posixMode = filePermissions.getPermissions(file);
            // 0400 + 0200 + 0100 = 0700 (octal) -> 448 in decimal
            assertEquals(0700, posixMode);
        } else {
            // Test fallback behavior
            file.setReadable(true);
            file.setWritable(true);
            file.setExecutable(true);

            Integer fallbackMode = filePermissions.getPermissions(file);
            // Owner and Group read/write/exec:
            // Owner: 0400 + 0200 + 0100 = 0700
            // Group: 040 + 020 + 010 = 070
            // Total: 0770 -> 504 in decimal
            assertEquals(0770, fallbackMode);
        }
    }
}
