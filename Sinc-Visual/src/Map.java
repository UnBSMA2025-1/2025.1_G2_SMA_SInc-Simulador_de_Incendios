import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import jade.lang.acl.ACLMessage;
import jade.core.Agent;
import jade.core.AgentContainer;
import jade.lang.acl.ACLMessage;


public class Map {
    public final int HEIGHT, WIDTH;
    public static float AMBIENT_TEMPERATURE = 25;
    public final Tile[][] map;
    public MapPanel gui;
    private final Random random = new Random();
    public ContainerController container;
    public WindMode windMode = WindMode.LOCAL;

    private final Set<String> activeFireAgents = new HashSet<>();
    public final Set<String> activeSeedAgents = new HashSet<>();
    public final Set<String> activeSeedStatusAgents = new HashSet<>();

    public int firstMap = 0;


    private AgentController windAgentController;
    private String windAgentName;

    public Map(int width, int height, ContainerController container) {
        this.WIDTH = width;
        this.HEIGHT = height;
        this.container = container;
        this.map = new Tile[WIDTH][HEIGHT];

        boolean success = this.loadFromImage(
                "./Sinc-Visual/src/public/forest_image3.png",
                "./Sinc-Visual/src/image_to_map_converter.py"
        );

        if (success) {
            System.out.println("Map loaded from image successfully!");
            System.out.println(this.generateImageReport());
            createWindAgent();

            return;
        } else {
            System.out.println("Using random generated map instead");
        }

        createWindAgent();
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
            int numberOfWaterSeeds = WIDTH*HEIGHT/100;
            if (numberOfWaterSeeds == 0){
                numberOfWaterSeeds = random.nextInt(0, 4);
            }
            int k = 0;
            for (int i = 0; i < numberOfSeeds; i++) {
                int x, y;
                k++;
                do {
                    x = random.nextInt(WIDTH);
                    y = random.nextInt(HEIGHT);
                } while (map[x][y].getType() != 0);
                createSeedAgent(x, y); // gera vegetação tipo 1 a 3

                if(k <= numberOfWaterSeeds){
                    do {
                        x = random.nextInt(WIDTH);
                        y = random.nextInt(HEIGHT);
                    } while (map[x][y].getType() != 0);
                    createSeedAgent(x, y, 6); // gera água

                }

            }}

        firstMap = 111;

    }

    private void createWindAgent() {
        try {
            windAgentName = "WindAgent_" + UUID.randomUUID();
            windAgentController = container.createNewAgent(windAgentName, "WindAgent", new Object[]{this});
            windAgentController.start();
            System.out.println("WindAgent created: " + windAgentName);
        } catch (Exception e) {
            System.err.println("Failed to create WindAgent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setWindMode(WindMode mode) {
        this.windMode = mode;
        System.out.println("Wind mode changed to: " + mode);

        if (mode == WindMode.LOCAL) {
            // Reset all tiles to local wind patterns
            resetToLocalWind();
        }
        // Global wind will be applied automatically by WindAgent when mode is GLOBAL
    }

    private void resetToLocalWind() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Tile tile = map[x][y];
                if (tile != null) {
                    // Regenerate local wind properties
                    double windVelocity = 0.5 + 2.5 * random.nextDouble();
                    Direction windDirection = Direction.values()[random.nextInt(Direction.values().length)];
                    tile.setWindVelocity(windVelocity);
                    tile.setWindDirection(windDirection);
                }
            }
        }
        if (gui != null) {
            gui.repaint();
        }
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

    public void createSeedStatusAgent(){
        try {
            String agentName = "SeedStatusAgent_" + UUID.randomUUID();
            container.createNewAgent(agentName, "SeedStatusAgent", null).start();
            activeSeedStatusAgents.add(agentName);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void stopAllSeedStatusAgents() {
        Set<String> agentsCopy = new HashSet<>(activeSeedStatusAgents);
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
        activeSeedStatusAgents.clear();
    }
        public void stopWindAgent() {
            if (windAgentController != null) {
                try {
                    windAgentController.kill();
                    System.out.println("WindAgent stopped: " + windAgentName);
                } catch (Exception e) {
                    System.err.println("Failed to stop WindAgent: " + e.getMessage());
                    e.printStackTrace();
                }
            }
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

            stopWindAgent(); // Para o agente de vento
            initialize();
            createWindAgent(); // Recria o agente de vento
            if (gui != null) gui.repaint();
        }

        // Method to get wind agent name for external communication
        public String getWindAgentName() {
            return windAgentName;
        }


        /**
         * Load map from image using Python ML script
         */
        public boolean loadFromImage(String imagePath, String pythonScriptPath) {
            return loadFromImage(imagePath, pythonScriptPath, "color");
        }

        /**
         * Load map from image with specified method
         */
        public boolean loadFromImage(String imagePath, String pythonScriptPath, String method) {
            try {
                System.out.println("Converting image to map: " + imagePath);

                // Prepare temporary output files
                String jsonOutput = "temp_map_" + System.currentTimeMillis() + ".json";
                String javaOutput = "temp_loader_" + System.currentTimeMillis() + ".java";

                // Build Python command
                ProcessBuilder pb = new ProcessBuilder(
                        "python", pythonScriptPath, imagePath,
                        "--width", String.valueOf(WIDTH),
                        "--height", String.valueOf(HEIGHT),
                        "--method", method,
                        "--output", jsonOutput,
                        "--java-output", javaOutput
                );

                pb.redirectErrorStream(true);
                Process process = pb.start();

                // Read Python output
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                System.out.println("Python ML Conversion Output:");
                while ((line = reader.readLine()) != null) {
                    System.out.println(" ML ->  " + line);
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    System.out.println("Image conversion completed successfully!");

                    // Load the generated map data
                    boolean loadSuccess = loadFromGeneratedJson(jsonOutput);

                    // Clean up temporary files
                    new File(jsonOutput).delete();
                    new File(javaOutput).delete();

                    if (loadSuccess) {
                        System.out.println("Map loaded from image successfully!");
                        System.out.println(generateImageReport());
                        return true;
                    } else {
                        System.err.println("Failed to load generated map data");
                        return false;
                    }
                } else {
                    System.err.println("Python script failed with exit code: " + exitCode);
                    return false;
                }

            } catch (Exception e) {
                System.err.println("Error converting image to map: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        /**
         * Load map data from generated JSON file
         */
        private boolean loadFromGeneratedJson(String jsonFilePath) {
            try {
                // Stop any existing fire agents
                stopAllFireAgents();

                // Read JSON file
                BufferedReader reader = new BufferedReader(new FileReader(jsonFilePath));
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
                reader.close();

                // Parse JSON using Gson
                Gson gson = new Gson();
                Type listType = new TypeToken<List<List<TileData>>>(){}.getType();
                List<List<TileData>> mapData = gson.fromJson(jsonContent.toString(), listType);

                // Validate dimensions
                if (mapData.size() != HEIGHT || mapData.get(0).size() != WIDTH) {
                    System.err.println("Map dimensions mismatch! Expected: " + WIDTH + "x" + HEIGHT +
                            ", Got: " + mapData.get(0).size() + "x" + mapData.size());
                    return false;
                }

                // Load tile data
                for (int y = 0; y < HEIGHT; y++) {
                    for (int x = 0; x < WIDTH; x++) {
                        TileData tileData = mapData.get(y).get(x);

                        // Convert wind direction index to enum (with safety check)
                        Direction windDir = Direction.values()[tileData.windDirection % Direction.values().length];

                        // Create new tile with generated data
                        map[x][y] = new Tile(x, y, tileData.type,
                                tileData.humidity, tileData.fuel,
                                tileData.windVelocity, windDir);
                    }
                }

                // Repaint GUI if available
                if (gui != null) {
                    gui.repaint();
                }

                return true;

            } catch (Exception e) {
                System.err.println("Error loading JSON map data: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        /**
         * Generate detailed report for image-generated maps
         */
        public String generateImageReport() {
            int[] typeCounts = new int[7]; // 0-6 tile types
            double totalHumidity = 0, totalFuel = 0, totalWind = 0;
            int totalTiles = WIDTH * HEIGHT;

            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    Tile tile = map[x][y];
                    typeCounts[tile.getType()]++;
                    totalHumidity += tile.getHumidity();
                    totalFuel += tile.getFuel();
                    totalWind += tile.getWindVelocity();
                }
            }

            StringBuilder report = new StringBuilder();
            report.append("=== Relatório do Mapa Gerado por Imagem ===\n");
            report.append("Dimensões do Mapa: ").append(WIDTH).append("x").append(HEIGHT).append("\n");
            report.append("Total de Tiles: ").append(totalTiles).append("\n\n");

            report.append("Distribuição dos Tipos de Terreno:\n");
            String[] typeNames = {"Sem Vegetação", "Vegetação Seca", "Vegetação Úmida",
                    "Vegetação Comum", "Em Chamas", "Queimado", "Água"};

            for (int i = 0; i < typeCounts.length; i++) {
                if (typeCounts[i] > 0) {
                    double percentage = (double) typeCounts[i] / totalTiles * 100;
                    report.append(String.format("  %s: %d tiles (%.1f%%)\n",
                            typeNames[i], typeCounts[i], percentage));
                }
            }

            report.append("\nMédias Ambientais:\n");
            report.append(String.format("  Umidade Média: %.3f\n", totalHumidity / totalTiles));
            report.append(String.format("  Combustível Médio: %.3f\n", totalFuel / totalTiles));
            report.append(String.format("  Velocidade do Vento Média: %.3f\n", totalWind / totalTiles));

            return report.toString();
        }

        /**
         * Reset map and optionally reload from image
         */
        public void reset(String imagePath, String pythonScriptPath) {
            stopAllFireAgents();

            if (imagePath != null && pythonScriptPath != null) {
                // Try to reload from image
                if (!loadFromImage(imagePath, pythonScriptPath)) {
                    System.out.println("Failed to reload from image, using random generation");
                    initialize(); // Fallback to random
                }
            } else {
                initialize(); // Normal random reset
            }

            if (gui != null) gui.repaint();
        }

        /**
         * Data class for JSON parsing with Gson
         */
        private static class TileData {
            public int x, y, type, windDirection;
            public double humidity, fuel, windVelocity;

            // Default constructor for Gson
            public TileData() {}
        }

    }
