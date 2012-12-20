package org.geowebcache.seed.threaded;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.filter.request.RequestFilter;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.seed.Job;
import org.geowebcache.seed.JobStatus;
import org.geowebcache.seed.TaskStatus;
import org.geowebcache.seed.TileRequest;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.TileRangeIterator;
import org.springframework.util.Assert;


/**
 * Base class for seeding jobs
 * 
 * @author Kevin Smith, OpenGeo
 *
 */
abstract class ThreadedJob implements Job {

    private static final Log log = LogFactory.getLog(GWCTask.class);
    
    final private AtomicLong activeThreads = new AtomicLong();
    final protected ThreadedTileBreeder breeder;
    final protected int threadCount;
    final protected long id;
    final protected TileRangeIterator tri;
    final protected TileLayer tl;
    
    public static final long TIME_NOT_STARTED=-1;
    private long groupStartTime=TIME_NOT_STARTED;
    
    private final boolean doFilterUpdate;
    
    /**
     * 
     * @param id unique ID of the Job
     * @param breeder the TileBreeder that created this job
     * @param tl the layer this job is operating on
     * @param threadCount the number of threads to try to use
     * @param tri iterator over the tiles to be handled
     * @param doFilterUpdate update relevant filters on the layer after the job completes
     */
    protected ThreadedJob(long id, ThreadedTileBreeder breeder, TileLayer tl, int threadCount, 
            TileRangeIterator tri, boolean doFilterUpdate) {

        Assert.notNull(breeder);
        Assert.notNull(tl);
        Assert.notNull(tri);
        Assert.isTrue(threadCount>0,"threadCount must be positive");
        Assert.isTrue(id>=0,"Job id must be non-negative");
        
        this.breeder = breeder;
        this.threadCount = threadCount;
        this.id = id;
        this.tri = tri;
        this.tl = tl;
        this.doFilterUpdate = doFilterUpdate;
    }

    protected GWCTask[] threads;

    public ThreadedTileBreeder getBreeder() {
        return breeder;
    }

    public long getId() {
        return id;
    }

    public GWCTask[] getTasks() {
        return threads;
    }

    
    public TileRequest getNextLocation() throws InterruptedException {
        long gridLoc[] = tri.nextMetaGridLocation();
        if (gridLoc == null) return null;  // We're done
        return new ThreadedTileRequest(gridLoc[0], gridLoc[1], gridLoc[2]);
    }
    
    protected void myTask(GWCTask task) {
        Assert.isTrue(task.getJob()==this, "Task does not belong to this Job");
    }
    
    public long getThreadCount() {
        return activeThreads.get();
    }

    public void threadStarted(GWCTask thread) {
        myTask(thread);
        
        activeThreads.incrementAndGet();
        if(this.groupStartTime==TIME_NOT_STARTED){
            this.groupStartTime=System.currentTimeMillis();
        }
    }

    public void threadStopped(GWCTask thread) {
        myTask(thread);
        
        activeThreads.decrementAndGet();
        
        long membersRemaining = activeThreads.decrementAndGet();
        if (0 == membersRemaining) {
            finished();
        }
    }
    
    public TileRange getRange() {
        return tri.getTileRange();
    }

    public TileLayer getLayer() {
        return tl;
    }

    /**
     * Called when all the tasks in the job have finished running.
     */
    protected void finished(){
        if (doFilterUpdate) {
            runFilterUpdates();
        }
        double groupTotalTimeSecs = (System.currentTimeMillis() - (double) groupStartTime) / 1000;
        log.info("Job "+id+" finished " /*+ parsedType*/ + " after "
                + groupTotalTimeSecs + " seconds");
    }
    
    /**
     * Updates any request filters
     */
    private void runFilterUpdates() {
        // We will assume that all filters that can be updated should be updated
        List<RequestFilter> reqFilters = tl.getRequestFilters();
        if (reqFilters != null && !reqFilters.isEmpty()) {
            for(RequestFilter reqFilter: reqFilters) {
                if (reqFilter.update(tl, getRange().getGridSetId())) {
                    log.debug("Updated request filter " + reqFilter.getName());
                } else {
                    log.debug("Request filter " + reqFilter.getName()
                            + " returned false on update.");
                }
            }
        }
    }

    public void terminate() {
        for(GWCTask task: threads){
            task.terminateNicely();
        }
    }

    protected class ThreadedTileRequest implements TileRequest {
    
        public long[] getGridLoc() {
            return gridLoc;
        }
    
        final long gridLoc[];
        
        long retryAt;
        long failures;
        
    
        
        ThreadedTileRequest(long x, long y, long zoom) {
            super();
            this.gridLoc = new long[] {x, y, zoom};
        }
    
        public int compareTo(TileRequest o) {
            return Long.signum(this.retryAt - o.getRetryAt());
        }
    
        public long getX() {
            return gridLoc[0];
        }
    
        public long getY() {
            return gridLoc[1];
        }
    
        public long getZoom() {
            return gridLoc[2];
        }
    
        public long getRetryAt() {
            return retryAt;
        }
    
        public long getFailures() {
            return failures;
        }
    
        public Job getJob() {
            return ThreadedJob.this;
        }
        
    }

    /**
     * Returns the current status of the job.  Does not do any locking.
     */
    public JobStatus getStatus() {
        Collection<TaskStatus> taskStatuses = new ArrayList<TaskStatus>(threads.length);
        for(GWCTask task: threads) {
            taskStatuses.add(task.getStatus());
        }
        return new JobStatus(taskStatuses, System.currentTimeMillis(), this.id);
    }

    public STATE getState() {
        Assert.state(threads.length>0, "Job should have at least one task.");
        boolean allReadyUnset = true; // No tasks that aren't READY or UNSET have been seen
        boolean running = false; // At least one running task has been seen
        for(GWCTask task: threads){
            switch(task.getState()){
            case DEAD:
                return STATE.DEAD; // TODO not sure this is right, maybe it should only be if all are DEAD.
            case DONE:
                allReadyUnset = false;
                break;
            case READY:
                break;
            case RUNNING:
                allReadyUnset = false;
                running = true;
                break;
            default:
                break;
            }
        }
        // None are dead, some are running, so the job is running
        if(running) {
            return STATE.RUNNING;
        }
        // All are Ready/Unset
        if(allReadyUnset) {
            return STATE.READY;
        }
        // Some are Done, any others are Ready/Unset
        return STATE.DONE;
    }
    
    
}
