package controller;

public class InputController {

    private boolean manualMode;

    public InputController() {

        manualMode = false;
    }

    public void enableManualMode() {

        manualMode = true;
    }

    public void enableAutoMode() {

        manualMode = false;
    }

    public boolean isManualMode() {

        return manualMode;
    }

    public void toggleMode() {

        manualMode = !manualMode;
    }
}