package br.com.cuscuz.sync.client;

import br.com.cuscuz.sync.manifest.ManifestCodec;
import br.com.cuscuz.sync.manifest.ManifestException;
import br.com.cuscuz.sync.model.SyncManifest;
import br.com.cuscuz.sync.status.StatusManifestCodec;
import br.com.cuscuz.sync.status.StatusPingProtocol;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class StatusPingManifestClient {
    private static final int MINECRAFT_1_20_1_PROTOCOL = 763;
    private static final int MAX_PACKET_BYTES = 128 * 1024;
    private static final int CONNECT_TIMEOUT_MILLIS = 8_000;
    private static final int READ_TIMEOUT_MILLIS = 15_000;

    private StatusPingManifestClient() {
    }

    public static SyncManifest fetch(String address) throws IOException, ManifestException {
        ServerAddress parsed = ServerAddress.parseString(address);
        ResolvedServerAddress resolved = ServerNameResolver.DEFAULT.resolveAddress(parsed)
                .orElseThrow(() -> new IOException("Não foi possível resolver o endereço do servidor."));
        try (Socket socket = new Socket()) {
            socket.connect(resolved.asInetSocketAddress(), CONNECT_TIMEOUT_MILLIS);
            socket.setSoTimeout(READ_TIMEOUT_MILLIS);
            try (DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                 DataInputStream input = new DataInputStream(socket.getInputStream())) {
                writeHandshake(output, parsed);
                writePacket(output, new byte[]{0});
                output.flush();
                return decodeStatusJson(readStatusJson(input));
            }
        }
    }

    static SyncManifest decodeStatusJson(String json) throws IOException, ManifestException {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Servidor respondeu um status JSON inválido.", exception);
        }
        if (!root.has(StatusManifestCodec.JSON_FIELD)
                || !root.get(StatusManifestCodec.JSON_FIELD).isJsonPrimitive()
                || !root.get(StatusManifestCodec.JSON_FIELD).getAsJsonPrimitive().isString()) {
            throw new IOException("Servidor não publicou o manifesto no Server List Ping.");
        }
        byte[] manifestBytes;
        try {
            manifestBytes = StatusManifestCodec.decode(root.get(StatusManifestCodec.JSON_FIELD).getAsString());
        } catch (IOException exception) {
            throw new ManifestException("Manifesto do Server List Ping está corrompido.", exception);
        }
        return ManifestCodec.decode(manifestBytes);
    }

    private static void writeHandshake(DataOutputStream output, ServerAddress address) throws IOException {
        ByteArrayOutputStream packetBytes = new ByteArrayOutputStream();
        try (DataOutputStream packet = new DataOutputStream(packetBytes)) {
            writeVarInt(packet, 0);
            writeVarInt(packet, MINECRAFT_1_20_1_PROTOCOL);
            writeString(packet, StatusPingProtocol.markedHost(address.getHost()));
            packet.writeShort(address.getPort());
            writeVarInt(packet, 1);
        }
        writePacket(output, packetBytes.toByteArray());
    }

    private static void writePacket(DataOutputStream output, byte[] packet) throws IOException {
        writeVarInt(output, packet.length);
        output.write(packet);
    }

    private static String readStatusJson(DataInputStream input) throws IOException {
        int packetLength = readVarInt(input);
        if (packetLength < 2 || packetLength > MAX_PACKET_BYTES) {
            throw new IOException("Resposta de status possui tamanho inválido: " + packetLength + '.');
        }
        byte[] packet = input.readNBytes(packetLength);
        if (packet.length != packetLength) {
            throw new EOFException("Resposta de status terminou antes do esperado.");
        }
        try (DataInputStream packetInput = new DataInputStream(new java.io.ByteArrayInputStream(packet))) {
            if (readVarInt(packetInput) != 0) {
                throw new IOException("Servidor respondeu um pacote de status inesperado.");
            }
            int jsonBytes = readVarInt(packetInput);
            if (jsonBytes <= 0 || jsonBytes > MAX_PACKET_BYTES || jsonBytes > packetInput.available()) {
                throw new IOException("JSON de status possui tamanho inválido: " + jsonBytes + '.');
            }
            byte[] bytes = packetInput.readNBytes(jsonBytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (value.length() > 255 || bytes.length > 255 * 3) {
            throw new IOException("Endereço do servidor é grande demais.");
        }
        writeVarInt(output, bytes.length);
        output.write(bytes);
    }

    private static void writeVarInt(DataOutputStream output, int value) throws IOException {
        do {
            int part = value & 0x7F;
            value >>>= 7;
            if (value != 0) {
                part |= 0x80;
            }
            output.writeByte(part);
        } while (value != 0);
    }

    private static int readVarInt(DataInputStream input) throws IOException {
        int result = 0;
        for (int position = 0; position < 5; position++) {
            int current = input.readUnsignedByte();
            result |= (current & 0x7F) << (position * 7);
            if ((current & 0x80) == 0) {
                return result;
            }
        }
        throw new IOException("VarInt de status é grande demais.");
    }
}
