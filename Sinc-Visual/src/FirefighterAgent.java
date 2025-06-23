import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import java.util.*;

public class FirefighterAgent extends Agent {
    private Map map;
    private int x, y;
    private int speed = 1;
    private int extinguishRadius = 2;

    // BDI Components
    private Set<FireLocation> beliefs;
    private FireLocation currentDesire;
    private Queue<int[]> intentions;
    private int[] previousPos = new int[3];

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            this.x = (Integer) args[0];
            this.y = (Integer) args[1];
            this.map = (Map) args[2];
        }

        this.beliefs = new HashSet<>();
        this.intentions = new LinkedList<>();

        Tile tile = map.getTile(x, y);

        this.previousPos[0] = x;
        this.previousPos[1] = y;
        this.previousPos[2] = tile.getType();

        // Add the main BDI behavior
        addBehaviour(new BDIBehaviour());

        System.out.println("FirefighterAgent " + getLocalName() + " started at position (" + x + ", " + y + ")");
    }

    private class BDIBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // BDI Loop: Perceive -> Decide -> Act
            perceive();
            decide();
            act();

            // Small delay to control simulation speed
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void perceive() {
        beliefs.clear();

        for (int mapX = 0; mapX < map.WIDTH; mapX++) {
            for (int mapY = 0; mapY < map.HEIGHT; mapY++) {
                Tile tile = map.getTile(mapX, mapY);
                if (tile != null && tile.getType() == 4) {
                    beliefs.add(new FireLocation(mapX, mapY));
                }
            }
        }

        if (!beliefs.isEmpty()) {
            System.out.println("FirefighterAgent " + getLocalName() + " perceives " + beliefs.size() + " fires");
        }
    }

    private void decide() {
        if (beliefs.isEmpty()) {
            currentDesire = null;
            intentions.clear();
            return;
        }

        // Find nearest fire
        FireLocation nearestFire = null;
        double minDistance = Double.MAX_VALUE;

        for (FireLocation fire : beliefs) {
            double distance = calculateDistance(x, y, fire.x, fire.y);
            if (distance < minDistance) {
                minDistance = distance;
                nearestFire = fire;
            }
        }

        // Only change desire if we don't have one or found a closer fire
        if (currentDesire == null || nearestFire != currentDesire) {
            currentDesire = nearestFire;
            generateIntentions();
            System.out.println("FirefighterAgent " + getLocalName() + " targeting fire at (" +
                    currentDesire.x + ", " + currentDesire.y + ")");
        }
    }

    // INTENTIONS: Generate simple movement plan
    private void generateIntentions() {
        intentions.clear();

        if (currentDesire == null) return;

        // Simple pathfinding: move step by step toward target
        int currentX = x;
        int currentY = y;

        while (currentX != currentDesire.x || currentY != currentDesire.y) {
            int[] nextMove = new int[2];

            // Move horizontally first, then vertically
            if (currentX < currentDesire.x) {
                nextMove[0] = currentX + 1;
                nextMove[1] = currentY;
                currentX++;
            } else if (currentX > currentDesire.x) {
                nextMove[0] = currentX - 1;
                nextMove[1] = currentY;
                currentX--;
            } else if (currentY < currentDesire.y) {
                nextMove[0] = currentX;
                nextMove[1] = currentY + 1;
                currentY++;
            } else if (currentY > currentDesire.y) {
                nextMove[0] = currentX;
                nextMove[1] = currentY - 1;
                currentY--;
            }

            intentions.add(nextMove);
        }
    }

    // ACT: Execute intentions (move and extinguish)
    private void act() {
        if (tryExtinguishFires()) {
            return; // If we extinguished something, wait for next cycle
        }

        // If no fires to extinguish, move according to intentions
        if (!intentions.isEmpty() && currentDesire != null) {
            int[] nextMove = intentions.poll();

            // Check if the move is valid
            if (isValidMove(nextMove[0], nextMove[1])) {
                Tile tile = this.map.getTile(x,y); //Atual
                tile.setType(this.previousPos[2]);

                x = nextMove[0]; //Futuro
                y = nextMove[1]; //Futuro

                if (map.getGui() != null) {
                    //previousTile.setType(this.previousPos[2]);
                    Tile newTile = this.map.getTile(x, y);
                    this.previousPos[2] = newTile.getType();
                    newTile = this.map.getTile(x, y); //Atual = futuro
                    newTile.setType(7); //Atual = 7
                }
            } else {
                // Invalid move, recalculate path
                generateIntentions();
            }
        }
    }

    // Try to extinguish fires within reach
    private boolean tryExtinguishFires() {
        boolean extinguished = false;

        // Check all tiles within extinguish radius
        for (int dx = -extinguishRadius; dx <= extinguishRadius; dx++) {
            for (int dy = -extinguishRadius; dy <= extinguishRadius; dy++) {
                int checkX = x + dx;
                int checkY = y + dy;

                Tile tile = map.getTile(checkX, checkY);
                if (tile != null && (tile.getType() == 4)) { // Fire tile
                    // Extinguish the fire
                    tile.setType(5); // tile
                    tile.setFuel(0);


                    extinguished = true;
                    System.out.println("FirefighterAgent " + getLocalName() + " extinguished fire at (" +
                            checkX + ", " + checkY + ")");

                    String fireAgentName = "FireAgent_" + checkX + "_" + checkY;
                }
            }
        }


        return extinguished;
    }

    // Helper methods
    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private boolean isValidMove(int newX, int newY) {
        // Check bounds
        if (newX < 0 || newX >= map.WIDTH || newY < 0 || newY >= map.HEIGHT) {
            return false;
        }

        // Check if tile is passable (not water)
        Tile tile = map.getTile(newX, newY);
        return tile != null;
        //&& tile.getType() != 6; // Can't move through water
    }

    // Simple data class for fire locations
    private static class FireLocation {
        int x, y;

        FireLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FireLocation that = (FireLocation) obj;
            return x == that.x && y == that.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    // Getters for debugging/visualization
    public int getX() { return x; }
    public int getY() { return y; }
    public FireLocation getCurrentTarget() { return currentDesire; }
}