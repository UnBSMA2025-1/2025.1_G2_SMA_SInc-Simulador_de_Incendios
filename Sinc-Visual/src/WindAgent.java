import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Random;

public class WindAgent extends Agent {
    private Map map;
    private Direction globalWindDirection;
    private double globalWindVelocity;
    private Random random = new Random();
    private static final int UPDATE_INTERVAL = 5000; // 5 seconds

    public WindAgent() {
    }

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            this.map = (Map) args[0];
        }

        initializeGlobalWind();

        addBehaviour(new WindUpdateBehaviour(this, UPDATE_INTERVAL));
        //addBehaviour(new WindRequestBehaviour());

        System.out.println("WindAgent " + getLocalName() + " started with initial wind: " +
                globalWindDirection + " at " + globalWindVelocity + " m/s");
    }

    private void initializeGlobalWind() {
        globalWindDirection = Direction.values()[random.nextInt(Direction.values().length)];
        globalWindVelocity = 1.0 + 3.0 * random.nextDouble(); // 1-4 m/s
    }

    private class WindUpdateBehaviour extends TickerBehaviour {
        public WindUpdateBehaviour(Agent agent, long period) {
            super(agent, period);
        }

        @Override
        protected void onTick() {
            updateGlobalWind();
            if (map != null && map.windMode == WindMode.GLOBAL) {
                applyGlobalWindToMap();

            }
        }
    }

    private void updateGlobalWind() {
        double changeProb = 0.3;

        if (random.nextDouble() < changeProb) {
            shiftWindDirection();
        }
        else {
            globalWindDirection = Direction.values()[random.nextInt(Direction.values().length)];
        }

        double velocityChange = (random.nextDouble() - 0.5) * 0.8;
        globalWindVelocity = Math.max(0.5, Math.min(4.0, globalWindVelocity + velocityChange));

        System.out.println("Global wind updated: " + globalWindDirection + " at " +
                String.format("%.2f", globalWindVelocity) + " m/s");
    }

    private void shiftWindDirection() {
        Direction[] directions = Direction.values();
        int currentIndex = globalWindDirection.ordinal();

        int nextIndex = (currentIndex + 1) % directions.length;
        int prevIndex = (currentIndex - 1 + directions.length) % directions.length;

        // Choose randomly between the two adjacent directions
        globalWindDirection = random.nextBoolean() ? directions[nextIndex] : directions[prevIndex];
    }

    private void applyGlobalWindToMap() {
        if (map == null) return;

        // Apply global wind to all tiles
        for (int x = 0; x < map.WIDTH; x++) {
            for (int y = 0; y < map.HEIGHT; y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null) {
                    // Set global wind with some local variation
                    double localVariation = 0.8 + 0.4 * random.nextDouble(); // 0.8 to 1.2 multiplier
                    tile.setWindDirection(globalWindDirection);
                    tile.setWindVelocity(globalWindVelocity * localVariation);
                }
            }
        }

        if (map.gui != null) {
            map.gui.repaint();
        }
    }


    @Override
    protected void takeDown() {
        System.out.println("WindAgent " + getLocalName() + " terminating.");
    }

    public Direction getGlobalWindDirection() {
        return globalWindDirection;
    }

    public double getGlobalWindVelocity() {
        return globalWindVelocity;
    }

    public void forceWindUpdate() {
        updateGlobalWind();
        if (map != null && map.windMode == WindMode.GLOBAL) {
            applyGlobalWindToMap();
        }
    }
}