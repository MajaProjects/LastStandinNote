import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;

public class Main {

    public static void main(String[] args) {
        System.out.println("LastStandinNote iniciado");

        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("LastStandinNote");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel panel = new JPanel();
            panel.setBackground(Color.BLACK);
            panel.setPreferredSize(new Dimension(960, 540));

            ventana.setContentPane(panel);
            ventana.setResizable(false);
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}