package br.com.cuscuz.sync.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class ServerSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    String note = "Modrinth funciona sem chave. Para procurar também no CurseForge, cole a API key em curseForgeApiKey; a alteração é relida automaticamente em até 5 minutos.";
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
        String systemProperty = normalized(System.getProperty("cuscuzSync.curseForgeApiKey"));
        if (!systemProperty.isEmpty()) {
            return systemProperty;
        }
        String environment = normalized(System.getenv("CURSEFORGE_API_KEY"));
        if (!environment.isEmpty()) {
            return environment;
        }
        String shortEnvironment = normalized(System.getenv("CF_API_KEY"));
        return shortEnvironment.isEmpty() ? normalized(curseForgeApiKey) : shortEnvironment;
    }

    static String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}
