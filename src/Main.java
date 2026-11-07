import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
public class Main {

    public static void main(String[] args) {
        System.out.println("LastStandinNote iniciado");

        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("LastStandinNote");
            ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

            GamePanel panel = new GamePanel();
            ventana.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    panel.requestApplicationExit();
                }
            });

            ventana.setContentPane(panel);
            ventana.setResizable(false);
            WindowController display = new WindowController(ventana, panel);
            display.showWindow();
        });
    }
}

