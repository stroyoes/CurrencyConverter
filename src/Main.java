import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.logging.Level;
import java.util.logging.Logger;
import ui.CurrencyConverterUI;

public class Main {
    public static void main(String[] args) {
        // Set Nimbus Look and Feel
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(CurrencyConverterUI.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        // Launch the UI
        javax.swing.SwingUtilities.invokeLater(() -> {
            new CurrencyConverterUI().setVisible(true);
        });
    }
}
