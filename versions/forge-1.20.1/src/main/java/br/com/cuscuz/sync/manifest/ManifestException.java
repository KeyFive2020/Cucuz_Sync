package br.com.cuscuz.sync.manifest;

public final class ManifestException extends Exception {
    public ManifestException(String message) {
        super(message);
    }

    public ManifestException(String message, Throwable cause) {
        super(message, cause);
    }
}
