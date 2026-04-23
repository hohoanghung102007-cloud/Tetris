import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import javax.imageio.ImageIO;

public class Brick {
    public static final int TILE_SIZE = 13;
    public static final int VISUAL_HEIGHT = 19;
    public static final int LIP_OFFSET = 6;

    private static HashMap<String, BufferedImage> cache = new HashMap<>();

    public double x, y;
    public BufferedImage image;

    public Brick(String color, int startX, int startY) {
        this.x = startX;
        this.y = startY;

        if (!cache.containsKey(color)) {
            try {
                BufferedImage img = ImageIO.read(new File("Textures/" + color + " Brick.png"));
                cache.put(color, img);
            } catch (Exception e) {
                System.out.println("Error loading image: " + color);
            }
        }
        this.image = cache.get(color);
    }

    public void draw(Graphics g, int scale) {
        int drawX = (int) Math.round(x) * scale;
        int drawY = (int) Math.round(y - LIP_OFFSET) * scale;
        
        if (image != null) {
            g.drawImage(image, drawX, drawY, TILE_SIZE * scale, VISUAL_HEIGHT * scale, null);
        }
    }

    public Rectangle getHitbox() {
        return new Rectangle((int)Math.round(x), (int)Math.round(y), TILE_SIZE, TILE_SIZE);
    }
}