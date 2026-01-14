public class Index2D implements Pixel2D {
    /** X coordinate of the pixel */
    private int _x;

    /** Y coordinate of the pixel */
    private int _y;
    public Index2D() {this(0,0);}
    /**
     * Constructs a new Index2D with the given x and y coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Index2D(int x, int y) {
        this._x = x;
        this._y = y;
    }

    /**
     * Copy constructor.
     * Creates a new Index2D from another Pixel2D object.
     *
     * @param t the Pixel2D object to copy from
     * @throws IllegalArgumentException if other is null
     */
    public Index2D(Pixel2D t) {
        if (t == null) {
            throw new IllegalArgumentException("other is null");
        }
        this._x = t.getX();
        this._y = t.getY();
    }
    /**
     * Returns the x coordinate of this pixel.
     *
     * @return the x coordinate
     */
    @Override
    public int getX() {

        return this._x;
    }
    /**
     * Returns the y coordinate of this pixel.
     *
     * @return the y coordinate
     */
    @Override
    public int getY() {

        return this._y;
    }

    /**
     * Computes the Euclidean distance between this pixel
     * and another Pixel2D.
     *
     * @param t the other pixel
     * @return the distance between the two pixels
     * @throws RuntimeException if p2 is null
     */
    @Override
    public double distance2D(Pixel2D t) {
        if (t == null) {
            throw new RuntimeException("t is null");
        }
        double dx = this._x - t.getX();
        double dy = this._y - t.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
    /**
     * Returns a string representation of this Index2D
     * in the format "(x,y)".
     *
     * @return string representation of the pixel
     */
    @Override
    public String toString() {

        return "(" + _x + "," + _y + ")";
    }

    /**
     * Compares this Index2D with another object for equality.
     * Two Index2D objects are equal if they have the same
     * x and y coordinates.
     *
     * @param t the object to compare with
     * @return true if the objects represent the same coordinates,
     *         false otherwise
     */
    @Override
    public boolean equals(Object t) {
        boolean ans = false;
        /////// you do NOT need to add your code below ///////
        if(t instanceof Pixel2D) {
            Pixel2D p = (Pixel2D) t;
            ans = (this.distance2D(p)==0);
        }
        ///////////////////////////////////
        return ans;
    }
}
