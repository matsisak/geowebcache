/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Arne Kepp / The Open Planning Project 2008 
 */
package org.geowebcache.seed;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheException;
import org.springframework.util.Assert;

/**
 * 
 */
public abstract class GWCTask {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(GWCTask.class);

    public static enum TYPE {
        UNSET, SEED, RESEED, TRUNCATE
    };

    public static enum STATE {
        UNSET, READY, RUNNING, DONE, DEAD
    };

    //protected int threadOffset = 0;

    protected final Job parentJob;
    
    protected final long taskId;

    protected final TYPE parsedType; // = TYPE.UNSET; // TODO Do we need the UNSET type?

    protected STATE state = STATE.UNSET;

    // protected final String layerName;

    protected long timeSpent = -1;

    protected long timeRemaining = -1;

    protected long tilesDone = -1;

    protected long tilesTotal = -1;

    protected boolean terminate = false;
    
    //private long groupStartTime;

    
    
    public GWCTask(long taskId, Job parentJob, TYPE parsedType) {
        super();
        this.taskId = taskId;
        this.parentJob = parentJob;
        this.parsedType = parsedType;
    }
    /**
     * Marks this task as active in the group by incrementing the shared counter, delegates to
     * {@link #doActionInternal()}, and makes sure to remove this task from the group count.
     */
    public final void doAction() throws GeoWebCacheException, InterruptedException {
        Assert.state(this.state==STATE.READY, "Task can not be started as it is "+state+" rather than READY");
        state=STATE.RUNNING;
        parentJob.threadStarted(this);
        try {
            doActionInternal();
        } finally {
            // If it's not DONE, it's DEAD at this point.
            if(state!=STATE.DONE && state!=STATE.DEAD){
                log.info("Task "+Long.toString(taskId)+"reached end but was still in state "+state+".  Setting to DEAD.");
                state=STATE.DEAD;
            }
            dispose();
            parentJob.threadStopped(this);
        }
    }


    /**
     * Called when task completes
     */
    protected abstract void dispose();

    /**
     * Extension point for subclasses to do what they do
     */
    protected abstract void doActionInternal() throws GeoWebCacheException, InterruptedException;

    public long getTaskId() {
        return taskId;
    }

    public Job getJob() {
        return parentJob;
    }

    public String getLayerName() {
        return parentJob.getLayer().getName();
    }

    /**
     * @return total number of tiles (in the whole task group), or < 0 if too many to count
     */
    public long getTilesTotal() {
        return tilesTotal;
    }

    public long getTilesDone() {
        return tilesDone;
    }

    /**
     * @return estimated remaining time in seconds, or {@code -2} if unknown
     */
    public long getTimeRemaining() {
        if (tilesTotal > 0) {
            return timeRemaining;
        } else {
            return -2;
        }
    }

    /**
     * @return task time spent in seconds
     */
    public long getTimeSpent() {
        return timeSpent;
    }

    public void terminateNicely() {
        this.terminate = true;
    }

    public TYPE getType() {
        return parsedType;
    }

    public STATE getState() {
        return state;
    }

    protected void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            this.state = STATE.DEAD;
            parentJob.threadStopped(this);
            throw new InterruptedException();
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("[").append(getTaskId()).append(": ").append(getLayerName())
                .append(", ").append(getType()).append(", ").append(getState()).append("]")
                .toString();
    }
    
    public TaskStatus getStatus(){
       return new TaskStatus(
                System.currentTimeMillis(),
                getTilesDone(),
                getTilesTotal(),
                getTimeRemaining(),
                getTaskId(),
                getState()
                );
    }
}
