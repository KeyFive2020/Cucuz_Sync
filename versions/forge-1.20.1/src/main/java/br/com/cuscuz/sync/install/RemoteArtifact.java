package br.com.cuscuz.sync.install;

import java.net.URI;

record RemoteArtifact(String fileName, URI uri, long size, String sha1, String sha512) {
}
