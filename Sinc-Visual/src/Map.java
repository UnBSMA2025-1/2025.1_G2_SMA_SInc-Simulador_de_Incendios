import java.util.Random;

public class Map {
    public static int WIDTH;
    public static int HEIGHT;
    public static float AMBIENT_TEMPERATURE = 25;
    private Tile[][] tiles;
    public WindMode windMode = WindMode.LOCAL;
    public int counter = 0;
    private final Random rnd = new Random();

    public Map(int w, int h) {
        WIDTH = w;
        HEIGHT = h;
        this.tiles = new Tile[WIDTH][HEIGHT];
        mapStart();
    }

    public void mapStart() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                /*
                 * Type =>
                 *   0 = Sem Vegetação;
                 *   1 = Vegetação Seca;
                 *   2 = Vegetação Molhada;
                 *   3 = Vegetação Comum;
                 *   4 = Vegetação Em Chamas;
                 *   5 = Vegetação Queimada;
                 *   6 = Água;
                 * */
                // TODO: Type atual deve ser parecido com aqueles dos Tiles ao seu redor;
                double rdm = rnd.nextDouble();
                int type;
                if (rdm < 0.10) { // 10% de chance de ser água
                    type = 6;
                } else if (rdm < 0.20) {
                    type = 0;
                } else {
                    type = 1 + rnd.nextInt(3);
                }

                float temperature = AMBIENT_TEMPERATURE + rnd.nextInt(-2, +2);
                double terrainHeight= 1; // <===== here PEIXOTO
                double humidity = 0.1 + 0.7 * rnd.nextDouble();
                double fuel = 0.5 + 2.0 * rnd.nextDouble();
                double windVel = 0.5 + 2.5 * rnd.nextDouble();
                Direction windDir = Direction.values()[rnd.nextInt(Direction.values().length)];
                tiles[x][y] = new Tile(x, y, type, humidity, fuel, windVel, windDir, temperature, terrainHeight);
            }
        }
    }

    // Metodo de exemplo para alterar dinamicamente um Tile (TODO: Deixar o agente realizar isso)
    public void changeRandomTileType(int x, int y) {
        if (WIDTH > 0 && HEIGHT > 0 && tiles[x][y] != null) {
            if (tiles[x][y].getType() == 4) {
                tiles[x][y].setType(5);
            } else {
                tiles[x][y].setType(4);
            }
        }
    }

    public Tile getTile(int x, int y) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
            return tiles[x][y];
        }
        return null;
    }

    public int getCounter() {
        return counter;
    }
}
