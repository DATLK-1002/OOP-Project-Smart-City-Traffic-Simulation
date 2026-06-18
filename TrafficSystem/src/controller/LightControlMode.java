package controller;

public enum LightControlMode {
    AUTOMATIC("Tự động"),
    MANUAL("Thủ công");

    private final String label;

    LightControlMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}