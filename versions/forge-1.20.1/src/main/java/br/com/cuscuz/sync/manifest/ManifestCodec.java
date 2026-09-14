package br.com.cuscuz.sync.manifest;

import br.com.cuscuz.sync.model.ManifestMod;
import br.com.cuscuz.sync.model.SyncManifest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ManifestCodec {
    public static final int MAX_MANIFEST_BYTES = 2 * 1024 * 1024;
    private static final int MAX_MODS = 2_000;
    private static final Pattern SAFE_ID = Pattern.compile("[a-z][a-z0-9_-]{1,127}");
    private static final Pattern SHA1 = Pattern.compile("[a-fA-F0-9]{40}");
    private static final Pattern SHA512 = Pattern.compile("[a-fA-F0-9]{128}");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ManifestCodec() {
    }

    public static SyncManifest decode(byte[] bytes) throws ManifestException {
        if (bytes.length == 0 || bytes.length > MAX_MANIFEST_BYTES) {
            throw new ManifestException("O manifesto está vazio ou excede 2 MiB.");
        }
        try {
            SyncManifest manifest = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), SyncManifest.class);
            validate(manifest);
            return manifest;
        } catch (JsonParseException | NullPointerException exception) {
            throw new ManifestException("O manifesto não é um JSON válido.", exception);
        }
    }

    public static byte[] encode(SyncManifest manifest) throws ManifestException {
        validate(manifest);
        return GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8);
    }

    public static void validate(SyncManifest manifest) throws ManifestException {
        if (manifest == null) {
            throw new ManifestException("Manifesto ausente.");
        }
        if (manifest.schemaVersion != 1) {
            throw new ManifestException("Versão de esquema não suportada: " + manifest.schemaVersion);
        }
        if (manifest.profileId == null || !SAFE_ID.matcher(manifest.profileId).matches()) {
            throw new ManifestException("profileId inválido.");
        }
        if (!"1.20.1".equals(manifest.minecraftVersion) || !"forge".equalsIgnoreCase(manifest.loader)) {
            throw new ManifestException("Este arquivo precisa declarar Minecraft 1.20.1 e Forge.");
        }
        if (manifest.mods == null || manifest.mods.size() > MAX_MODS) {
            throw new ManifestException("Lista de mods ausente ou grande demais.");
        }
        if (manifest.allowedClientMods == null) {
            throw new ManifestException("allowedClientMods precisa ser uma lista.");
        }

        for (String allowedId : manifest.allowedClientMods) {
            if (allowedId == null || !SAFE_ID.matcher(allowedId).matches()) {
                throw new ManifestException("Há um modId inválido em allowedClientMods.");
            }
        }

        Set<String> ids = new HashSet<>();
        for (ManifestMod mod : manifest.mods) {
            validateMod(mod, ids);
        }
    }

    private static void validateMod(ManifestMod mod, Set<String> ids) throws ManifestException {
        if (mod == null || mod.modId == null || !SAFE_ID.matcher(mod.modId).matches()) {
            throw new ManifestException("Há um modId inválido no manifesto.");
        }
        mod.modId = mod.modId.toLowerCase(Locale.ROOT);
        if (!ids.add(mod.modId)) {
            throw new ManifestException("modId duplicado: " + mod.modId);
        }
        if (mod.name == null || mod.name.isBlank() || mod.name.length() > 200
                || mod.version == null || mod.version.isBlank() || mod.version.length() > 100) {
            throw new ManifestException("Nome ou versão ausente em " + mod.modId + '.');
        }
        if (mod.environment == null || mod.platform == null) {
            throw new ManifestException("Ambiente ou plataforma ausente em " + mod.modId + '.');
        }
        if (mod.fileName != null && (!mod.fileName.isBlank())
                && (!isSafeJarName(mod.fileName))) {
            throw new ManifestException("Nome de arquivo inseguro em " + mod.modId + '.');
        }
        if (mod.sha1 != null && !mod.sha1.isBlank() && !SHA1.matcher(mod.sha1).matches()) {
            throw new ManifestException("SHA-1 inválido em " + mod.modId + '.');
        }
        if (mod.sha512 != null && !mod.sha512.isBlank() && !SHA512.matcher(mod.sha512).matches()) {
            throw new ManifestException("SHA-512 inválido em " + mod.modId + '.');
        }
        if (mod.platform == ManifestMod.Platform.MODRINTH
                && (mod.projectId == null || mod.projectId.isBlank() || mod.versionId == null || mod.versionId.isBlank())) {
            throw new ManifestException("Fonte Modrinth incompleta em " + mod.modId + '.');
        }
        if (mod.platform == ManifestMod.Platform.CURSEFORGE
                && (mod.projectId == null || mod.projectId.isBlank() || mod.fileId <= 0)) {
            throw new ManifestException("Fonte CurseForge incompleta em " + mod.modId + '.');
        }
    }

    public static boolean isSafeJarName(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".jar")
                && !name.contains("/") && !name.contains("\\") && !name.contains("..")
                && name.length() <= 180;
    }
}
