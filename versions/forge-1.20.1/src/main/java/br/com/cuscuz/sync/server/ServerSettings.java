package br.com.cuscuz.sync.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class ServerSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    String note = "A chave CurseForge é opcional. Modrinth funciona sem chave; CurseForge é usado como fallback.";
    String curseForgeApiKey = "";

    static ServerSettings loadOrCreate(Path directory) throws IOException {
        Path path = directory.resolve("server-settings.json");
        if (!Files.exists(path)) {
            ServerSettings settings = new ServerSettings();
            Files.writeString(path, GSON.toJson(settings), StandardCharsets.UTF_8);
            return settings;
        }
        ServerSettings settings = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), ServerSettings.class);
        return settings == null ? new ServerSettings() : settings;
    }

    String effectiveCurseForgeApiKey() {
        String systemProperty = System.getProperty("cuscuzSync.curseForgeApiKey", "");
        if (!systemProperty.isBlank()) {
            return systemProperty;
        }
        String environment = System.getenv("CURSEFORGE_API_KEY");
        return environment == null || environment.isBlank() ? curseForgeApiKey : environment;
    }
}
