import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

import java.util.Random;

public class FireAgent extends Agent {
    private int x, y;
    private Map map;
    private final Random rnd = new Random();

    /* Burning rates in kg/s */
    private static final double RATE_DRY = 0.5;
    private static final double RATE_DEFAULT = 0.3;
    private static final double RATE_WET = 0.1;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args == null || args.length < 3) {
            System.err.println("FireAgent: insufficient arguments");
            doDelete();
            return;
        }

        x = (int) args[0];
        y = (int) args[1];
        map = (Map) args[2];

        Tile tile = map.getTile(x, y);
        if (tile == null) {
            System.err.println("FireAgent: invalid tile");
            doDelete();
            return;
        }

        tile.setType(4); // On fire
        tile.setFireIntensity(1);

        addBehaviour(new TickerBehaviour(this, 1000) { // 1 second tick
            private boolean propagated = false;

            @Override
            protected void onTick() {
                Tile t = map.getTile(x, y);
                if (t == null) {
                    doDelete();
                    return;
                }

                // 1. Fuel consumption
                double consumption;
                switch (t.getType()) {
                    case 1: // Dry vegetation
                        consumption = RATE_DRY;
                        break;
                    case 2: // Wet vegetation
                        consumption = RATE_WET;
                        break;
                    default:
                        consumption = RATE_DEFAULT;
                }
                t.setFuel(Math.max(0, t.getFuel() - consumption));

                if (t.getHumidity() > 0.1)
                    t.setHumidity(Math.max(t.getHumidity() - 0.1, 0.1));

                // 2. Possibility of fire running out
                if (rnd.nextDouble() < 0.1) {
                    if (t.getFuel() > 0.5) {
                        if (t.getHumidity() > 0.5)
                            t.setType(2); // Wet Vegetation
                        else if (t.getHumidity() > 0.25)
                            t.setType(3); // Normal Vegetation
                        else
                            t.setType(1); // Dry Vegetation
                    } else
                        t.setType(5); // Burnt

                    t.setFireIntensity(0);
                    doDelete();
                }

                // 3. Propagation (one attempt while fuel > 0)
                if (!propagated && t.getFuel() > 0) {
                    for (Direction dir : Direction.values()) {
                        int nx = x + dir.dx;
                        int ny = y + dir.dy;
                        Tile neighbor = map.getTile(nx, ny);
                        if (neighbor == null) continue;

                        int nType = neighbor.getType();
                        if (nType == 0 || nType == 5 || nType == 6 || nType == 4) continue;

                        double p = probMonteAlegre(neighbor);

                        // Adjust by wind direction of current tile
                        double cos = Direction.cos(t.getWindDirection(), dir);
                        p *= ((cos+1.0)/2.0); // ((cos+1.0)/2.0) varia de 0 até 1
                        if (cos == 1) p += 0.15; // Direção do vento
                        else if (cos == 0.5) p -= 0.3; // Impedir que vá muito para os lados
                        else if (cos == 0) p -= 0.3; // Impedir que vá muito para os lados
                        else if (cos == -1) p += 0.1; // Só para não ser 0%
                        p = Math.max(0.01, Math.min(0.99, p));

                        if (rnd.nextDouble() < p) {
                            map.createFireAgent(nx, ny);
                        }
                    }
                    propagated = true;
                }

                // 4. Update fire intensity
                if (t.getFuel() > 0.7) t.setFireIntensity(3);
                else if (t.getFuel() > 0.3) t.setFireIntensity(2);
                else if (t.getFuel() > 0) t.setFireIntensity(1);

                // 5. Extinguish fire if no fuel
                if (t.getFuel() == 0) {
                    t.setType(5); // Burnt
                    t.setFireIntensity(0);
                    doDelete();
                }

                // 6. Update GUI
                if (map.getGui() != null) {
                    map.getGui().repaint();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        map.removeFireAgent(getLocalName()); // Remove agente da lista
        super.takeDown();
    }

    private double probMonteAlegre(Tile t) {
        double V = Math.min(t.getWindVelocity() / 3.0, 1.0);
        double U = t.getHumidity();
        double P = 0.58 + 0.44 * V - 0.38 * U;
        if (t.getType() == 1) P += 0.15; // Dry vegetation
        if (t.getType() == 2) P -= 0.35; // Wet vegetation
        if (t.getFuel() < 0.8) P -= 0.10;
        if (t.getFuel() > 1.5) P += 0.10;
        return Math.max(0.05, Math.min(0.95, P));
    }
}
