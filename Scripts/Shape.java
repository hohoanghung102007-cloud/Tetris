import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Shape {
    public List<Brick> bricks = new ArrayList<>();
    private static final String[] COLORS = {"Purple", "Red", "Green", "Orange", "Cyan", "Yellow"};
    private static final Random rand = new Random();
    public String color;

    public Shape(String type, int startX, int startY) {
        this.color = COLORS[rand.nextInt(COLORS.length)];
        int[][] coords = getCoordsForType(type);
        for (int[] pos : coords) {
            bricks.add(new Brick(color, startX + (pos[0] * 13), startY + (pos[1] * 13)));
        }
    }

    private int[][] getCoordsForType(String type) {
        return switch (type) {
            case "I" -> new int[][]{{0,0}, {1,0}, {2,0}, {3,0}};
            case "O" -> new int[][]{{0,0}, {1,0}, {0,1}, {1,1}};
            case "T" -> new int[][]{{1,0}, {0,1}, {1,1}, {2,1}};
            case "L" -> new int[][]{{2,0}, {0,1}, {1,1}, {2,1}};
            case "J" -> new int[][]{{0,0}, {0,1}, {1,1}, {2,1}};
            default  -> new int[][]{{0,0}, {1,0}, {2,0}, {0,1}};
        };
    }

    public void move(double dx, double dy) {
        for (Brick b : bricks) {
            b.x += dx;
            b.y += dy;
        }
    }

    public void rotate(boolean clockwise) {
        double pivotX = bricks.get(1).x; 
        double pivotY = bricks.get(1).y;

        for (Brick b : bricks) {
            double relX = b.x - pivotX;
            double relY = b.y - pivotY;
            if (clockwise) {
                b.x = pivotX - relY;
                b.y = pivotY + relX;
            } else {
                b.x = pivotX + relY;
                b.y = pivotY - relX;
            }
        }
    }
}