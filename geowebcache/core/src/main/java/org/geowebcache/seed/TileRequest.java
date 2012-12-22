package org.geowebcache.seed;

public interface TileRequest extends Comparable<TileRequest>{

    /**
     * Return the x, y, and zoom of the tile as an array.
     * @return
     */
    public long[] getGridLoc();
    
    /**
     * Get the X ordinate of the tile
     * @return
     */
    public long getX();
    
    /**
     * Get the Y ordinate of the tile
     * @return
     */
    public long getY();
    
    /**
     * Get the Zoom level of the tile
     * @return
     */
    public long getZoom();
    
    /**
     * Get the time at which this request can be retried.
     * @return
     */
    public long getRetryAt();
    
    /**
     * Get the number of failed attempts at this request.
     * @return
     */
    public long getFailures();
    
    /**
     * Get the job that issued this request.
     * @return
     */
    public Job getJob();
    
    /**
     * Compare two TileRequests by their retryAt properties.
     */
    public int compareTo(TileRequest o);
}
