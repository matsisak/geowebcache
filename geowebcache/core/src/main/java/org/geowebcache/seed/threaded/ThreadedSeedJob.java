package org.geowebcache.seed.threaded;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.SeedJob;
import org.geowebcache.seed.SeedTask;
import org.geowebcache.seed.TileRequest;
import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.storage.TileRangeIterator;
import org.springframework.util.Assert;

public class ThreadedSeedJob extends ThreadedJob implements SeedJob  {
    private static final Log log = LogFactory.getLog(org.geowebcache.seed.threaded.ThreadedSeedJob.class);

    final private AtomicLong failureCount = new AtomicLong();
    final private Queue<ThreadedTileRequest> retry = new LinkedList<ThreadedTileRequest>();
    final private boolean reseed;
    
    final private int tileFailureRetryCount; // Max failures per request
    final private long tileFailureRetryWaitTime; 
    final private long totalFailuresBeforeAborting; // Max failures total 
    
   
    
    // Package visible, Jobs should be created by calling the factory method on the TileBreeder
    ThreadedSeedJob(long id, int threadCount, ThreadedTileBreeder breeder, boolean reseed, TileRangeIterator tri, TileLayer tl,
            int tileFailureRetryCount, long tileFailureRetryWaitTime,
            long totalFailuresBeforeAborting, boolean doFilterUpdates) {
        super(id, breeder, tl, threadCount, tri, doFilterUpdates);
        
        this.reseed = reseed;
        this.tileFailureRetryCount = tileFailureRetryCount;
        this.tileFailureRetryWaitTime = tileFailureRetryWaitTime;
        this.totalFailuresBeforeAborting = totalFailuresBeforeAborting;
        
        threads = new GWCTask[threadCount];
        
        for (int i=0; i<threadCount; i++){
            threads[i] = new SeedTask(-1, this);
        }
    }
        
    /**
     * Get the next tile to try to request.
     */
    public TileRequest getNextLocation() throws InterruptedException {
        ThreadedTileRequest request = null;
        synchronized(retry) {
            // note, we're only checking if there's a tile we can work on, so peek, 
            // then poll to remove it from the queue if we decide to use it.
            request = retry.peek();
            
            if (request==null) {
                // No requests need to be retried so get a new one.
                request = (ThreadedTileRequest) super.getNextLocation();
            } else {
                // There's a request in the retry queue
                
                long delay = System.currentTimeMillis() - request.retryAt;
                
                if (delay>0) {
                    // It's not time for this request yet.
                    // They should be in roughly ascending order so no need to check the rest.
                    
                    // get a new tile
                    ThreadedTileRequest newRequest=(ThreadedTileRequest) super.getNextLocation();
                    if (newRequest!=null) {
                        // Another tile can be done in the meantime
                        request=newRequest;
                    } else {
                        // Nothing to do but wait so remove the it from the queue.
                        retry.poll();
                    }
                } else  {
                    // Retry request is ready to go so remove it from the queue.
                    retry.poll();
                }
            }
        } // End of synchronized(retry)
        
        // Check if we need to delay before returning it.
        if(request!=null){
            long delay = request.retryAt - System.currentTimeMillis();
            if (delay>0) {
                Thread.sleep(delay);
            }
        }
        return request;
    }
    
    
    public void failure(GWCTask task, TileRequest request, Exception e) throws GeoWebCacheException{
        Assert.isTrue(task.getJob()==this, "Task is not from this job.");
        Assert.isInstanceOf(ThreadedTileRequest.class, request);
        Assert.isTrue(request.getJob()==this, "Tile");
        
        ThreadedTileRequest tRequest = (ThreadedTileRequest) request;
        
        // TODO check, should this be tileFailureRetryCount or totalFailuresBeforeAborting?
        if (tileFailureRetryCount() == 0){
            // if GWC_SEED_RETRY_COUNT was not set then none of the settings have effect, in
            // order to keep backwards compatibility with the old behaviour
            if (tileFailureRetryCount() == 0) {
                if (e instanceof GeoWebCacheException) {
                    throw (GeoWebCacheException) e;
                }
                throw new GeoWebCacheException(e);
            }
        }
        
            
        if( failureCount.incrementAndGet() >= totalFailuresBeforeAborting ){
            // Too many failures, kill the task.
            // TODO: kill the entire job properly.
            log.info("Aborting seed thread " + Thread.currentThread().getName()
                    + ". Error count reached configured maximum of "
                    + totalFailuresBeforeAborting);
            breeder.setTaskState(task, GWCTask.STATE.DEAD);
            return;
        }
        
        String logMsg = "Seed failed at " + tRequest.toString() + " after "
                + (tRequest.failures+1) + " of " + (tileFailureRetryCount + 1)
                + " attempts.";
        
        if (tRequest.failures < tileFailureRetryCount) {
            // Try again
            tRequest.retryAt=System.currentTimeMillis()+tileFailureRetryWaitTime;
            tRequest.failures+=1;
            retry.add(tRequest);
            log.trace("Waiting " + tileFailureRetryWaitTime
                    + " before trying again");
        } else {
            // Give up on this tile
            log.info(logMsg
                    + " Skipping and continuing with next tile. Original error: "
                    + e.getMessage());
        }
    }
    
    public long totalFailuresBeforeAborting() {
        return totalFailuresBeforeAborting;
    }

    public int tileFailureRetryCount() {
        return this.tileFailureRetryCount;
    }

    public long tileFailureRetryWaitTime() {
        return this.tileFailureRetryWaitTime;
    }

    public long getFailures() {
        return failureCount.get();
    }

    public boolean isReseed() {
        return this.reseed;
    }

}
