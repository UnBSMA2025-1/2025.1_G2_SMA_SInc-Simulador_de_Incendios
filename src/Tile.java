public class Tile {
    private int x, y, type;
    private double humidity, fuel, windVelocity;
    private Direction windDirection;

    private int fireIntensity = 0;

    public int getFireIntensity() {
        return fireIntensity;
    }

    public void setFireIntensity(int fireIntensity) {
        this.fireIntensity = fireIntensity;
    }

    //Constructor
    public Tile(int x, int y, int type, double humidity, double fuel, double windVelocity, Direction windDirection) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.humidity = humidity;
        this.fuel = fuel;
        this.windVelocity = windVelocity;
        this.windDirection = windDirection;
    }

    //Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getType() { return type; }
    public double getFuel() { return fuel; }
    public double getHumidity() { return humidity; }
    public double getWindVelocity() { return windVelocity; }
    public Direction getWindDirection() { return windDirection; }

    //Setters
    public void setType(int type) { this.type = type; }
    public void setFuel(double fuel) { this.fuel = fuel; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public void setWindVelocity(double windVelocity) { this.windVelocity = windVelocity; }
    public void setWindDirection(Direction windDirection) { this.windDirection = windDirection; }

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
