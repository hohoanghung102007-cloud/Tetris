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

    private final int OFFSET_X = 20;
    private final int OFFSET_Y = 20;

    private Shape activeShape;
    private Shape nextShape; 
    private ArrayList<Brick> settledBricks = new ArrayList<>();
    private ArrayList<String> typeBag = new ArrayList<>();
    private Timer timer;

    private double fallSpeed = 1.0;
    private double fastFallSpeed = 5.0; 
    private boolean isFastFalling = false;

    private int score = 0;
    private boolean isGameOver = false;

    public GamePanel(int scale) {
        this.scale = scale;
        int panelW = (TILE * GRID_W + 120) * scale;
        int panelH = (TILE * GRID_H + 40) * scale;
        
        this.setPreferredSize(new Dimension(panelW, panelH));
        this.setBackground(new Color(240, 240, 240)); 
        
        spawnNewShape();
        
        timer = new Timer(1000 / 60, this);
        timer.start();

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isGameOver) return;
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

        if (nextShape == null) {
            nextShape = new Shape(typeBag.remove(0), 0, 0);
        }
        
        activeShape = nextShape;
        for(Brick b : activeShape.bricks) {
            b.x += TILE * (GRID_W / 2);
        }

        if (typeBag.isEmpty()) {
            String[] types = {"I", "O", "T", "L", "J"};
            for (String t : types) typeBag.add(t);
            Collections.shuffle(typeBag);
        }
        nextShape = new Shape(typeBag.remove(0), 0, 0);

        if (isColliding()) {
            isGameOver = true;
            timer.stop();
        }
    }

    private void tryMove(int dx, int dy) {
        if (isGameOver) return;
        activeShape.move(dx, dy);
        if (isColliding()) activeShape.move(-dx, -dy);
    }

    private void tryRotate(boolean clockwise) {
        if (isGameOver) return;
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
        if (isGameOver) return;
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
        int linesCleared = 0;
        for (int rowY = 0; rowY < TILE * GRID_H; rowY += TILE) {
            int count = 0;
            for (Brick b : settledBricks) {
                if ((int)Math.round(b.y) == rowY) count++;
            }
            if (count >= GRID_W) {
                linesCleared++;
                final int targetY = rowY;
                settledBricks.removeIf(b -> (int)Math.round(b.y) == targetY);
                for (Brick b : settledBricks) {
                    if (b.y < targetY) b.y += TILE;
                }
            }
        }
        if (linesCleared == 1) score += 100;
        else if (linesCleared == 2) score += 300;
        else if (linesCleared == 3) score += 500;
        else if (linesCleared >= 4) score += 1200;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int boardW = TILE * GRID_W * scale;
        int boardH = TILE * GRID_H * scale;
        g2.setColor(Color.WHITE);
        g2.fillRect(OFFSET_X * scale, OFFSET_Y * scale, boardW, boardH);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(OFFSET_X * scale, OFFSET_Y * scale, boardW, boardH);

        g2.translate(OFFSET_X * scale, OFFSET_Y * scale);
        ArrayList<Brick> allBricks = new ArrayList<>(settledBricks);
        if (activeShape != null) allBricks.addAll(activeShape.bricks);
        for (Brick b : allBricks) b.draw(g2, scale);
        g2.translate(-OFFSET_X * scale, -OFFSET_Y * scale);

        int scoreBoxX = (OFFSET_X + TILE * GRID_W + 20) * scale;
        int scoreBoxY = OFFSET_Y * scale;
        int scoreBoxW = 80 * scale;
        int scoreBoxH = 40 * scale;

        g2.setColor(Color.WHITE);
        g2.fillRect(scoreBoxX, scoreBoxY, scoreBoxW, scoreBoxH);
        g2.setColor(Color.BLACK);
        g2.drawRect(scoreBoxX, scoreBoxY, scoreBoxW, scoreBoxH);
        
        g2.setFont(new Font("Monospaced", Font.BOLD, 12 * scale));
        g2.drawString(String.format("%06d", score), scoreBoxX + 10 * scale, scoreBoxY + 25 * scale);

        int nextBoxX = scoreBoxX + 10 * scale;
        int nextBoxY = scoreBoxY + 100 * scale;
        int nextBoxSize = 60 * scale;

        g2.setColor(Color.WHITE);
        g2.fillRect(nextBoxX, nextBoxY, nextBoxSize, nextBoxSize);
        g2.setColor(Color.BLACK);
        g2.drawRect(nextBoxX, nextBoxY, nextBoxSize, nextBoxSize);

        if (nextShape != null) {
            g2.translate(nextBoxX + 15 * scale, nextBoxY + 15 * scale);
            for (Brick b : nextShape.bricks) {
                // ĐÃ SỬA LỖI Ở ĐÂY: Dùng double thay vì int
                double originalX = b.x; 
                double originalY = b.y;
                
                b.x = (b.x / TILE) * (TILE - 2); 
                b.y = (b.y / TILE) * (TILE - 2);
                b.draw(g2, scale);
                
                b.x = originalX; 
                b.y = originalY; 
            }
            g2.translate(-(nextBoxX + 15 * scale), -(nextBoxY + 15 * scale));
        }

        if (isGameOver) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 20 * scale));
            g2.drawString("GAME OVER", (OFFSET_X + 20) * scale, boardH / 2 + OFFSET_Y * scale);
        }
    }
}