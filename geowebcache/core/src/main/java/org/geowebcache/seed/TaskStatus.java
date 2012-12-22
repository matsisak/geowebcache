package org.geowebcache.seed;

import org.geowebcache.seed.GWCTask.STATE;

/**
 * Status of a seeding task at a particular time
 *
 */
public class TaskStatus {
    final private long time;
    final private long tilesDone;
    final private long tilesTotal;
    final private long timeRemaining;
    final private long taskId;
    final private GWCTask.STATE status;

    /**
     * 
     * @param time
     * @param tilesDone
     * @param tilesTotal
     * @param timeRemaining
     * @param taskId
     * @param status
     */
    public TaskStatus(long time, long tilesDone, long tilesTotal,
            long timeRemaining, long taskId, STATE status) {
        super();
        this.time = time;
        this.tilesDone = tilesDone;
        this.tilesTotal = tilesTotal;
        this.timeRemaining = timeRemaining;
        this.taskId = taskId;
        this.status = status;
    }

    /**
     * The time that the status was recorded
     * @return
     */
    public long getTime() {
        return time;
    }

    /**
     * The number of tiles the task has processed
     * @return
     */
    public long getTilesDone() {
        return tilesDone;
    }

    /**
     * The number of tiles assigned to the task
     * @return
     */
    public long getTilesTotal() {
        return tilesTotal;
    }

    /**
     * The number of tiles remaining for the task
     * @return
     */
    public long getTilesRemaining() {
        return timeRemaining;
    }


    /**
     * The id of the task
     * @return
     */
   public long getTaskId() {
        return taskId;
    }

   /**
    * The status of the task
    * @return
    */
    public GWCTask.STATE getStatus() {
        return status;
    }
    
    
}
