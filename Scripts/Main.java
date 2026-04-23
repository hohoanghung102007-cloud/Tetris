import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Main {
    public static void main(String[] args) {
        String input = JOptionPane.showInputDialog("Enter Scale (1, 2, 3 or 4):");
        if (input == null) return;
        
        int scale = Integer.parseInt(input);

        JFrame frame = new JFrame("Mini 3D Tetris");
        GamePanel game = new GamePanel(scale);
        
        frame.add(game);
        frame.setResizable(false);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}