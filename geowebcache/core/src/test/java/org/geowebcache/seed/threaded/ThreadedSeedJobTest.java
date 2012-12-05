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
 * @author Gabriel Roldan, OpenGeo, Copyright 2010
 */
package org.geowebcache.seed.threaded;

import static org.easymock.classextension.EasyMock.*;

import static org.geowebcache.TestHelpers.createFakeSourceImage;
import static org.geowebcache.TestHelpers.createWMSLayer;
import static org.geowebcache.TestHelpers.createRequest;

import static org.geowebcache.storage.TileObjectMatcher.tileObjectAt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.easymock.Capture;
import org.easymock.classextension.EasyMock;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.TestHelpers;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileResponseReceiver;
import org.geowebcache.layer.wms.WMSLayer;
import org.geowebcache.layer.wms.WMSMetaTile;
import org.geowebcache.layer.wms.WMSSourceHelper;
import org.geowebcache.seed.SeedRequest;
import org.geowebcache.seed.SeedTask;
import org.geowebcache.seed.GWCTask.TYPE;
import org.geowebcache.seed.threaded.ThreadedSeedJob;
import org.geowebcache.seed.threaded.ThreadedTileBreeder;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.TileRangeIterator;
import org.geowebcache.util.MockWMSSourceHelper;

/**
 * Unit test suite for {@link SeedTask}
 * 
 * @author Gabriel Roldan (OpenGeo)
 * @version $Id$
 */
public class ThreadedSeedJobTest extends TestCase {

    ThreadedTileBreeder breeder;
    StorageBroker storageBroker;
    WMSLayer tl;
    byte[] fakeWMSResponse;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        storageBroker = EasyMock.createMock(StorageBroker.class);
        expect(storageBroker.put((TileObject) anyObject())).andReturn(true).anyTimes();
        expect(storageBroker.get((TileObject) anyObject())).andReturn(false).anyTimes();
        replay(storageBroker);

        breeder = EasyMock.createMock(ThreadedTileBreeder.class);
        expect(breeder.getStorageBroker()).andReturn(storageBroker).anyTimes();
        replay(breeder);

        tl = createWMSLayer("image/png");
        fakeWMSResponse = createFakeSourceImage(tl);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * For a metatiled seed request over a given zoom level, make sure the correct wms calls are
     * issued
     * 
     * @throws Exception
     */
    @SuppressWarnings("serial")
    public void testSeedWMSRequests() throws Exception {

        // WMSSourceHelper that on makeRequest() returns always the saqme fake image
        WMSSourceHelper mockSourceHelper = EasyMock.createMock(WMSSourceHelper.class);


        Capture<Resource> resourceCapturer = new Capture<Resource>() {
            @Override
            public void setValue(Resource target) {
                try {
                    target.transferFrom(Channels.newChannel(new ByteArrayInputStream(
                            fakeWMSResponse)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        mockSourceHelper.makeRequest(EasyMock.<WMSMetaTile> anyObject(), capture(resourceCapturer));
        expectLastCall().times(3);
        
        mockSourceHelper.setConcurrency(32);
        mockSourceHelper.setBackendTimeout(120);
        replay(mockSourceHelper);

        tl.setSourceHelper(mockSourceHelper);

        final int zoomLevel = 4;
        SeedRequest req = createRequest(tl, TYPE.SEED, zoomLevel, zoomLevel);

        TileRange tr = ThreadedTileBreeder.createTileRange(req, tl);
        TileRangeIterator trIter = new TileRangeIterator(tr, tl.getMetaTilingFactors());

        boolean reseed = false;
        
        ThreadedSeedJob job = new ThreadedSeedJob(1,1, breeder, reseed, trIter, tl, 1,1,4, false);
        SeedTask seedTask = (SeedTask) job.getTasks()[0];
        
        seedTask.setTaskId(1L);
        seedTask.setThreadInfo(new AtomicInteger(), 0);
        /*
         * HACK: avoid SeedTask.getCurrentThreadArrayIndex failure.
         */
        Thread.currentThread().setName("pool-fake-thread-1");

        /*
         * Call the seed process
         */
        seedTask.doAction();

        verify(mockSourceHelper);
    }

    /**
     * For a metatiled seed request over a given zoom level, make sure the correct wms calls are
     * issued
     * 
     * @throws Exception
     */
    public void testSeedRetries() throws Exception {

        // WMSSourceHelper that on makeRequest() returns always the saqme fake image
        // WMSSourceHelper mockSourceHelper = new MockWMSSourceHelper();///
        // EasyMock.createMock(WMSSourceHelper.class);
        WMSSourceHelper mockSourceHelper = new MockWMSSourceHelper() {
            private int numCalls;

            @Override
            protected void makeRequest(TileResponseReceiver tileRespRecv, WMSLayer layer,
                    Map<String, String> wmsParams, String expectedMimeType, Resource target)
                    throws GeoWebCacheException {
                numCalls++;
                switch (numCalls) {
                case 1:
                    throw new GeoWebCacheException("test exception");
                case 2:
                    throw new RuntimeException("test unexpected exception");
                case 3:
                    throw new GeoWebCacheException("second test exception");
                case 4:
                    throw new RuntimeException("second test unexpected exception");
                default:
                    try {
                        target.transferFrom(Channels.newChannel(new ByteArrayInputStream(
                                fakeWMSResponse)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        tl.setSourceHelper(mockSourceHelper);

        final int zoomLevel = 4;
        SeedRequest req = createRequest(tl, TYPE.SEED, zoomLevel, zoomLevel);

        TileRange tr = ThreadedTileBreeder.createTileRange(req, tl);
        TileRangeIterator trIter = new TileRangeIterator(tr, tl.getMetaTilingFactors());

        final boolean reseed = false;
        final long totalFailuresBeforeAborting = 4;
        ThreadedSeedJob job = new ThreadedSeedJob(1,1, breeder, reseed, trIter, tl, 1,10,totalFailuresBeforeAborting, false);
        SeedTask seedTask = (SeedTask) job.getTasks()[0];


        /*
         * HACK: avoid SeedTask.getCurrentThreadArrayIndex failure.
         */
        Thread.currentThread().setName("pool-fake-thread-1");

        /*
         * Call the seed process
         */
        seedTask.doAction();
        assertEquals(totalFailuresBeforeAborting, job.getFailures());
    }

    long cellsInSubset(TileLayer tl, String gridSetId, int zoomLevel){
        final GridSubset gridSubset = tl.getGridSubset(gridSetId);

        final long[] coveredGridLevels = gridSubset.getCoverage(zoomLevel);

        // seeding should not include edge tiles produced by the meta tiling that don't fall into
        // the gridsubset's coverage
        final long starty = coveredGridLevels[1];
        final long startx = coveredGridLevels[0];
        final long endy = coveredGridLevels[3];
        final long endx = coveredGridLevels[2];

        final long expectedSavedTileCount = (endx - startx + 1)
                * (endy - starty + 1);
        
        return expectedSavedTileCount;
    }
    
    /**
     * Make sure when seeding a given zoom level, the correct tiles are sent to the
     * {@link StorageBroker}
     * 
     * @throws Exception
     */
    public void testSeedStoredTiles() throws Exception {


        // WMSSourceHelper that on makeRequest() returns always the saqme fake image
        WMSSourceHelper mockSourceHelper = new MockWMSSourceHelper();// EasyMock.createMock(WMSSourceHelper.class);
        // expect(mockSourceHelper.makeRequest((WMSMetaTile)
        // anyObject())).andReturn(fakeWMSResponse)
        // .anyTimes();
        // replay(mockSourceHelper);
        tl.setSourceHelper(mockSourceHelper);

        final String gridSetId = tl.getGridSubsets().iterator().next();
        final int zoomLevel = 3;
        SeedRequest req = createRequest(tl, TYPE.SEED, zoomLevel, zoomLevel);

        final GridSubset gridSubset = tl.getGridSubset(gridSetId);

        final long[] coveredGridLevels = gridSubset.getCoverage(zoomLevel);

        // seeding should not include edge tiles produced by the meta tiling that don't fall into
        // the gridsubset's coverage
        final long starty = coveredGridLevels[1];
        final long startx = coveredGridLevels[0];
        final long endy = coveredGridLevels[3];
        final long endx = coveredGridLevels[2];

        /*
         * Create a mock storage broker that has never an image in its blob store and that captures
         * the TileObject the seeder requests it to store for further test validation
         */
        storageBroker = EasyMock.createMock(StorageBroker.class);
        
        // Each tile in the subset should be put in storage once, the order doesn't matter.
        
        for (long x = startx; x <= endx; x++) {
            for (long y = starty; y <= endy; y++) {
                expect(storageBroker.put(tileObjectAt(x,y,zoomLevel))).andReturn(true).once();
            }
        }
        
        expect(storageBroker.get((TileObject) anyObject())).andReturn(false).anyTimes();
        replay(storageBroker);
        
        breeder = EasyMock.createMock(ThreadedTileBreeder.class);
        expect(breeder.getStorageBroker()).andReturn(storageBroker).anyTimes();
        replay(breeder);

        TileRange tr = ThreadedTileBreeder.createTileRange(req, tl);
        TileRangeIterator trIter = new TileRangeIterator(tr, tl.getMetaTilingFactors());

        boolean reseed = false;
        ThreadedSeedJob job = new ThreadedSeedJob(1,1, breeder, reseed, trIter, tl, 1,1,4, false);
        SeedTask task = (SeedTask) job.getTasks()[0];
        task.setTaskId(1L);

        /*
         * HACK: avoid SeedTask.getCurrentThreadArrayIndex failure.
         */
        Thread.currentThread().setName("pool-fake-thread-1");

        /*
         * Call the seed process
         */
        task.doAction();

        verify(storageBroker);
    }

}
