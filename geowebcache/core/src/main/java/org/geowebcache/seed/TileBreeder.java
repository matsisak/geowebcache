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
 * @author Marius Suta / The Open Planning Project 2008 (original code from SeedRestlet)
 * @author Arne Kepp / The Open Planning Project 2009 (original code from SeedRestlet)
 * @author Gabriel Roldan / OpenGeo 2010
 * @author Kevin Smith / OpenGeo 2012
 */

package org.geowebcache.seed;

import java.util.Iterator;

import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileRange;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Class in charge of dispatching seed/truncate tasks.
 * <p>
 * As of version 1.2.4a+, it is possible to control how GWC behaves in the event that a backend (WMS
 * for example) request fails during seeding, using the following environment variables:
 * <ul>
 * <li>{@code GWC_SEED_RETRY_COUNT}: specifies how many times to retry a failed request for each
 * tile being seeded. Use {@code 0} for no retries, or any higher number. Defaults to {@code 0}
 * retry meaning no retries are performed. It also means that the defaults to the other two
 * variables do not apply at least you specify a higher value for GWC_SEED_RETRY_COUNT;
 * <li>{@code GWC_SEED_RETRY_WAIT}: specifies how much to wait before each retry upon a failure to
 * seed a tile, in milliseconds. Defaults to {@code 100ms};
 * <li>{@code GWC_SEED_ABORT_LIMIT}: specifies the aggregated number of failures that a group of
 * seeding threads should reach before aborting the seeding operation as a whole. This value is
 * shared by all the threads launched as a single thread group; so if the value is {@code 10} and
 * you launch a seed task with four threads, when {@code 10} failures are reached by all or any of
 * those four threads the four threads will abort the seeding task. The default is {@code 1000}.
 * </ul>
 * These environment variables can be established by any of the following ways, in order of
 * precedence:
 * <ol>
 * <li>As a Java environment variable: for example {@code java -DGWC_SEED_RETRY_COUNT=5 ...};
 * <li>As a Servlet context parameter: for example
 * 
 * <pre>
 * <code>
 *   &lt;context-param&gt;
 *    &lt;!-- milliseconds between each retry upon a backend request failure --&gt;
 *    &lt;param-name&gt;GWC_SEED_RETRY_WAIT&lt;/param-name&gt;
 *    &lt;param-value&gt;500&lt;/param-value&gt;
 *   &lt;/context-param&gt;
 * </code>
 * </pre>
 * 
 * In the web application's {@code WEB-INF/web.xml} configuration file;
 * <li>As a System environment variable:
 * {@code export GWC_SEED_ABORT_LIMIT=2000; <your usual command to run GWC here>}
 * </ol>
 * </p>
 * 
 * @author Kevin Smith, based on Gabriel Roldan's class, now renamed {@link ThreadedTileBreeder}
 */
public interface TileBreeder extends ApplicationContextAware {

/**
 * Create and dispatch tasks to fulfil a seed request
 * 
 * @param layerName
 * @param sr
 * @throws GeoWebCacheException
 */
// TODO: The SeedRequest specifies a layer name. Would it make sense to use that instead of including one as a separate parameter?
public abstract void seed(String layerName, SeedRequest sr)
        throws GeoWebCacheException;

/**
 * Create tasks to manipulate the cache (Seed, truncate, etc)  They will still need to be dispatched.
 * 
 * @param tr The range of tiles to work on.
 * @param type The type of task(s) to create
 * @param threadCount The number of threads to use, forced to 1 if type is TRUNCATE
 * @param filterUpdate // TODO: What does this do?
 * @return Array of tasks.  Will have length threadCount or 1.
 * @throws GeoWebCacheException
 */
public abstract GWCTask[] createTasks(TileRange tr, GWCTask.TYPE type,
        int threadCount, boolean filterUpdate) throws GeoWebCacheException;

/**
 * Create tasks to manipulate the cache (Seed, truncate, etc).  They will still need to be dispatched.
 * 
 * @param tr The range of tiles to work on.
 * @param tl The layer to work on.  Overrides any layer specified on tr.
 * @param type The type of task(s) to create
 * @param threadCount The number of threads to use, forced to 1 if type is TRUNCATE
 * @param filterUpdate // TODO: What does this do?
 * @return Array of tasks.  Will have length threadCount or 1.
 * @throws GeoWebCacheException
 */
public abstract GWCTask[] createTasks(TileRange tr, TileLayer tl,
        GWCTask.TYPE type, int threadCount, boolean filterUpdate)
        throws GeoWebCacheException;

/**
 * Dispatches tasks 
 * 
 * @param tasks
 */
public abstract void dispatchTasks(GWCTask[] tasks);

/**
 * Method returns List of Strings representing the status of the currently running and scheduled
 * threads
 * 
 * @return array of {@code [[tilesDone, tilesTotal, tilesRemaining, taskID, taskStatus],...]}
 *         where {@code taskStatus} is one of:
 *         {@code 0 = PENDING, 1 = RUNNING, 2 = DONE, -1 = ABORTED}
 */
public abstract long[][] getStatusList();

/**
 * Method returns List of Strings representing the status of the currently running and scheduled
 * threads for a specific layer.
 * 
 * @return array of {@code [[tilesDone, tilesTotal, tilesRemaining, taskID, taskStatus],...]}
 *         where {@code taskStatus} is one of:
 *         {@code 0 = PENDING, 1 = RUNNING, 2 = DONE, -1 = ABORTED}
 * @param layerName the name of the layer.  null for all layers.
 * @return
 */
public abstract long[][] getStatusList(String layerName);

public abstract void setTileLayerDispatcher(
        TileLayerDispatcher tileLayerDispatcher);

public abstract void setThreadPoolExecutor(SeederThreadPoolExecutor stpe);

public abstract void setStorageBroker(StorageBroker sb);

public abstract StorageBroker getStorageBroker();

/**
 * Find a layer by name.
 * @param layerName
 * @return
 * @throws GeoWebCacheException
 */
public abstract TileLayer findTileLayer(String layerName)
        throws GeoWebCacheException;

/**
 * Get all tasks that are running
 * @return
 */
public abstract Iterator<GWCTask> getRunningTasks();

/**
 * Get all tasks that are running or waiting to run.
 * @return
 */
public abstract Iterator<GWCTask> getRunningAndPendingTasks();

/**
 * Get all tasks that are waiting to run.
 * @return
 */
public abstract Iterator<GWCTask> getPendingTasks();

/**
 * Terminate a running or pending task
 * @param id
 * @return
 */
public abstract boolean terminateGWCTask(long id);

/**
 * Get an iterator over the layers.
 * @return
 */
public abstract Iterable<TileLayer> getLayers();

}