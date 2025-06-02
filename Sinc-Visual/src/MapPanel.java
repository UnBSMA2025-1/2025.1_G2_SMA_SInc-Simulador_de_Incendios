import jade.wrapper.AgentController;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class MapPanel extends JFrame {
    private Map map;
    private int tileSize = 30;
    private JButton startButton, resetButton, globalWindButton, localWindButton, stopButton;
    private MapPanelInner mapPanelInner;
    private final Random rnd = new Random();
    private Thread simulationThread;
    public static boolean activeSeedsOnGoing = false;


    public MapPanel(Map map) {
        this.map = map;
        map.gui = this;
        setTitle("Simulação de Incêndio Florestal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(map.WIDTH * tileSize, map.HEIGHT * tileSize + 160);
        setLayout(new BorderLayout());

        mapPanelInner = new MapPanelInner();
        add(mapPanelInner, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel();
        startButton = new JButton("Iniciar Simulação");
        startButton.addActionListener(e -> startSimulation());
        buttonsPanel.add(startButton);
        if(map.firstMap != 0){
            startButton.setEnabled(false);}


        resetButton = new JButton("Gerar Mapa");
        resetButton.addActionListener(e -> generateMap());
        buttonsPanel.add(resetButton);

        localWindButton = new JButton("Vento Local");
        localWindButton.addActionListener(e -> setLocalWind());
        buttonsPanel.add(localWindButton);
        if(map.firstMap != 0){localWindButton.setEnabled(false);}


        globalWindButton = new JButton("Vento Global");
        globalWindButton.addActionListener(e -> setGlobalWind());
        if(map.firstMap != 0){globalWindButton.setEnabled(false);}

        buttonsPanel.add(globalWindButton);

        stopButton = new JButton("Parar Simulação");
        stopButton.addActionListener(e -> stopSimulation());
        stopButton.setEnabled(false);
        buttonsPanel.add(stopButton);

        add(buttonsPanel, BorderLayout.SOUTH);

        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new FlowLayout());
        legendPanel.add(new JLabel("0: Sem Vegetação"));
        legendPanel.add(new JLabel("1: Vegetação Seca"));
        legendPanel.add(new JLabel("2: Vegetação Úmida"));
        legendPanel.add(new JLabel("3: Vegetação Comum"));
        legendPanel.add(new JLabel("4: Em Chamas"));
        legendPanel.add(new JLabel("5: Queimado"));
        legendPanel.add(new JLabel("6: Água"));
        add(legendPanel, BorderLayout.NORTH);

        setVisible(true);
    }

    public static void setActiveSeed(boolean i) {
        activeSeedsOnGoing = i;
    }

    private void startSimulation() {
        startButton.setEnabled(false);
        resetButton.setEnabled(false);
        globalWindButton.setEnabled(false);
        localWindButton.setEnabled(false);
        stopButton.setEnabled(true);

        simulationThread = new Thread(() -> {
            int x, y;
            int type;
            do {
                x = rnd.nextInt(map.WIDTH);
                y = rnd.nextInt(map.HEIGHT);
                Tile tile = map.getTile(x, y);
                type = tile.getType();
            } while (type == 0 || type == 4 || type == 5 || type == 6);

            map.createFireAgent(x, y);

            while (map.hasFire() && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            SwingUtilities.invokeLater(() -> {
                repaint();
                JOptionPane.showMessageDialog(this, map.generateReport(),
                        "Relatório", JOptionPane.INFORMATION_MESSAGE);
                startButton.setEnabled(true);
                resetButton.setEnabled(true);
                globalWindButton.setEnabled(true);
                localWindButton.setEnabled(true);
                stopButton.setEnabled(false);
            });
        });
        simulationThread.start();
    }

    private void stopSimulation() {
        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
        }
        map.stopAllFireAgents();
        startButton.setEnabled(true);
        resetButton.setEnabled(true);
        globalWindButton.setEnabled(true);
        localWindButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void generateMap() {
        startButton.setEnabled(false);
        resetButton.setEnabled(false);
        localWindButton.setEnabled(false);
        globalWindButton.setEnabled(false);
        stopButton.setEnabled(false);

        new Thread(() -> {
            map.stopAllSeedStatusAgents();
            activeSeedsOnGoing = true;
            map.reset();
            map.createSeedStatusAgent();
            // aguarda todos os SeedAgents terminarem
            while (activeSeedsOnGoing) {
                try {
                    Thread.sleep(500); // espera para não travar a CPU
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            map.stopAllSeedStatusAgents();
            SwingUtilities.invokeLater(() -> {
                map.terrainData();
                repaint();
                startButton.setEnabled(true);
                resetButton.setEnabled(true);
                localWindButton.setEnabled(true);
                globalWindButton.setEnabled(true);
            });
        }).start();
    }


    private void resetMap() {
        stopSimulation();
        map.reset();
        repaint();
    }

    private void setLocalWind() {
        map.windMode = WindMode.LOCAL;
        map.reset();
        JOptionPane.showMessageDialog(this, "Modo de Vento Local ativado.\nCada tile tem seu próprio vento.", "Vento Local", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setGlobalWind() {
        Direction[] directions = Direction.values();
        String[] options = new String[directions.length];
        for (int i = 0; i < directions.length; i++) options[i] = directions[i].name();

        String chosenDirection = (String) JOptionPane.showInputDialog(
                this, "Escolha a direção do vento global:",
                "Direção do Vento", JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (chosenDirection == null) return;

        String velocityStr = JOptionPane.showInputDialog(this, "Velocidade do vento (m/s):", "1.0");
        if (velocityStr == null) return;
        double velocity;
        try {
            velocity = Double.parseDouble(velocityStr);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Valor inválido!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Direction direction = Direction.valueOf(chosenDirection);
        for (int i = 0; i < map.WIDTH; i++)
            for (int j = 0; j < map.HEIGHT; j++) {
                map.map[i][j].setWindDirection(direction);
                map.map[i][j].setWindVelocity(velocity);
            }
        map.windMode = WindMode.GLOBAL;
        JOptionPane.showMessageDialog(this, "Modo de Vento Global ativado.", "Vento Global", JOptionPane.INFORMATION_MESSAGE);
        repaint();
    }

    public void repaint() {
        if (mapPanelInner != null) {
            mapPanelInner.repaint();
        }
        super.repaint();
    }

    class MapPanelInner extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int i = 0; i < map.WIDTH; i++) {
                for (int j = 0; j < map.HEIGHT; j++) {
                    Tile tile = map.map[i][j];
                    g.setColor(getColorForType(tile));
                    g.fillRect(i * tileSize, j * tileSize, tileSize, tileSize);

                    g.setColor(Color.BLACK);
                    g.drawRect(i * tileSize, j * tileSize, tileSize, tileSize);

                    // Adiciona o número do tile
                    g.setColor(Color.BLACK); // Cor do texto
                    String text = String.valueOf(tile.getType());
                    FontMetrics fm = g.getFontMetrics();
                    int x = i * tileSize + (tileSize - fm.stringWidth(text)) / 2;
                    int y = j * tileSize + (tileSize - fm.getHeight()) / 2 + fm.getAscent();
                    g.drawString(text, x, y);

                    if (tile.getType() != 0 && tile.getType() != 5 && tile.getType() != 6) {
                        drawWindDirection(g, i, j, tile.getWindDirection());
                    }
                }
            }
        }

        private void drawWindDirection(Graphics g, int i, int j, Direction dir) {
            int startLineX = i * tileSize + tileSize / 4;
            int startLineY = j * tileSize + tileSize / 4;
            int arrowSize = tileSize / 6;

            g.setColor(Color.ORANGE);
            int endX = startLineX + dir.dx * arrowSize;
            int endY = startLineY + dir.dy * arrowSize;
            g.drawLine(startLineX, startLineY, endX, endY);

            int arrSize = tileSize / 12;
            int perpX = -dir.dy, perpY = dir.dx;
            int arr1X = endX + (perpX - dir.dx) * arrSize;
            int arr1Y = endY + (perpY - dir.dy) * arrSize;
            int arr2X = endX + (-perpX - dir.dx) * arrSize;
            int arr2Y = endY + (-perpY - dir.dy) * arrSize;
            g.drawLine(endX, endY, arr1X, arr1Y);
            g.drawLine(endX, endY, arr2X, arr2Y);
        }

        private Color getColorForType(Tile tile) {
            switch (tile.getType()) {
                case 0: return new Color(85, 58, 46); // Sem Vegetação (marrom)
                case 1: return new Color(200, 255, 0); // Vegetação Seca (verde-amarelado)
                case 2: return new Color(0, 100, 0); // Vegetação Úmida (verde escuro)
                case 3: return new Color(0, 255, 0); // Vegetação Comum (verde)
                case 4:
                    return getColorFireIntensity(tile); // Em Chamas (vermelho)
                case 5: return new Color(170, 170, 170); // Queimado (cinza)
                case 6: return new Color(30, 144, 255); // Água (azul)
                default: return Color.BLACK;
            }
        }

        private static Color getColorFireIntensity(Tile tile) {
            switch (tile.getFireIntensity()) {
                case 1:
                    return new Color(182, 149, 0);
                case 2:
                    return new Color(255, 100, 0);
                case 3:
                    return new Color(255, 0, 0);
                default:
                    return Color.RED;
            }
        }

    }
}