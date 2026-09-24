package com.audio.mixer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VolumeController {

    private final PipewireVolumeService volumeService;

    public VolumeController(PipewireVolumeService volumeService) {
        this.volumeService = volumeService;
    }

    /**
     * Estado completo del sink: un objeto por canal, en el orden real
     * que reporta pactl, con label técnico + abreviatura + volumen actual.
     */
    @GetMapping("/state")
    public List<ChannelVolume> getState() throws Exception {
        return volumeService.getChannelVolumes();
    }

    @PostMapping("/volumes/{index}")
    public void setChannel(@PathVariable int index, @RequestBody Map<String, Integer> body) throws Exception {
        Integer value = body.get("value");
        if (value == null) {
            throw new IllegalArgumentException("Falta el campo 'value' en el body");
        }
        volumeService.setChannel(index, value);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleServerError(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
    }
}
