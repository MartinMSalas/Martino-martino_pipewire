package com.audio.mixer;

/**
 * Representa un canal de audio tal como lo reporta pactl:
 * - label: nombre técnico exacto (ej. "front-left", "low-frequency-effects")
 * - shortName: abreviatura para mostrar en la UI (ej. "FL", "LFE")
 * - percent: volumen actual en porcentaje
 */
public record ChannelVolume(String label, String shortName, int percent) {
}
