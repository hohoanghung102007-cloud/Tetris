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

    // --- CÁC BIẾN MỚI THÊM ---
    private int score = 0;
    private boolean isGameOver = false;

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
                if (isGameOver) return; // Nếu game over thì không cho bấm nút
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
        
        // Tạo gạch mới
        activeShape = new Shape(typeBag.remove(0), TILE * (GRID_W / 2), 0);

        // KIỂM TRA GAME OVER: Nếu gạch mới sinh ra đã va chạm ngay lập tức
        if (isColliding()) {
            isGameOver = true;
            timer.stop(); // Dừng vòng lặp game
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
            // Chạm biên trái/phải/dưới
            if (b.x < 0 || b.x >= TILE * GRID_W || b.y >= TILE * GRID_H) return true;
            // Chạm gạch đã cố định
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

        // Quy tắc tính điểm từ ảnh bạn gửi
        if (linesCleared == 1) score += 100;
        else if (linesCleared == 2) score += 300;
        else if (linesCleared == 3) score += 500;
        else if (linesCleared >= 4) score += 1200;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Vẽ gạch
        ArrayList<Brick> allVisibleBricks = new ArrayList<>(settledBricks);
        if (activeShape != null) allVisibleBricks.addAll(activeShape.bricks);
        allVisibleBricks.sort((b1, b2) -> Double.compare(b2.y, b1.y));

        for (Brick b : allVisibleBricks) {
            b.draw(g, scale);
        }

        // --- VẼ ĐIỂM SỐ ---
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 8 * scale));
        g.drawString("SCORE: " + score, 5 * scale, 15 * scale);

        // --- VẼ GIAO DIỆN GAME OVER ---
        if (isGameOver) {
            // Vẽ lớp phủ tối màu
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, getWidth(), getHeight());

            // Vẽ chữ GAME OVER
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 15 * scale));
            String msg = "GAME OVER";
            
            // Tính toán để căn giữa chữ
            FontMetrics metrics = g.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(msg)) / 2;
            int y = getHeight() / 2;
            
            g.drawString(msg, x, y);

            // Vẽ điểm chung cuộc dưới chữ Game Over
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 8 * scale));
            String finalScore = "Final Score: " + score;
            int xScore = (getWidth() - g.getFontMetrics().stringWidth(finalScore)) / 2;
            g.drawString(finalScore, xScore, y + 20 * scale);
        }
    }
}