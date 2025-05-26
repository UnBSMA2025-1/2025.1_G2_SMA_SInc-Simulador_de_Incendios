public enum Direction {
    N ( 0,-1), NE(+1,-1), E(+1, 0), SE(+1,+1),
    S ( 0,+1), SW(-1,+1), W(-1, 0), NW(-1,-1);

    public final int dx;
    public final int dy;
    Direction(int dx,int dy){ this.dx=dx; this.dy=dy; }

    public static double cos(Direction a, Direction b){
        int dot = a.dx*b.dx + a.dy*b.dy;
        double mag = Math.sqrt(a.dx*a.dx+a.dy*a.dy)*Math.sqrt(b.dx*b.dx+b.dy*b.dy);
        return dot/mag;
    }

    public static Direction fromStep(int dx,int dy){
        for(Direction d: values()) if(d.dx==Integer.signum(dx) && d.dy==Integer.signum(dy)) return d;
        return N;
    }
}