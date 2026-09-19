package com.audio.mixer;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presets")
public class PresetController {

    private final PresetStore presetStore;
    private final PipewireVolumeService volumeService;

    public PresetController(PresetStore presetStore, PipewireVolumeService volumeService) {
        this.presetStore = presetStore;
        this.volumeService = volumeService;
    }

    /** Devuelve todos los presets guardados: { "musica": [80, 80, ...], "docs": [...], ... } */
    @GetMapping
    public Map<String, List<Integer>> getPresets() throws Exception {
        return presetStore.loadAll();
    }

    /** Guarda los valores actuales del preset como su nuevo default (no toca el audio real). */
    @PostMapping("/{name}")
    public void savePreset(@PathVariable String name, @RequestBody List<Integer> volumes) throws Exception {
        presetStore.save(name, volumes);
    }

    /**
     * Activa un preset: aplica los valores recibidos directamente a la salida
     * real (el sink configurado), y devuelve el estado ya actualizado para
     * que el front pueda reflejarlo en el panel "Activo".
     */
    @PostMapping("/{name}/activate")
    public List<ChannelVolume> activatePreset(@PathVariable String name, @RequestBody List<Integer> volumes) throws Exception {
        volumeService.setVolumes(volumes);
        return volumeService.getChannelVolumes();
    }
}
