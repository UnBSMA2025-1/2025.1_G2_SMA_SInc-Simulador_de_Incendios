import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class MapPanel extends JPanel {
    private Map map;
    private static final int tileSize = 40; // Tamanho visual de cada Tile em pixels
    private JButton startButton, stopButton, restartButton, localWindButton, globalWindButton;
    private MapTiles mapTiles;

    private final Random rnd = new Random();
    private javax.swing.Timer simulationTimer; // Timer para controlar a simulação

    public MapPanel(Map map) {
        this.map = map;
        setPreferredSize(new Dimension(map.WIDTH * tileSize, map.HEIGHT * tileSize + 140));
        setLayout(new BorderLayout());

        mapTiles = new MapTiles();
        add(mapTiles, BorderLayout.CENTER);

        JPanel botoesPanel = new JPanel();

        startButton = new JButton("Iniciar Simulação");
        startButton.addActionListener(e -> startSimulation());
        botoesPanel.add(startButton);

        stopButton = new JButton("Parar Simulação");
        stopButton.addActionListener(e -> stopSimulation());
        stopButton.setEnabled(false);
        botoesPanel.add(stopButton);

        restartButton = new JButton("Reiniciar Mapa");
        restartButton.addActionListener(e -> restartMap());
        botoesPanel.add(restartButton);

        localWindButton = new JButton("Vento Local");
        localWindButton.addActionListener(e -> setToLocalWind());
        botoesPanel.add(localWindButton);

        globalWindButton = new JButton("Vento Global");
        globalWindButton.addActionListener(e -> setToGlobalWind());
        botoesPanel.add(globalWindButton);

        add(botoesPanel, BorderLayout.SOUTH);

        JPanel legendaPanel = new JPanel();
        legendaPanel.setLayout(new FlowLayout());
        legendaPanel.add(new JLabel("0: Sem Vegetação"));
        legendaPanel.add(new JLabel("1: Mata Seca"));
        legendaPanel.add(new JLabel("2: Mata Molhada"));
        legendaPanel.add(new JLabel("4: Em Chamas"));
        legendaPanel.add(new JLabel("5: Mato Queimado"));
        legendaPanel.add(new JLabel("6: Água"));
        add(legendaPanel, BorderLayout.NORTH);

        setVisible(true);
    }

    public void startSimulation() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        restartButton.setEnabled(false);
        localWindButton.setEnabled(false);
        globalWindButton.setEnabled(false);

        if (simulationTimer == null) {
            simulationTimer = new javax.swing.Timer(200, e -> {
                // Lógica a ser executada a cada "tick" do timer
                boolean tileSuitableFound = false;
                int attempts = 0;
                int targetX = -1, targetY = -1;

                // Tenta encontrar um tile adequado para mudar
                while (!tileSuitableFound && attempts < 5) { // Limitar tentativas para evitar loop infinito
                    targetX = rnd.nextInt(Map.WIDTH);
                    targetY = rnd.nextInt(Map.HEIGHT);
                    Tile currentTile = map.getTile(targetX, targetY);
                    if (currentTile != null &&
                            currentTile.getType() != 0 && // Não Sem Vegetação
                            currentTile.getType() != 5 && // Não Queimado
                            currentTile.getType() != 6) { // Não Água
                        tileSuitableFound = true;
                    }
                    attempts++;
                }

                if (tileSuitableFound) {
                    map.changeRandomTileType(targetX, targetY);
                    map.counter++;
                } else {
                    stopSimulation();
                }
                mapTiles.repaint();
            });
        }
        simulationTimer.start(); // Inicia ou reinicia o timer
    }

    private void stopSimulation() {
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        restartButton.setEnabled(true);
    }

    private void restartMap() {
        stopSimulation();
        map.mapStart();
        map.counter = -1;
        localWindButton.setEnabled(true);
        globalWindButton.setEnabled(true);
        mapTiles.repaint();
    }

    private void setToLocalWind() {
        stopSimulation();
        map.windMode = WindMode.LOCAL;
        map.mapStart();
        JOptionPane.showMessageDialog(
                this, "Modo Vento Local ativado.\nCada tile tem seu próprio vento.",
                "Vento Local", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setToGlobalWind() {
        stopSimulation();
        Direction[] dirs = Direction.values();
        String[] options = new String[dirs.length];
        for (int i = 0; i < dirs.length; i++) options[i] = dirs[i].name();

        String chosenDirection = (String) JOptionPane.showInputDialog(
                this, "Escolha a direção do vento global:",
                "Direção do Vento", JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (chosenDirection == null) return;

        String velStr = JOptionPane.showInputDialog(
                this, "Velocidade do vento (m/s):", "1.0");
        if (velStr == null) return;
        double windVel;
        try {
            windVel = Double.parseDouble(velStr);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Valor inválido!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Direction windDir = Direction.valueOf(chosenDirection);
        for (int i = 0; i < map.WIDTH; i++)
            for (int j = 0; j < map.HEIGHT; j++) {
                map.getTile(i, j).setWindVelocity(windVel);
                map.getTile(i, j).setWindDirection(windDir);
            }
        map.windMode = WindMode.GLOBAL;
        JOptionPane.showMessageDialog(this, "Modo Vento Global ativado.",
                "Vento Global", JOptionPane.INFORMATION_MESSAGE);
        repaint();
    }

    class MapTiles extends JPanel {
        public MapTiles () {
            setPreferredSize(new Dimension(Map.WIDTH * tileSize, Map.HEIGHT * tileSize));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Limpa o painel

            // Desenha cada Tile
            for (int x = 0; x < Map.WIDTH; x++) {
                for (int y = 0; y < Map.HEIGHT; y++) {
                    Tile tile = map.getTile(x, y);
                    if (tile != null) {
                        // Define a cor do Tile baseado no seu tipo
                        g.setColor(getColorForType(tile.getType()));
                        // Desenha o Tile como um retângulo preenchido
                        g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);

                        // Desenha uma borda para cada Tile (TODO: Deixar a borda?)
                        g.setColor(Color.BLACK);
                        g.drawRect(x * tileSize, y * tileSize, tileSize, tileSize);
                    }
                }
            }

            // Exibe o contador
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            String countText = "Update count: " + map.getCounter();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(countText, 5, getHeight() - fm.getDescent() - 5);
        }

        private Color getColorForType(int type) {
            switch (type) {
                case 0:
                    return new Color(85, 58, 46); // Sem Vegetação
                case 1:
                    return new Color(200, 255, 0); // Vegetação Seca
                case 2:
                    return new Color(0, 100, 0); // Vegetação Molhada
                case 3:
                    return new Color(0, 255, 0); // Vegetação Comum
                case 4:
                    return new Color(255, 0, 0); // Vegetação em Chamas
                case 5:
                    return new Color(170, 170, 170); // Vegetação Queimada
                case 6:
                    return new Color(30, 144, 255); // Água
                default:
                    return Color.BLACK;      // Cor padrão para tipos desconhecidos
            }
        }
    }
}
