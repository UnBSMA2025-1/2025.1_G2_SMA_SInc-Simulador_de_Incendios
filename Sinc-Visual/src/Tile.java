public class Tile {
    private int x, y, type;
    private float temperature;
    private double humidity, fuel, windVelocity, terrainHeight;
    private Direction windDirection;

    //Constructor
    public Tile(int x, int y, int type, double humidity, double fuel, double windVelocity, Direction windDirection, float temperature, double terrainHeight) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.humidity = humidity;
        this.fuel = fuel;
        this.windVelocity = windVelocity;
        this.windDirection = windDirection;
        this.temperature = temperature;
        this.terrainHeight = terrainHeight;
    }

    //Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getType() { return type; }
    public double getFuel() { return fuel; }
    public double getHumidity() { return humidity; }
    public double getWindVelocity() { return windVelocity; }
    public Direction getWindDirection() { return windDirection; }
    public float getTemperature() { return temperature; }
    public double getTerrainHeight() { return terrainHeight; }

    //Setters
    public void setType(int type) { this.type = type; }
    public void setFuel(double fuel) { this.fuel = fuel; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public void setWindVelocity(double windVelocity) { this.windVelocity = windVelocity; }
    public void setWindDirection(Direction windDirection) { this.windDirection = windDirection; }
    public void setTemperature(float temperature) { this.temperature = temperature; }


    @Override
    public String toString() {
        return "Tile(" + x + "," + y + ") - " +
                " Type: " + type +
                ", Humidity: " + humidity +
                ", Fuel: " + fuel +
                ", Wind Velocity: " + windVelocity +
                ", Wind Direction: " + windDirection +
                "\n";
    }
}
