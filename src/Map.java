import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class Map {
    public final int WIDTH, HEIGHT;
    public final Tile[][] map;
    public MapPanel gui;
    private final Random random = new Random();
    public ContainerController container;
    public WindMode windMode = WindMode.LOCAL;

    private final Set<String> activeFireAgents = new HashSet<>();
    public final Set<String> activeSeedAgents = new HashSet<>();

    public int firstMap = 0;



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
                double humidity = 0;
                double fuel = 0;
                double windVelocity = 0.5 + 2.5 * random.nextDouble();
                Direction windDirection = Direction.values()[random.nextInt(Direction.values().length)];

                double r = random.nextDouble();
                int type = 0;
                map[x][y] = new Tile(x, y, type, humidity, fuel, windVelocity, windDirection);
            }
        }
        if(firstMap != 0){
        int numberOfSeeds = WIDTH*HEIGHT/10;
        for (int i = 0; i < numberOfSeeds; i++) {
            int x, y;
            do {
                x = random.nextInt(WIDTH);
                y = random.nextInt(HEIGHT);
            } while (map[x][y].getType() != 0);

            createSeedAgent(x, y); // gera vegetação tipo 1 a 3
        }
        int numberOfWaterSeeds = WIDTH*HEIGHT/100;
        if (numberOfWaterSeeds == 0){
            numberOfWaterSeeds = random.nextInt(0, 4);
        }
        for (int i = 0; i < numberOfWaterSeeds; i++) {
            int x, y;
            do {
                x = random.nextInt(WIDTH);
                y = random.nextInt(HEIGHT);
            } while (map[x][y].getType() != 0);

            createSeedAgent(x, y, 6); // gera água
        }
        }
        firstMap++;

    }

    public void terrainData() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int type = map[x][y].getType();
                double fuel, humidity;

                switch (type){ //diferenciar os detalhes dos tipos de terrenos aqui
                    case 1:
                        fuel = 0.5 + 2.0 * random.nextDouble();
                        map[x][y].setFuel(fuel);
                        humidity = 0.1 + 0.7 * random.nextDouble();
                        map[x][y].setHumidity(humidity);
                        break;
                    case 2:
                        fuel = 0.5 + 2.0 * random.nextDouble();
                        map[x][y].setFuel(fuel);
                        humidity = 0.1 + 0.7 * random.nextDouble();
                        map[x][y].setHumidity(humidity);
                        break;
                    case 3:
                        fuel = 0.5 + 2.0 * random.nextDouble();
                        map[x][y].setFuel(fuel);
                        humidity = 0.1 + 0.7 * random.nextDouble();
                        map[x][y].setHumidity(humidity);
                        break;
                    case 6:
                        map[x][y].setFuel(0);
                        map[x][y].setHumidity(10);
                        break;
                    default:
                        map[x][y].setFuel(0);
                        humidity = 0.1 + 0.7 * random.nextDouble();
                        break;


                }
                double windVelocity = 0.5 + 2.5 * random.nextDouble();
                Direction windDirection = Direction.values()[random.nextInt(Direction.values().length)];
                map[x][y].setWindVelocity(windVelocity);
                map[x][y].setWindDirection(windDirection);
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
    public void createSeedAgent(int x, int y, int vegetationType) {
        Tile tile = map[x][y];
        if (tile.getType() != 0) return;
        tile.setType(vegetationType); //tipo herdado
        try {
            String agentName = "SeedAgent_" + x + "_" + y + "_" + UUID.randomUUID();
            container.createNewAgent(agentName, "SeedAgent", new Object[]{x, y, this, vegetationType}).start();
            activeSeedAgents.add(agentName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createSeedAgent(int x, int y) {
        int randomType = 1 + random.nextInt(3);
        createSeedAgent(x, y, randomType);
    }


    public void removeSeedAgent(String agentName) {
        activeSeedAgents.remove(agentName);
    }

    public void stopAllSeedAgents() {
        Set<String> agentsCopy = new HashSet<>(activeSeedAgents);
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
        activeSeedAgents.clear();
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
        stopAllSeedAgents();
        initialize();
        if (gui != null) gui.repaint();
    }
}