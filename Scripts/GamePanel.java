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
        int panelW = (TILE * GRID_W + 140) * scale;
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

        // --- 1. VẼ BOARD CHÍNH ---
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

        // --- 2. VẼ Ô SCORE ---
        int scoreBoxX = (OFFSET_X + TILE * GRID_W + 20) * scale;
        int scoreBoxY = OFFSET_Y * scale;
        int scoreBoxW = 95 * scale; 
        int scoreBoxH = 50 * scale;

        g2.setColor(Color.WHITE);
        g2.fillRect(scoreBoxX, scoreBoxY, scoreBoxW, scoreBoxH);
        g2.setColor(Color.BLACK);
        g2.drawRect(scoreBoxX, scoreBoxY, scoreBoxW, scoreBoxH);
        
        // Vẽ chữ nhãn "SCORE" căn giữa
        g2.setFont(new Font("Monospaced", Font.BOLD, 10 * scale));
        FontMetrics fm = g2.getFontMetrics();
        String labelScore = "SCORE";
        int labelScoreX = scoreBoxX + (scoreBoxW - fm.stringWidth(labelScore)) / 2;
        g2.drawString(labelScore, labelScoreX, scoreBoxY + 15 * scale);

        // Vẽ con số điểm căn giữa
        g2.setFont(new Font("Monospaced", Font.BOLD, 14 * scale));
        String scoreVal = String.format("%06d", score);
        int valScoreX = scoreBoxX + (scoreBoxW - g2.getFontMetrics().stringWidth(scoreVal)) / 2;
        g2.drawString(scoreVal, valScoreX, scoreBoxY + 35 * scale);

        // --- 3. VẼ Ô NEXT ---
        int nextBoxX = scoreBoxX + 10 * scale;
        int nextBoxY = scoreBoxY + 100 * scale;
        int nextBoxSize = 75 * scale;

        g2.setColor(Color.WHITE);
        g2.fillRect(nextBoxX, nextBoxY, nextBoxSize, nextBoxSize);
        g2.setColor(Color.BLACK);
        g2.drawRect(nextBoxX, nextBoxY, nextBoxSize, nextBoxSize);

        // Vẽ chữ nhãn "NEXT" căn giữa
        g2.setFont(new Font("Monospaced", Font.BOLD, 10 * scale));
        String labelNext = "NEXT";
        int labelNextX = nextBoxX + (nextBoxSize - g2.getFontMetrics().stringWidth(labelNext)) / 2;
        g2.drawString(labelNext, labelNextX, nextBoxY + 15 * scale);

        if (nextShape != null) {
            double minX = 999, maxX = -999, minY = 999, maxY = -999;
            for (Brick b : nextShape.bricks) {
                minX = Math.min(minX, b.x); maxX = Math.max(maxX, b.x);
                minY = Math.min(minY, b.y); maxY = Math.max(maxY, b.y);
            }
            double pW = (maxX - minX + TILE) * scale;
            double pH = (maxY - minY + TILE) * scale;

            // Căn giữa khối gạch vào phần còn lại của ô (phía dưới chữ NEXT)
            double tx = nextBoxX + (nextBoxSize - pW) / 2.0 - (minX * scale);
            double ty = nextBoxY + 20 * scale + (nextBoxSize - 20 * scale - pH) / 2.0 - (minY * scale);

            g2.translate(tx, ty);
            for (Brick b : nextShape.bricks) b.draw(g2, scale);
            g2.translate(-tx, -ty);
        }

        // --- 4. GAME OVER ---
        if (isGameOver) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(OFFSET_X * scale, OFFSET_Y * scale, boardW, boardH);
            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 20 * scale));
            String msg = "GAME OVER";
            FontMetrics msgFm = g2.getFontMetrics();
            int gx = (OFFSET_X * scale) + (boardW - msgFm.stringWidth(msg)) / 2;
            int gy = (OFFSET_Y * scale) + (boardH / 2);
            g2.drawString(msg, gx, gy);
        }
    }
}