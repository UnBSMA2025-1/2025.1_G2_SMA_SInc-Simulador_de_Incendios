import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

import java.util.Random;

public class SeedAgent extends Agent {
    private int x, y;
    private Map map;
    private final Random rnd = new Random();
    private int vegetationType;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args == null || args.length < 3) {
            System.err.println("SeedAgent: argumentos insuficientes");
            doDelete();
            return;
        }

        x = (int) args[0];
        y = (int) args[1];
        map = (Map) args[2];

        Tile tile = map.getTile(x, y);
        if (tile == null) {
            System.err.println("SeedAgent: tile inválido");
            doDelete();
            return;
        }

        if (args.length >= 4) {
            vegetationType = (int) args[3];
        } else {
            vegetationType = 1 + rnd.nextInt(3);
        }
        tile.setType(vegetationType);

        // intervalo de 200ms
        addBehaviour(new TickerBehaviour(this, 200) {
            private int ticks = 0;

            @Override
            protected void onTick() {
                Tile t = map.getTile(x, y);
                if (t == null) {
                    doDelete();
                    return;
                }

                for (Direction dir : Direction.values()) {
                    if (rnd.nextDouble() < 0.65) {
                        int nx = x + dir.dx;
                        int ny = y + dir.dy;
                        Tile neighbor = map.getTile(nx, ny);
                        if (neighbor == null) continue;

                        if (neighbor.getType() == 0) {
                            map.createSeedAgent(nx, ny, vegetationType);
                        }
                    }
                }

                if (map.getGui() != null) {
                    map.getGui().repaint();
                }

                ticks++;
                if (ticks >= 3) {  // ciclos p morrer
                    doDelete();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        map.removeSeedAgent(getLocalName());
        System.out.println("Morri");
        super.takeDown();
    }
}
