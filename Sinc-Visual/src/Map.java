import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Map {
    public final int HEIGHT, WIDTH;
    public static float AMBIENT_TEMPERATURE = 25;
    public final Tile[][] map;
    public MapPanel gui;
    private final Random random = new Random();
    public ContainerController container;
    public WindMode windMode = WindMode.LOCAL;

    private final Set<String> activeFireAgents = new HashSet<>();

    public Map(int width, int height, ContainerController container) {
        this.WIDTH = width;
        this.HEIGHT = height;
        this.container = container;
        this.map = new Tile[WIDTH][HEIGHT];
        initialize();
    }

    public void initialize() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                double humidity = 0.1 + 0.7 * random.nextDouble();
                double fuel = 0.5 + 2.0 * random.nextDouble();
                double windVelocity = 0.5 + 2.5 * random.nextDouble();
                Direction windDirection = Direction.values()[random.nextInt(Direction.values().length)];

                int type;
                double r = random.nextDouble();
                if (r < 0.10) type = 6; // água
                else if (r < 0.20) type = 0; // sem vegetação
                else type = 1 + random.nextInt(3); // 1, 2 ou 3 tipos de vegetação

                map[x][y] = new Tile(x, y, type, humidity, fuel, windVelocity, windDirection);
            }
        }
    }

    public Tile getTile(int x, int y) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
            return map[x][y];
        }
        return null;
    }

    public MapPanel getGui() {
        return gui;
    }

    public void createFireAgent(int x, int y) {
        Tile tile = map[x][y];
        int type = tile.getType();
        if (type == 0 || type == 4 || type == 5 || type == 6) return; // ignora sem vegetação, em chamas, queimado, água
        tile.setType(4); // define como em chamas
        try {
            String agentName = "FireAgent_" + x + "_" + y + "_" + UUID.randomUUID();
            container.createNewAgent(agentName, "FireAgent", new Object[]{x, y, this}).start();
            activeFireAgents.add(agentName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeFireAgent(String agentName) {
        activeFireAgents.remove(agentName);
    }

    public void stopAllFireAgents() {
        // Itera sobre uma cópia para evitar ConcurrentModificationException
        Set<String> agentsCopy = new HashSet<>(activeFireAgents);
        for (String agentName : agentsCopy) {
            try {
                AgentController ac = container.getAgent(agentName);
                if (ac != null) {
                    ac.kill();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        activeFireAgents.clear();
    }

    public boolean hasFire() {
        for (int x = 0; x < WIDTH; x++)
            for (int y = 0; y < HEIGHT; y++)
                if (map[x][y].getType() == 4) return true;
        return false;
    }

    public String generateReport() {
        int burnt = 0, preserved = 0, dry = 0, wet = 0, common = 0, noVeg = 0;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int t = map[x][y].getType();
                if (t == 5) burnt++;
                else if (t == 0) noVeg++;
                else {
                    preserved++;
                    if (t == 1) dry++;
                    else if (t == 2) wet++;
                    else if (t == 3) common++;
                }
            }
        }
        return "Relatório Final\n" +
                "----------------------------\n" +
                "Tiles Queimados          : " + burnt + "\n" +
                "Tiles Preservados        : " + preserved + "\n" +
                "  - Vegetação Seca       : " + dry + "\n" +
                "  - Vegetação Úmida      : " + wet + "\n" +
                "  - Vegetação Comum      : " + common + "\n" +
                "Tiles Sem Vegetação      : " + noVeg;
    }

    public void reset() {
        stopAllFireAgents(); // Para todos os agentes antes de reiniciar
        initialize();
        if (gui != null) gui.repaint();
    }
}