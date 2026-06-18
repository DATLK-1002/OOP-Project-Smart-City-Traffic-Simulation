package util;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.media.AudioClip;
import model.vehicle.Ambulance;
import model.vehicle.Bicycle;
import model.vehicle.FireTruck;
import model.vehicle.Motorbike;
import model.vehicle.Vehicle;

/**
 * Quản lý phát âm thanh giao thông bằng file WAV thật.
 * Có cache clip và cooldown để tránh phát dồn nhiều lần trong cùng một khoảng thời gian ngắn.
 */
public class SoundManager {

    private static final String RESOURCE_ROOT = "/assets/sounds/";

    private static final Map<String, String> FILE_NAMES = Map.of(
            "horn", "horn.wav",
            "siren", "siren.wav",
            "traffic-light-change", "traffic-light-change.wav",
            "turn-signal", "turn-signal.wav",
            "car-engine", "car-engine.wav",
            "motorbike-engine", "motorbike-engine.wav",
            "bicycle-bell", "bicycle-bell.wav"
    );

    private static final Map<String, Long> COOLDOWNS_MS = Map.of(
            "horn", 650L,
            "siren", 1100L,
            "traffic-light-change", 220L,
            "turn-signal", 240L,
            "car-engine", 1300L,
            "motorbike-engine", 1000L,
            "bicycle-bell", 2200L
    );

    private static final Map<String, Double> VOLUMES = Map.of(
            "horn", 0.75,
            "siren", 0.82,
            "traffic-light-change", 0.40,
            "turn-signal", 0.58,
            "car-engine", 0.18,
            "motorbike-engine", 0.20,
            "bicycle-bell", 0.35
    );

    private static final Map<String, AudioClip> CLIP_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_PLAYED_AT = new ConcurrentHashMap<>();

    private boolean enabled;

    public SoundManager() {
        enabled = true;
    }

    public void playHorn() {
        play("horn");
    }

    public void playSiren() {
        play("siren");
    }

    public void playTrafficLightChange() {
        play("traffic-light-change");
    }

    public void playTurnSignal() {
        play("turn-signal");
    }

    public void playVehicleSound(Vehicle vehicle) {
        if (vehicle == null || vehicle.getSpeed() <= 0) {
            return;
        }

        if (vehicle.isLeftIndicatorOn() || vehicle.isRightIndicatorOn()) {
            playTurnSignal();
        }

        if (vehicle instanceof Ambulance || vehicle instanceof FireTruck) {
            playSiren();
        } else if (vehicle instanceof Motorbike) {
            play("motorbike-engine");
        } else if (vehicle instanceof Bicycle) {
            play("bicycle-bell");
        } else {
            play("car-engine");
        }
    }

    public void play(String soundName) {
        if (!enabled || soundName == null || soundName.trim().isEmpty()) {
            return;
        }

        String key = soundName.trim();
        if (!canPlayNow(key)) {
            return;
        }

        AudioClip clip = clipFor(key);
        if (clip == null) {
            System.out.println("[Sound missing] " + key);
            return;
        }

        LAST_PLAYED_AT.put(key, System.currentTimeMillis());
        clip.play(VOLUMES.getOrDefault(key, 0.50));
    }

    private boolean canPlayNow(String soundName) {
        long now = System.currentTimeMillis();
        long last = LAST_PLAYED_AT.getOrDefault(soundName, 0L);
        long cooldown = COOLDOWNS_MS.getOrDefault(soundName, 250L);
        return now - last >= cooldown;
    }

    private AudioClip clipFor(String soundName) {
        return CLIP_CACHE.computeIfAbsent(soundName, this::loadClip);
    }

    private AudioClip loadClip(String soundName) {
        String fileName = FILE_NAMES.get(soundName);
        if (fileName == null) {
            return null;
        }

        try {
            URL resource = SoundManager.class.getResource(RESOURCE_ROOT + fileName);
            if (resource != null) {
                return new AudioClip(resource.toExternalForm());
            }
        } catch (Exception ignored) {
            // thử fallback ở filesystem bên dưới
        }

        try {
            File file = new File("src/assets/sounds/" + fileName);
            if (file.exists()) {
                return new AudioClip(file.toURI().toString());
            }
        } catch (Exception ignored) {
            // fallback cuối cùng: null
        }

        return null;
    }

    public void mute() {
        enabled = false;
    }

    public void unmute() {
        enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
