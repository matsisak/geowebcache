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
 * @author Marius Suta / The Open Planning Project 2008
 * @author Arne Kepp / The Open Planning Project 2009 
 */
package org.geowebcache.seed;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;
import org.geowebcache.storage.TileRange;

/**
 * A GWCTask for seeding/reseeding the cache.
 *
 */
public class SeedTask extends GWCTask {
    private static Log log = LogFactory.getLog(org.geowebcache.seed.SeedTask.class);

    //private StorageBroker storageBroker;

    private SeedJob parentSeedJob;

    /**
     * Constructs a SeedTask
     * @param sb
     * @param trIter
     * @param tl
     * @param reseed
     * @param doFilterUpdate
     */
    public SeedTask(long taskId, SeedJob job) {
        super(
                taskId, job, 
                job.isReseed() ? GWCTask.TYPE.RESEED: GWCTask.TYPE.SEED);
        this.parentSeedJob = job;

        super.state = GWCTask.STATE.READY;
    }
    
    @Override
    protected void doActionInternal() throws GeoWebCacheException, InterruptedException {
        super.state = GWCTask.STATE.RUNNING;

        // Lower the priority of the thread
        Thread.currentThread().setPriority(
                (java.lang.Thread.NORM_PRIORITY + java.lang.Thread.MIN_PRIORITY) / 2);

        checkInterrupted();

        // approximate thread creation time
        final long START_TIME = System.currentTimeMillis();

        final String layerName = parentJob.getLayer().getName();
        log.info(Thread.currentThread().getName() + " begins seeding layer : " + layerName);

        TileRange tr = parentJob.getRange();

        checkInterrupted();
        // TODO move to TileRange object, or distinguish between thread and task
        super.tilesTotal = tileCount(tr);

        final int metaTilingFactorX = parentJob.getLayer().getMetaTilingFactors()[0];
        final int metaTilingFactorY = parentJob.getLayer().getMetaTilingFactors()[1];

        final boolean tryCache = !parentSeedJob.isReseed();

        checkInterrupted();

        long seedCalls = 0;
        TileRequest request = null;
        while ((request = parentJob.getNextLocation()) != null && this.terminate == false) {

            checkInterrupted();
            Map<String, String> fullParameters = tr.getParameters();

            ConveyorTile tile = new ConveyorTile(parentSeedJob.getBreeder().getStorageBroker(), layerName, tr.getGridSetId(), request.getGridLoc(),
                    tr.getMimeType(), fullParameters, null, null);

            try {
                checkInterrupted();
                parentJob.getLayer().seedTile(tile, tryCache); // Try to seed
                
                if (log.isTraceEnabled()) {
                    log.trace(Thread.currentThread().getName() + " seeded " + request.toString());
                }
                
                // final long totalTilesCompleted = trIter.getTilesProcessed();
                // note: computing the # of tiles processed by this thread instead of by the whole group
                // also reduces thread contention as the trIter methods are synchronized and profiler
                // shows 16 threads block on synchronization about 40% the time
                final long tilesCompletedByThisThread = seedCalls * metaTilingFactorX
                        * metaTilingFactorY;

                updateStatusInfo(parentJob.getLayer(), tilesCompletedByThisThread, START_TIME);

                checkInterrupted();
                seedCalls++;
                
            } catch (Exception e) {
                parentSeedJob.failure(this, request, e); // Handle Failure
            }
            
        } // Iterate over tile requests.

        if (this.terminate) {
            log.info("Job on " + Thread.currentThread().getName() + " was terminated after "
                    + this.tilesDone + " tiles");
        } else {
            log.info(Thread.currentThread().getName() + " completed (re)seeding layer " + layerName
                    + " after " + this.tilesDone + " tiles and " + this.timeSpent + " seconds.");
        }

        checkInterrupted();
        
        // TODO:  This seems to be waiting for the first thread that started running?  
        //        Wouldn't the last to stop be more appropriate
        //        Move to the finished handler in the job?
        //if (threadOffset == 0 && doFilterUpdate) {
        //    runFilterUpdates(tr.getGridSetId());
        //}

        super.state = GWCTask.STATE.DONE;
    }

    /**
     * helper for counting the number of tiles
     * 
     * @param tr
     * @return -1 if too many
     */
    private long tileCount(TileRange tr) {

        final int startZoom = tr.getZoomStart();
        final int stopZoom = tr.getZoomStop();

        long count = 0;

        for (int z = startZoom; z <= stopZoom; z++) {
            long[] gridBounds = tr.rangeBounds(z);

            final long minx = gridBounds[0];
            final long maxx = gridBounds[2];
            final long miny = gridBounds[1];
            final long maxy = gridBounds[3];

            long thisLevel = (1 + maxx - minx) * (1 + maxy - miny);

            if (thisLevel > (Long.MAX_VALUE / 4) && z != stopZoom) {
                return -1;
            } else {
                count += thisLevel;
            }
        }

        return count;
    }

    /**
     * Helper method to update the members tracking thread progress.
     * 
     * @param layer
     * @param zoomStart
     * @param zoomStop
     * @param level
     * @param gridBounds
     * @return
     */
    private void updateStatusInfo(TileLayer layer, long tilesCount, long start_time) {

        // working on tile
        this.tilesDone = tilesCount;

        // estimated time of completion in seconds, use a moving average over the last
        this.timeSpent = (int) (System.currentTimeMillis() - start_time) / 1000;

        long threadCount = parentJob.getThreadCount();
        long timeTotal = Math.round((double) timeSpent
                * (((double) tilesTotal / threadCount) / (double) tilesCount));

        this.timeRemaining = (int) (timeTotal - timeSpent);
    }

    @Override
    protected void dispose() {
        if (parentJob.getLayer() instanceof WMSLayer) {
            ((WMSLayer) parentJob.getLayer()).cleanUpThreadLocals();
        }
    }
}
