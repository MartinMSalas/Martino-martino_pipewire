package com.audio.mixer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Guarda y lee los presets (Música, Docs, Películas, etc.) en un archivo
 * presets.json en el directorio de trabajo de la app. Cada preset es
 * simplemente un array de volúmenes por canal, en el mismo orden que
 * reporta pactl para el sink configurado.
 */
@Component
public class PresetStore {

    private static final Path PRESET_PATH =
            Paths.get(System.getProperty("user.home"), ".mixer-app", "presets.json");

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, List<Integer>> loadAll() throws Exception {
        if (!Files.exists(PRESET_PATH)) {
            return new HashMap<>();
        }
        return mapper.readValue(
                PRESET_PATH.toFile(),
                new TypeReference<Map<String, List<Integer>>>() {}
        );
    }

    public void save(String name, List<Integer> volumes) throws Exception {
        Map<String, List<Integer>> all = loadAll();
        all.put(name, volumes);

        Files.createDirectories(PRESET_PATH.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(PRESET_PATH.toFile(), all);
    }
}
