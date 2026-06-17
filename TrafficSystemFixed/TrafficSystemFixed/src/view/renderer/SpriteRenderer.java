package view.renderer;

public class SpriteRenderer implements Renderer {

    @Override
    public void render() {

        System.out.println("[SpriteRenderer] Rendering sprites...");
    }

    @Override
    public void clear() {

        System.out.println("[SpriteRenderer] Clearing sprites...");
    }
}
