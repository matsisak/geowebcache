package org.geowebcache.seed;

import org.geowebcache.GeoWebCacheException;

public interface SeedJob extends Job {

    /**
     * Notify of a failure.
     * @param task the task which failed.
     * @param gridLoc the tile location which failed.
     * @param e the cause of the failure.
     */
    public void failure(GWCTask task, TileRequest request, Exception e) throws GeoWebCacheException;
    
    /**
     * Get the number of failures allowed before the job as a whole fails.
     * @return
     */
    public long totalFailuresBeforeAborting();
    
    /**
     * Get the number of failures allowed on a particular tile before giving up on it.
     * @return
     */
    public int tileFailureRetryCount();
    
    /**
     * Time in milliseconds to wait before re-attempting a request.
     * @return
     */
    public long tileFailureRetryWaitTime(); 

    /**
     * Get the number of failures that have occurred.
     * @return
     */
    public long getFailures();
    
    /**
     * Is this Job reseeding existing tiles.
     * @return
     */
    public boolean isReseed();
}
