package org.geowebcache.seed.threaded;

import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.TruncateJob;
import org.geowebcache.storage.TileRangeIterator;
import org.geowebcache.seed.TruncateTask;

public class ThreadedTruncateJob extends ThreadedJob implements TruncateJob {

    // Package visible, Jobs should be created by calling the factory method on the TileBreeder
    ThreadedTruncateJob(long id, ThreadedTileBreeder breeder,TileRangeIterator tri, TileLayer tl, boolean doFilterUpdates) {
        super(id, breeder, tl, 1, tri, doFilterUpdates);
        
        threads = new GWCTask[1];
        threads[0] = new TruncateTask(-1, this);
    }

    public void runSynchronously() throws GeoWebCacheException,
            InterruptedException {
        threads[0].doAction();
    }
    
    


}
