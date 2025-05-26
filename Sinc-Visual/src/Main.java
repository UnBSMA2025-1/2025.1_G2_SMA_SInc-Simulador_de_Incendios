import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Map map = new Map(25, 15);

        // Executa a criação da GUI na Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Simulação de Incêndio Florestal");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            MapPanel mapPanel = new MapPanel(map);
            frame.add(mapPanel);

            frame.pack(); // Ajusta o tamanho da janela ao conteúdo
            frame.setLocationRelativeTo(null); // Centraliza a janela na tela
            frame.setVisible(true);
        });
    }
}
