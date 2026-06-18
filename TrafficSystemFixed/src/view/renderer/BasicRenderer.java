package view.renderer;

public class BasicRenderer implements Renderer {

    @Override
    public void render() {

        System.out.println("[BasicRenderer] Rendering frame...");
    }

    @Override
    public void clear() {

        System.out.println("[BasicRenderer] Clearing screen...");
    }
}
