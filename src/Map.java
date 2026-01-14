

/**
 * This class represents a 2D map (int[w][h]) as a "screen" or a raster matrix or maze over integers.
 * This is the main class needed to be implemented.
 *
 * @author boaz.benmoshe
 *
 */
public class Map implements Map2D{
private int [][] _map ;
private boolean _cyclicFlag = false;
    // edit this class below
	/**
	 * Constructs a w*h 2D raster map with an init value v.
	 * @param w
	 * @param h
	 * @param v
	 */
	public Map(int w, int h, int v) {init(w, h, v);}

    /**
	 * Constructs a square map (size*size).
	 * @param size
	 */
	public Map(int size) {this(size,size, 0);}

	/**
	 * Constructs a map from a given 2D array.
	 * @param data
	 */
	public Map(int[][] data) {
		init(data);
	}

    public Map() {

    }

    /**
     * Initializes the internal matrix to the given dimensions and fills every entry with v.
     * Existing data is discarded. Both dimensions must be strictly positive.
     *
     * @param w desired width (number of columns)
     * @param h desired height (number of rows)
     * @param v value assigned to every cell in the map
     * @throws RuntimeException if { w <= 0} or { h <= 0}
     */

	@Override
	public void init(int w, int h, int v) {
        if (w <= 0 || h <= 0) {
            throw new RuntimeException("Invalid map size");
        }

        this._map = new int[w][h];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                this._map[i][j] = v;
            }
        }
	}

    /**
     * Initializes this map by deep-copying the provided 2D array.
     * Rejects null inputs, empty arrays, or ragged arrays where rows differ in length.
     *
     * @param arr source matrix to copy
     * @throws RuntimeException for null, empty, or ragged inputs
     */

	@Override
	public void init(int[][] arr) {
        // check null
        if (arr == null) {
            throw new RuntimeException("Array is null");
        }

        // check empty array
        if (arr.length == 0 || arr[0] == null || arr[0].length == 0) {
            throw new RuntimeException("Array is empty");
        }

        int w = arr.length;
        int h = arr[0].length;

        // check ragged array
        for (int i = 0; i < w; i++) {
            if (arr[i] == null || arr[i].length != h) {
                throw new RuntimeException("Ragged 2D array");
            }
        }

        // deep copy
        this._map = new int[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                this._map[i][j] = arr[i][j];
            }
        }
    }

    /**
     * Returns a deep copy of the underlying map to preserve encapsulation.
     * If the map has not been initialized, {null} is returned.
     *
     * @return deep copy of the current map or {null} if uninitialized
     */

	@Override
	public int[][] getMap() {
        if (this._map == null) {
            return null;
        }

        int w = this.getWidth();
        int h = this.getHeight();

        int [][] deepCopy = new int[w][h];
    for(int i=0; i<w;i++){
        for(int j=0;j<h;j++){
    deepCopy[i][j]= this._map[i][j];

        }
    }

		return deepCopy;
	}

    /**
     * Reports the current width (number of columns) of the map.
     * Assumes the map has been initialized beforehand.
     *
     * @return map width
     */

	@Override
	public int getWidth() {
        return this._map.length;
    }

    /**
     * Reports the current height (number of rows) of the map.
     * Assumes the map has been initialized beforehand.
     *
     * @return map height
     */

	@Override
	public int getHeight() {
        return this._map[0].length;
    }

    /**
     * Retrieves the value stored at the coordinate ({x},{y}).
     * Coordinates must be within bounds or a {RuntimeException} is thrown.
     *
     * @param x column index
     * @param y row index
     * @return value stored at the requested coordinate
     */
	@Override
	public int getPixel(int x, int y) {
        if(x<0 || y<0 || x>=this.getWidth() || y>= this.getHeight()){
            throw new RuntimeException("out of bounds");
        }
        return this._map[x][y];
    }

    /**
     * Retrieves the value stored at the provided coordinate.
     * A null pixel or an out-of-bounds coordinate will raise a RuntimeException.
     *
     * @param p coordinate to read
     * @return value stored at {p}
     */

	@Override
	public int getPixel(Pixel2D p) {
        if (p==null) {
            throw new RuntimeException("p is null");
        }
        if (p.getX() < 0 || p.getY() < 0 ||
                p.getX() >= this.getWidth() || p.getY() >= this.getHeight()) {
            throw new RuntimeException("Pixel out of bounds: " + p);
        }

        // Index2D other = (Index2D) p;
        return this._map[p.getX()][p.getY()];
    }
    /**
     * Writes {v} into the coordinate ({x},{y}).
     * Requires the map to be initialized and the coordinates to be valid.
     *
     * @param x column index to modify
     * @param y row index to modify
     * @param v value to assign
     * @throws RuntimeException when the map is uninitialized or indices are out of bounds
     */

	@Override
	public void setPixel(int x, int y, int v) {

        if (_map == null) {
            throw new RuntimeException("Map is not initialized");
        }

        if (x < 0 || y < 0 || x >= this.getWidth() || y >= this.getHeight()) {
            throw new RuntimeException(
                    "Pixel out of bounds: (" + x + "," + y + ")"
            );
        }

        this._map[x][y] = v;
    }

    /**
     * Writes {v} into the coordinate represented by {p}.
     * Null inputs or out-of-bounds coordinates result in a {RuntimeException}.
     *
     * @param p coordinate to modify
     * @param v value to assign
     */

	@Override
	public void setPixel(Pixel2D p, int v) {
        if (p==null) {
            throw new RuntimeException("p is null");
        }
        if (p.getX() < 0 || p.getY() < 0 ||
                p.getX() >= this.getWidth() || p.getY() >= this.getHeight()) {
            throw new RuntimeException("Pixel out of bounds: " + p);
        }
        this._map[p.getX()][p.getY()] = v;
	}

    /**
     * Checks whether the given coordinate lies within the current map bounds.
     *
     * @param p coordinate to test
     * @return {true} if the coordinate is valid for this map, {false} otherwise
     * @throws RuntimeException when {p} is null
     */

    @Override
    public boolean isInside(Pixel2D p) {
        if (p == null) {
            throw new RuntimeException("p is null");
        }
        if (p.getX() < 0 || p.getY() < 0 ||
                p.getX() >= getWidth() || p.getY() >= getHeight()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isCyclic() {
        return _cyclicFlag;
    }

    @Override
    public void setCyclic(boolean cy) {
        _cyclicFlag = cy;
    }






	@Override
	/** 
	 * Fills this map with the new color (new_v) starting from p.
	 *
     *  Fills (paints) the connected component of a given pixel with a new color.
     *
     *  The connected component of a pixel p consists of all pixels that:
     *  - have the same original color as p
     *  - are connected to p using 4-neighbors (up, down, left, right)
     *
     *  If cyclic is true, the map is treated as cyclic (crossing one border enters
     *  from the opposite side). If cyclic is false, pixels outside the map are ignored
     *  and the starting pixel must be inside the map.
     *
     *  If the color of the starting pixel is already new_v, no filling is performed
     *  and the method returns 0.
     *
     *  @param xy the starting pixel for the fill operation
     *  @param new_v the new color to apply
     *  @param cyclic true if the map is cyclic, false otherwise
     *  @return the number of pixels that were filled
     */

	public int fill(Pixel2D xy, int new_v) {
        if (xy == null) {
            throw new RuntimeException("pixel is null");
        }
        if (this._map == null) {
            throw new RuntimeException("map is null");
        }

        int w = getWidth();
        int h = getHeight();

        int x = xy.getX();
        int y = xy.getY();

        // אם לא מחזורי - חייב להיות בתוך המפה
        if (!isCyclic()) {
            if (!this.isInside(xy)) {
                throw new RuntimeException("out of bounds");
            }
        } else {
            // מחזורי: עיטוף נקודת התחלה (מספיק step אחד כי השכנים הם ±1)
            if (x < 0) x = w - 1;
            if (x >= w) x = 0;
            if (y < 0) y = h - 1;
            if (y >= h) y = 0;
        }

        int oldColor = this._map[x][y];
        if (oldColor == new_v) {
            return 0;
        }

        return fillDfs(x, y, oldColor, new_v);
    }

    @Override
	/**
     * Computes the shortest valid path between {p1} and {p2} while avoiding pixels marked {obsColor}.
     * Uses a BFS-based distance map generated by {allDistance(Pixel2D, int, boolean)} and reconstructs the path
     * by walking backward from the destination. Returns {null} if either endpoint is an obstacle
     * or no valid path exists.
     *
     * @param p1 starting point
     * @param p2 destination point
     * @param obsColor value treated as an obstacle
     * @param cyclic whether the map wraps around its borders
     * @return array of consecutive pixels from {p1} to {p2}, or {null} when unreachable
     */
	public Pixel2D[] shortestPath(Pixel2D p1, Pixel2D p2, int obsColor) {
        if (p1 == null||p2==null){
            throw new RuntimeException("p1 or p2 is null");
        }
        int w = getWidth();
        int h = getHeight();

        int x1 = p1.getX();
        int y1 = p1.getY();
        int x2 = p2.getX();
        int y2 = p2.getY();

        if (!isCyclic()) {
            if (!this.isInside(p1) || !this.isInside(p2)) {
                throw new RuntimeException("out of bounds");
            }
        }else{
            x1 = wrap(x1, w);
            y1 = wrap(y1, h);
            x2 = wrap(x2, w);
            y2 = wrap(y2, h);
        }

        if (_map[x1][y1] == obsColor || _map[x2][y2] == obsColor) {
            return null;
        }

        Map2D distMap = allDistance(new Index2D(x1, y1), obsColor);
        int endDist = distMap.getPixel(x2, y2);
        if (endDist < 0) {
            return null;
        }

        Pixel2D[] path = new Pixel2D[endDist + 1];
        int cx = x2;
        int cy = y2;
        int currDist = endDist;

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (currDist >= 0) {
            path[currDist] = new Index2D(cx, cy);
            if (currDist == 0) {
                break;
            }

            boolean foundPrev = false;
            for (int k = 0; k < 4 && !foundPrev; k++) {
                int nx = cx + dx[k];
                int ny = cy + dy[k];

                if (isCyclic()) {
                    nx = wrap(nx, w);
                    ny = wrap(ny, h);
                } else {
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h) {
                        continue;
                    }
                }

                if (distMap.getPixel(nx, ny) == currDist - 1) {
                    cx = nx;
                    cy = ny;
                    currDist--;
                    foundPrev = true;
                }
            }

            if (!foundPrev) {
                // Should not happen if distances are consistent
                return null;
            }
        }

        return path;
    }

    /**
     * Builds a map of shortest-path distances from {start} to every accessible pixel,
     * treating {obsColor} as an obstacle. Unreachable pixels are marked with {-1}.
     * The computation uses a queue-based BFS over 4-neighborhood connectivity.
     *
     * @param start starting coordinate
     * @param obsColor value representing an obstacle
     * @return new {Map2D} of the same size containing distances or {-1} for unreachable entries
     */

    @Override
    public Map2D allDistance(Pixel2D start, int obsColor) {
        if (start == null) throw new RuntimeException("start is null");

        int w = getWidth();
        int h = getHeight();
        int sx = start.getX();
        int sy = start.getY();

        if (!isCyclic()) {
            if (!isInside(start)) throw new RuntimeException("start out of bounds");
        } else {
            sx = wrap(sx, w);
            sy = wrap(sy, h);
        }

        Map result = new Map(w, h, -1);
        result.setCyclic(this.isCyclic());

        if (_map[sx][sy] == obsColor) {
            return result;
        }

        int maxSize = w * h;
        int[] qx = new int[maxSize];
        int[] qy = new int[maxSize];
        int head = 0, tail = 0;

        result.setPixel(sx, sy, 0);
        qx[tail] = sx;
        qy[tail] = sy;
        tail++;

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (head < tail) {
            int cx = qx[head];
            int cy = qy[head];
            head++;

            for (int k = 0; k < 4; k++) {
                int nx = cx + dx[k];
                int ny = cy + dy[k];

                if (isCyclic()) {
                    nx = wrap(nx, w);
                    ny = wrap(ny, h);
                } else {
                    if (nx < 0 || nx >= w || ny < 0 || ny >= h) continue;
                }

                if (_map[nx][ny] == obsColor) continue;
                if (result.getPixel(nx, ny) != -1) continue;

                result.setPixel(nx, ny, result.getPixel(cx, cy) + 1);
                qx[tail] = nx;
                qy[tail] = ny;
                tail++;
            }
        }

        return result;
    }


    ////////////////////// Private Methods ///////////////////////

    /**
     * Recursively fills the connected component using a depth-first search (DFS).
     *
     * Starting from the pixel at coordinates (x, y), this method:
     * - checks boundary conditions (or wraps coordinates if cyclic is true)
     * - verifies that the pixel has the original color oldColor
     * - paints the pixel with the new color new_v
     * - continues the fill process on the four neighboring pixels
     *
     * The coloring itself is used as a "visited" marker to prevent infinite recursion.
     *
     * @param x the x-coordinate of the current pixel
     * @param y the y-coordinate of the current pixel
     * @param oldColor the original color of the connected component
     * @param new_v the new color to apply
     * @return the number of pixels filled by this call and its recursive calls
     */
    private int fillDfs(int x, int y, int oldColor, int new_v) {
        int w = getWidth();
        int h = getHeight();
        
        if (isCyclic()) {
            if (x < 0) x = w - 1;
            if (x >= w) x = 0;
            if (y < 0) y = h - 1;
            if (y >= h) y = 0;
        } else {
            if (x < 0 || x >= w || y < 0 || y >= h) {
                return 0;
            }
        }

        if (this._map[x][y] != oldColor) {
            return 0;
        }

        this._map[x][y] = new_v;
        int count = 1;

        count += fillDfs(x + 1, y, oldColor, new_v);
        count += fillDfs(x - 1, y, oldColor, new_v);
        count += fillDfs(x, y + 1, oldColor, new_v);
        count += fillDfs(x, y - 1, oldColor, new_v);

        return count;
    }


     /**
     * Wraps a coordinate into the range {@code [0, limit-1]} to support cyclic behavior.
     * @param value coordinate that may be outside the map
     * @param limit upper bound (exclusive) of the axis length
     * @return wrapped coordinate within valid bounds
     */

     private int wrap(int value, int limit) {
         int r = value % limit;
         if (r < 0) r += limit;
         return r;
     }


}
