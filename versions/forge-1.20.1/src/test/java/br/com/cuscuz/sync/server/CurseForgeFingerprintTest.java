package br.com.cuscuz.sync.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurseForgeFingerprintTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void matchesKnownMurmur2Vector() throws Exception {
        Path file = temporaryDirectory.resolve("sample.jar");
        Files.writeString(file, "foo");
        assertEquals(197_930_586L, CurseForgeFingerprint.compute(file));
    }

    @Test
    void ignoresCurseForgeWhitespaceBytes() throws Exception {
        Path compact = temporaryDirectory.resolve("compact.jar");
        Path spaced = temporaryDirectory.resolve("spaced.jar");
        Files.writeString(compact, "foobar");
        Files.writeString(spaced, "f o\to\r\nb\r a r");
        assertEquals(CurseForgeFingerprint.compute(compact), CurseForgeFingerprint.compute(spaced));
    }
}
