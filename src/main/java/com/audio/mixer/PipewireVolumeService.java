package com.audio.mixer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee y modifica el volumen por canal de un sink de PipeWire/PulseAudio
 * usando el comando `pactl`. El orden y nombre de los canales se toma
 * SIEMPRE de lo que reporta pactl en ese momento (no se asume un orden
 * fijo como FL/FR/FC/LFE/RL/RR), porque distintos sinks (virtual vs.
 * hardware ALSA) pueden reportarlos en orden distinto.
 */
@Service
public class PipewireVolumeService {

    @Value("${mixer.sink-name}")
    private String sinkName;

    // Ej: "front-left: 65536 / 100% / 0.00 dB" -> captura "front-left" y "100"
    private static final Pattern CHANNEL_ENTRY =
            Pattern.compile("([a-zA-Z][a-zA-Z-]*):\\s*\\d+\\s*/\\s*(\\d+)%");

    private static final Map<String, String> SHORT_NAMES = Map.ofEntries(
            Map.entry("front-left", "FL"),
            Map.entry("front-right", "FR"),
            Map.entry("front-center", "FC"),
            Map.entry("center", "FC"),
            Map.entry("low-frequency-effects", "LFE"),
            Map.entry("lfe", "LFE"),
            Map.entry("rear-left", "RL"),
            Map.entry("rear-right", "RR"),
            Map.entry("side-left", "SL"),
            Map.entry("side-right", "SR"),
            Map.entry("mono", "MONO")
    );

    /**
     * Devuelve los canales del sink, en el mismo orden en que pactl los reporta,
     * con su nombre técnico, su abreviatura para mostrar, y su volumen actual.
     */
    public List<ChannelVolume> getChannelVolumes() throws Exception {
        String output = runCommand("pactl", "list", "sinks");
        String[] lines = output.split("\n");

        boolean inTargetSink = false;
        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.startsWith("Name: " + sinkName)) {
                inTargetSink = true;
                continue;
            }
            if (inTargetSink && line.startsWith("Name: ")) {
                break;
            }
            if (inTargetSink && line.startsWith("Volume:")) {
                List<ChannelVolume> channels = parseChannelLine(line);
                if (!channels.isEmpty()) {
                    return channels;
                }
            }
        }
        throw new IllegalStateException(
                "No se encontró el sink '" + sinkName + "' o no tiene línea de Volume. " +
                "Verificá el nombre con: pactl list sinks short");
    }

    /**
     * Aplica un array completo de volúmenes (uno por canal, en el mismo
     * orden que devuelve getChannelVolumes) al sink.
     */
    public void setVolumes(List<Integer> percentages) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("pactl");
        command.add("set-sink-volume");
        command.add(sinkName);
        for (int p : percentages) {
            command.add(Math.max(0, p) + "%");
        }
        runCommand(command.toArray(new String[0]));
    }

    /**
     * Cambia el volumen de un solo canal (por índice, según el orden actual
     * reportado por pactl), preservando el resto. pactl no permite tocar
     * un canal aislado directamente, así que leemos el estado actual,
     * lo modificamos, y reenviamos todo el array en el mismo orden.
     */
    public void setChannel(int index, int percent) throws Exception {
        List<ChannelVolume> channels = getChannelVolumes();
        if (index < 0 || index >= channels.size()) {
            throw new IllegalArgumentException(
                    "Índice de canal inválido: " + index + " (el sink tiene " + channels.size() + " canales)");
        }
        List<Integer> volumes = new ArrayList<>();
        for (ChannelVolume c : channels) {
            volumes.add(c.percent());
        }
        volumes.set(index, percent);
        setVolumes(volumes);
    }

    private List<ChannelVolume> parseChannelLine(String line) {
        List<ChannelVolume> result = new ArrayList<>();
        Matcher m = CHANNEL_ENTRY.matcher(line);
        while (m.find()) {
            String label = m.group(1);
            int percent = Integer.parseInt(m.group(2));
            String shortName = SHORT_NAMES.getOrDefault(label, label.toUpperCase());
            result.add(new ChannelVolume(label, shortName, percent));
        }
        return result;
    }

    private String runCommand(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Comando falló (" + exitCode + "): " + command[0] + " -> " + sb);
        }
        return sb.toString();
    }
}
