import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;

public class GamePanel extends JPanel implements ActionListener {
    private final int GRID_W = 14;
    private final int GRID_H = 23;
    private final int TILE = 13;
    private int scale;

    private Shape activeShape;
    private ArrayList<Brick> settledBricks = new ArrayList<>();
    private ArrayList<String> typeBag = new ArrayList<>();
    private Timer timer;

    private double fallSpeed = 1.0;
    private double fastFallSpeed = 5.0; 
    private boolean isFastFalling = false;

    public GamePanel(int scale) {
        this.scale = scale;
        this.setPreferredSize(new Dimension(TILE * GRID_W * scale, TILE * GRID_H * scale));
        this.setBackground(new Color(20, 20, 20));
        
        spawnNewShape();
        
        timer = new Timer(1000 / 60, this);
        timer.start();

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) tryMove(-TILE, 0);
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) tryMove(TILE, 0);
                if (e.getKeyCode() == KeyEvent.VK_DOWN) isFastFalling = true;
                if (e.getKeyCode() == KeyEvent.VK_E) tryRotate(true);
                if (e.getKeyCode() == KeyEvent.VK_Q) tryRotate(false);
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) isFastFalling = false;
            }
        });
    }

    private void spawnNewShape() {
        if (typeBag.isEmpty()) {
            String[] types = {"I", "O", "T", "L", "J"};
            for (String t : types) typeBag.add(t);
            Collections.shuffle(typeBag);
        }
        activeShape = new Shape(typeBag.remove(0), TILE * (GRID_W / 2), -TILE * 2);
    }

    private void tryMove(int dx, int dy) {
        activeShape.move(dx, dy);
        if (isColliding()) activeShape.move(-dx, -dy);
    }

    private void tryRotate(boolean clockwise) {
        activeShape.rotate(clockwise);
        if (isColliding()) {
            activeShape.move(TILE, 0);
            if (isColliding()) {
                activeShape.move(-TILE * 2, 0);
                if (isColliding()) {
                    activeShape.move(TILE, 0);
                    activeShape.rotate(!clockwise);
                }
            }
        }
    }

    private boolean isColliding() {
        for (Brick b : activeShape.bricks) {
            if (b.x < 0 || b.x >= TILE * GRID_W || b.y >= TILE * GRID_H) return true;
            for (Brick s : settledBricks) {
                if (b.getHitbox().intersects(s.getHitbox())) return true;
            }
        }
        return false;
    }

    private boolean checkBottomCollision() {
        for (Brick b : activeShape.bricks) {
            if (b.y + TILE >= TILE * GRID_H) return true;
            Rectangle nextPos = new Rectangle((int)Math.round(b.x), (int)Math.round(b.y) + 1, TILE, TILE);
            for (Brick s : settledBricks) {
                if (nextPos.intersects(s.getHitbox())) return true;
            }
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        double currentMoveSpeed = isFastFalling ? fastFallSpeed : fallSpeed;
        for (int i = 0; i < (int)currentMoveSpeed; i++) {
            if (!checkBottomCollision()) {
                activeShape.move(0, 1);
            } else {
                for (Brick b : activeShape.bricks) {
                    b.y = Math.round(b.y / (double)TILE) * TILE;
                    b.x = Math.round(b.x / (double)TILE) * TILE;
                    settledBricks.add(b);
                }
                checkFullRows();
                spawnNewShape();
                break;
            }
        }
        repaint();
    }

    private void checkFullRows() {
        for (int rowY = 0; rowY < TILE * GRID_H; rowY += TILE) {
            int count = 0;
            for (Brick b : settledBricks) {
                if ((int)Math.round(b.y) == rowY) count++;
            }
            if (count >= GRID_W) {
                final int targetY = rowY;
                settledBricks.removeIf(b -> (int)Math.round(b.y) == targetY);
                for (Brick b : settledBricks) {
                    if (b.y < targetY) b.y += TILE;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ArrayList<Brick> allVisibleBricks = new ArrayList<>(settledBricks);
        if (activeShape != null) allVisibleBricks.addAll(activeShape.bricks);

        allVisibleBricks.sort((b1, b2) -> Double.compare(b2.y, b1.y));

        for (Brick b : allVisibleBricks) {
            b.draw(g, scale);
        }
    }
}