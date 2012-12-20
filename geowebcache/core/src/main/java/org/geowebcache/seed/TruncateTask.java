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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheException;

public class TruncateTask extends GWCTask {
    private static Log log = LogFactory.getLog(TruncateTask.class);


    public TruncateTask(long taskId, TruncateJob job) {
        super(taskId, job, GWCTask.TYPE.TRUNCATE);
        this.state=STATE.READY;
    }

    @Override
    protected void doActionInternal() throws GeoWebCacheException, InterruptedException {
        super.state = GWCTask.STATE.RUNNING;
        checkInterrupted();
        try {
            parentJob.getBreeder().getStorageBroker().delete(parentJob.getRange());
        } catch (Exception e) {
            e.printStackTrace();
            super.state = GWCTask.STATE.DEAD;
            log.error("During truncate request: " + e.getMessage());
        }

        checkInterrupted();

        if (super.state != GWCTask.STATE.DEAD) {
            super.state = GWCTask.STATE.DONE;
            log.debug("Completed truncate request.");
        }
    }



    @Override
    protected void dispose() {
        // do nothing
    }

}
