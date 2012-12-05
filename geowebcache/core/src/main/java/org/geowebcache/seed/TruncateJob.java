package org.geowebcache.seed;

import org.geowebcache.GeoWebCacheException;

public interface TruncateJob extends Job {

    /**
     * Run the job synchronously within the current thread.
     */
    public void runSynchronously() throws GeoWebCacheException, InterruptedException;
}
