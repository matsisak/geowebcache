package org.geowebcache.seed;

import org.geowebcache.layer.TileLayer;
import org.geowebcache.storage.TileRange;

public interface Job {

    /**
     * Get the job ID number of this job.
     * @return
     */
    public long getId();

    /**
     * Get the next location to work on.
     * @return 
     */
    public TileRequest getNextLocation() throws InterruptedException ;
    

    
    /**
     * Get the worker tasks for this job.
     * @return
     */
    public GWCTask[] getTasks();
    
    /**
     * Stop this job
     */
    public void terminate();
    
    /**
     * Get the number of tasks working on this job
     * @return
     */
    public long getThreadCount();
    
    /**
     * Notify the job that one of its threads has started
     * @param thread
     * @throws IllegalArgumentException the task is not a ready task from this Job.
     */
    public void threadStarted(GWCTask thread);
    
    /**
     * Notify the job that one of its threads has ended
     * @param thread
     * @throws IllegalArgumentException the task is not a running task from this Job.
     */
    public void threadStopped(GWCTask thread);
    

    /**
     * Get the tile breeder for this job.
     * @return
     */
    public TileBreeder getBreeder();
    
    
    /**
     * Get the layer this Job is working on.
     * @return
     */
    public TileLayer getLayer();
    
    /**
     * Get the range of tiles this Job is working on.
     * @return
     */
    public TileRange getRange();

    /**
     * Get a report of the status of the job
     * @return
     */
    public JobStatus getStatus();
    
    /**
     * Get the current state of the job, based on the states of its respective tasks.
     * @return
     */
    public GWCTask.STATE getState();
}
