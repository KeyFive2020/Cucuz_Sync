package br.com.cuscuz.sync.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Variante MurmurHash2 usada pelo endpoint de fingerprints do CurseForge. */
final class CurseForgeFingerprint {
    private static final long MULTIPLIER = 0x5bd1e995L;

    private CurseForgeFingerprint() {
    }

    static long compute(Path path) throws IOException {
        long normalizedLength = normalizedLength(path);
        long hash = (1L ^ normalizedLength) & 0xffff_ffffL;
        long word = 0;
        int shift = 0;
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                for (int index = 0; index < read; index++) {
                    int value = buffer[index] & 0xff;
                    if (isIgnoredWhitespace(value)) {
                        continue;
                    }
                    word |= ((long) value) << shift;
                    shift += 8;
                    if (shift == 32) {
                        hash = mixWord(hash, word);
                        word = 0;
                        shift = 0;
                    }
                }
            }
        }
        if (shift > 0) {
            hash = ((hash ^ word) * MULTIPLIER) & 0xffff_ffffL;
        }
        hash = ((hash ^ (hash >>> 13)) * MULTIPLIER) & 0xffff_ffffL;
        return (hash ^ (hash >>> 15)) & 0xffff_ffffL;
    }

    private static long normalizedLength(Path path) throws IOException {
        long count = 0;
        byte[] buffer = new byte[64 * 1024];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                for (int index = 0; index < read; index++) {
                    if (!isIgnoredWhitespace(buffer[index] & 0xff)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static long mixWord(long hash, long word) {
        long first = (word * MULTIPLIER) & 0xffff_ffffL;
        long second = ((first ^ (first >>> 24)) * MULTIPLIER) & 0xffff_ffffL;
        return ((hash * MULTIPLIER) ^ second) & 0xffff_ffffL;
    }

    private static boolean isIgnoredWhitespace(int value) {
        return value == 0x09 || value == 0x0a || value == 0x0d || value == 0x20;
    }
}
