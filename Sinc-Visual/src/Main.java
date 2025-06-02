import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.core.Runtime;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer container = runtime.createMainContainer(profile);

        Map map = new Map(30, 20, container);

        SwingUtilities.invokeLater(() -> {
            MapPanel mapPanel = new MapPanel(map);
            map.gui = mapPanel;
            mapPanel.setLocationRelativeTo(null);
            mapPanel.setVisible(true);
        });

        try {
            AgentController sniffer = container.createNewAgent("sniffer", "jade.tools.sniffer.Sniffer", new Object[]{});
            sniffer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}