package com.smartcity.gui;

import javafx.scene.media.AudioClip;
import java.net.URL;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiChannel;

public class SoundManager {
    private AudioClip hornClip;
    private AudioClip sirenClip;
    private AudioClip turnClip;

    private boolean soundEnabled = true;
    private Synthesizer synthesizer;
    private MidiChannel midiChannel;

    public SoundManager() {
        // Try to load audio files
        try {
            URL hornUrl = getClass().getResource("/com/smartcity/gui/assets/sounds/horn.wav");
            if (hornUrl != null) hornClip = new AudioClip(hornUrl.toExternalForm());

            URL sirenUrl = getClass().getResource("/com/smartcity/gui/assets/sounds/siren.wav");
            if (sirenUrl != null) sirenClip = new AudioClip(sirenUrl.toExternalForm());

            URL turnUrl = getClass().getResource("/com/smartcity/gui/assets/sounds/turn_signal.wav");
            if (turnUrl != null) turnClip = new AudioClip(turnUrl.toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load audio files: " + e.getMessage() + ". Using MIDI fallback.");
        }

        // Initialize MIDI synthesizer as fallback
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel[] channels = synthesizer.getChannels();
            if (channels != null && channels.length > 0) {
                midiChannel = channels[0];
                // Set instrument to a synth/brass for horn, or default
                midiChannel.programChange(80); // 80 = Synth Lead
            }
        } catch (Exception e) {
            System.err.println("MIDI initialization failed: " + e.getMessage());
        }
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void playHorn() {
        if (!soundEnabled) return;

        if (hornClip != null) {
            hornClip.play();
        } else if (midiChannel != null) {
            // MIDI fallback for horn (loud chord)
            new Thread(() -> {
                try {
                    midiChannel.noteOn(60, 90); // middle C
                    midiChannel.noteOn(64, 90); // E
                    Thread.sleep(250);
                    midiChannel.noteOff(60);
                    midiChannel.noteOff(64);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    public void playSiren() {
        if (!soundEnabled) return;

        if (sirenClip != null) {
            sirenClip.play();
        } else if (midiChannel != null) {
            // MIDI fallback for siren (alternating frequencies)
            new Thread(() -> {
                try {
                    for (int i = 0; i < 4; i++) {
                        midiChannel.noteOn(72, 80); // High C
                        Thread.sleep(150);
                        midiChannel.noteOff(72);
                        midiChannel.noteOn(77, 80); // F
                        Thread.sleep(150);
                        midiChannel.noteOff(77);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    public void playTurnSignal() {
        if (!soundEnabled) return;

        if (turnClip != null) {
            turnClip.play();
        } else if (midiChannel != null) {
            // MIDI fallback for turn signal (short high tick)
            new Thread(() -> {
                try {
                    midiChannel.noteOn(84, 40); // very high soft note
                    Thread.sleep(50);
                    midiChannel.noteOff(84);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    public void stopAllSounds() {
        try {
            if (hornClip != null && hornClip.isPlaying()) hornClip.stop();
            if (sirenClip != null && sirenClip.isPlaying()) sirenClip.stop();
            if (turnClip != null && turnClip.isPlaying()) turnClip.stop();
            if (midiChannel != null) {
                midiChannel.allNotesOff();
            }
        } catch (Exception e) {
            // safe shutdown
        }
    }
}