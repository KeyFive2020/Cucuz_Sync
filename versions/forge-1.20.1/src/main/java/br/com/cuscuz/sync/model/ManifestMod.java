package br.com.cuscuz.sync.model;

/** Um mod administrado pelo perfil do servidor. */
public final class ManifestMod {
    public String modId = "";
    public String name = "";
    public String version = "";
    public boolean required = true;
    public Environment environment = Environment.BOTH;
    public Platform platform = Platform.NONE;
    public String projectId = "";
    public String versionId = "";
    public long fileId;
    public String fileName = "";
    public long size;
    public String sha1 = "";
    public String sha512 = "";
    public String downloadUrl = "";

    public enum Environment {
        BOTH,
        CLIENT,
        SERVER
    }

    public enum Platform {
        NONE,
        MODRINTH,
        CURSEFORGE
    }
}
