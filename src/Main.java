import javax.swing.JFrame;
import javax.swing.SwingUtilities;
public class Main {

    public static void main(String[] args) {
        System.out.println("LastStandinNote iniciado");

        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("LastStandinNote");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            GamePanel panel = new GamePanel();

            ventana.setContentPane(panel);
            ventana.setResizable(false);
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
