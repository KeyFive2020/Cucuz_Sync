package br.com.cuscuz.sync.install;

import br.com.cuscuz.sync.model.ManifestMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DownloadServiceTest {
    @Test
    void selectsDeclaredHashInsteadOfPrimaryFile() {
        ManifestMod mod = new ManifestMod();
        mod.sha1 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        JsonArray files = JsonParser.parseString("""
                [
                  {"filename":"wrong.jar","primary":true,"hashes":{"sha1":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}},
                  {"filename":"right.jar","primary":false,"hashes":{"sha1":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}}
                ]
                """).getAsJsonArray();

        JsonObject selected = DownloadService.selectExactFile(files, mod);
        assertEquals("right.jar", selected.get("filename").getAsString());
    }
}
