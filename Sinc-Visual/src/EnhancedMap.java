import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jade.wrapper.ContainerController;

import java.io.*;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Random;

public class EnhancedMap extends Map {

    public EnhancedMap(int width, int height, ContainerController container) {
        super(width, height, container);
    }

    public boolean loadFromGeneratedData(String jsonFilePath) {
        try {
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

                    // Convert wind direction index to enum
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

            System.out.println("Map loaded successfully from: " + jsonFilePath);
            return true;

        } catch (Exception e) {
            System.err.println("Error loading map from JSON: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean convertImageToMap(String imagePath, String pythonScriptPath) {
        return convertImageToMap(imagePath, pythonScriptPath, WIDTH, HEIGHT, "color");
    }

    public boolean convertImageToMap(String imagePath, String pythonScriptPath,
                                     int mapWidth, int mapHeight, String method) {
        try {
            // Prepare output files
            String jsonOutput = "temp_generated_map.json";
            String javaOutput = "temp_generated_loader.java";

            // Build Python command
            ProcessBuilder pb = new ProcessBuilder(
                    "python", pythonScriptPath, imagePath,
                    "--width", String.valueOf(mapWidth),
                    "--height", String.valueOf(mapHeight),
                    "--method", method,
                    "--output", jsonOutput,
                    "--java-output", javaOutput,
                    "--visualize"
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Python: " + line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Image conversion completed successfully!");

                // Load the generated map data
                boolean loadSuccess = loadFromGeneratedData(jsonOutput);

                // Clean up temporary files
                new File(jsonOutput).delete();
                new File(javaOutput).delete();

                return loadSuccess;
            } else {
                System.err.println("Python script failed with exit code: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error executing Python script: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String generateImageMapReport() {
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
        report.append("=== Image-Generated Map Report ===\n");
        report.append("Map Dimensions: ").append(WIDTH).append("x").append(HEIGHT).append("\n");
        report.append("Total Tiles: ").append(totalTiles).append("\n\n");

        report.append("Tile Distribution:\n");
        String[] typeNames = {"No Vegetation", "Dry Vegetation", "Wet Vegetation",
                "Common Vegetation", "On Fire", "Burnt", "Water"};

        for (int i = 0; i < typeCounts.length; i++) {
            if (typeCounts[i] > 0) {
                double percentage = (double) typeCounts[i] / totalTiles * 100;
                report.append(String.format("  %s: %d tiles (%.1f%%)\n",
                        typeNames[i], typeCounts[i], percentage));
            }
        }

        report.append("\nEnvironmental Averages:\n");
        report.append(String.format("  Average Humidity: %.3f\n", totalHumidity / totalTiles));
        report.append(String.format("  Average Fuel: %.3f\n", totalFuel / totalTiles));
        report.append(String.format("  Average Wind Velocity: %.3f\n", totalWind / totalTiles));

        return report.toString();
    }

    /**
     * Data class for JSON parsing
     */
    private static class TileData {
        public int x, y, type, windDirection;
        public double humidity, fuel, windVelocity;
    }
}
