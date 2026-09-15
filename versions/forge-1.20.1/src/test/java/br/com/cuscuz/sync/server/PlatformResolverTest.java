package br.com.cuscuz.sync.server;

import br.com.cuscuz.sync.model.ManifestMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformResolverTest {
    private static final String SHA1 = "0123456789abcdef0123456789abcdef01234567";
    private static final String SHA512 = "a".repeat(128);

    @Test
    void appliesExactModrinthFileAndEnvironment() throws Exception {
        ArtifactIdentity artifact = new ArtifactIdentity(Path.of("create.jar"), "create.jar", 12, SHA1, SHA512, 42);
        ManifestMod mod = new ManifestMod();
        mod.modId = "create";
        String json = """
                {
                  "id":"version123", "project_id":"project1", "environment":"client_and_server",
                  "game_versions":["1.20.1"], "loaders":["forge"],
                  "files":[{"filename":"create.jar", "size":12,
                    "url":"https://cdn.modrinth.com/data/project1/versions/version123/create.jar",
                    "hashes":{"sha1":"%s", "sha512":"%s"}}]
                }
                """.formatted(SHA1, SHA512);

        assertTrue(ModrinthResolver.apply(JsonParser.parseString(json).getAsJsonObject(), artifact, mod));
        assertEquals(ManifestMod.Platform.MODRINTH, mod.platform);
        assertEquals("project1", mod.projectId);
        assertEquals("version123", mod.versionId);
        assertTrue(PlatformResolver.hasDownloadSource(mod));
    }

    @Test
    void appliesExactCurseForgeFile() throws Exception {
        ArtifactIdentity artifact = new ArtifactIdentity(Path.of("only-cf.jar"), "only-cf.jar", 12, SHA1, SHA512, 4_000_000_001L);
        ManifestMod mod = new ManifestMod();
        mod.modId = "only_cf";
        String json = """
                [{"id":1234,"file":{"id":7654,"modId":1234,"fileFingerprint":4000000001,
                  "fileName":"only-cf.jar","fileLength":12,
                  "downloadUrl":"https://edge.forgecdn.net/files/7/654/only-cf.jar",
                  "hashes":[{"value":"%s","algo":1}]}}]
                """.formatted(SHA1);
        JsonArray matches = JsonParser.parseString(json).getAsJsonArray();

        assertTrue(CurseForgeResolver.apply(matches, artifact, mod));
        assertEquals(ManifestMod.Platform.CURSEFORGE, mod.platform);
        assertEquals("1234", mod.projectId);
        assertEquals(7654, mod.fileId);
        assertTrue(PlatformResolver.hasDownloadSource(mod));
    }

    @Test
    void refreshIntervalIsFiveMinutes() {
        assertEquals(5, ManifestRefreshService.REFRESH_MINUTES);
    }

    @Test
    void mapsLegacyModrinthProjectSideMetadata() {
        ManifestMod mod = new ManifestMod();
        ModrinthResolver.applyProjectEnvironment(JsonParser.parseString(
                "{\"client_side\":\"unsupported\",\"server_side\":\"required\"}").getAsJsonObject(), mod);
        assertEquals(ManifestMod.Environment.SERVER, mod.environment);
    }

    @Test
    void copiesEnvironmentForAnotherModIdInTheSameJar() {
        ManifestMod source = new ManifestMod();
        source.environment = ManifestMod.Environment.SERVER;
        source.platform = ManifestMod.Platform.MODRINTH;
        source.projectId = "project";
        source.versionId = "version";

        ManifestMod target = new ManifestMod();
        PlatformResolver.copySource(source, target);

        assertEquals(ManifestMod.Environment.SERVER, target.environment);
        assertEquals(ManifestMod.Platform.MODRINTH, target.platform);
    }

    @Test
    void normalizesCurseForgeApiKeysCopiedFromAControlPanel() {
        assertEquals("secret-key", ServerSettings.normalized("  secret-key\r\n"));
        assertEquals("", ServerSettings.normalized(null));
    }
}
