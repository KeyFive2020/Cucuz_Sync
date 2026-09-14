package br.com.cuscuz.sync.model;

import java.util.ArrayList;
import java.util.List;

/** Contrato completo publicado pelo servidor. */
public final class SyncManifest {
    public int schemaVersion = 1;
    public String profileId = "default";
    public String name = "Servidor Minecraft";
    public String minecraftVersion = "1.20.1";
    public String loader = "forge";
    public String manifestVersion = "1";
    public String generatedAt = "";
    public List<String> allowedClientMods = new ArrayList<>();
    public List<ManifestMod> mods = new ArrayList<>();
}
