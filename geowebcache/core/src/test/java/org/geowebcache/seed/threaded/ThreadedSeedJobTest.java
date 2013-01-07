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
 * @author Kevin Smith, OpenGeo, Copyright 2012
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
import java.util.Map;

import org.easymock.Capture;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileResponseReceiver;
import org.geowebcache.layer.wms.WMSLayer;
import org.geowebcache.layer.wms.WMSMetaTile;
import org.geowebcache.layer.wms.WMSSourceHelper;
import org.geowebcache.seed.AbstractJobTest;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.seed.Job;
import org.geowebcache.seed.SeedJob;
import org.geowebcache.seed.SeedRequest;
import org.geowebcache.seed.SeedTask;
import org.geowebcache.seed.GWCTask.TYPE;
import org.geowebcache.seed.TileRequest;
import org.geowebcache.seed.threaded.ThreadedSeedJob;
import org.geowebcache.seed.threaded.ThreadedTileBreeder;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.TileRangeIterator;
import org.geowebcache.util.MockWMSSourceHelper;

/**
 * Unit test suite for {@link ThreadedSeedJob}
 * 
 */
public class ThreadedSeedJobTest extends AbstractJobTest {

    private ThreadedTileBreeder breeder;
    private StorageBroker storageBroker;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        storageBroker = EasyMock.createMock(StorageBroker.class);
        expect(storageBroker.put((TileObject) anyObject())).andReturn(true).anyTimes();
        expect(storageBroker.get((TileObject) anyObject())).andReturn(false).anyTimes();
        replay(storageBroker);

        breeder = EasyMock.createMock(ThreadedTileBreeder.class);
        expect(breeder.getStorageBroker()).andReturn(storageBroker).anyTimes();
        replay(breeder);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Check that failure and the retry queue work correctly.
     * 
     * Note, this test is depends on timing.  Using a debugger may alter its behaviour.
     * 
     * @throws Exception
     */
    public void testGetNextRequestWithRetry() throws Exception {
        TileRangeIterator tri = createMock(TileRangeIterator.class);
        
        expect(tri.nextMetaGridLocation()).andReturn(new long[] {1,2,3});
        expect(tri.nextMetaGridLocation()).andReturn(new long[] {4,5,6});
        expect(tri.nextMetaGridLocation()).andReturn(new long[] {7,8,9});
        expect(tri.nextMetaGridLocation()).andReturn(null).anyTimes();
        replay(tri);
        
        SeedJob job = (SeedJob) initNextLocation(tri);
        
        TileRequest tr;
        
        GWCTask task = job.getTasks()[0];
        
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {1,2,3});
        assertEquals(0, tr.getFailures());
        
        job.failure(task, tr, new RuntimeException("Just a test"));
        Thread.sleep(1500); // Make sure we give it time to get through the queue.
        
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {1,2,3});
        assertEquals(1, tr.getFailures());
        
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {4,5,6});
        assertEquals(0, tr.getFailures());
        
        job.failure(task, tr, new RuntimeException("Just a test"));
        // Don't sleep this time,
        
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {7,8,9}); // Failed tile shouldn't have made it through yet so it should get a new one
        assertEquals(0, tr.getFailures());
        
        
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {4,5,6}); // Call should block and then return the second failed tile.
        assertEquals(1, tr.getFailures());
        
        TileRequest trDone = job.getNextLocation();
        assertNull(trDone);
        
        job.failure(task, tr, new RuntimeException("Just a test")); // Fail again
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {4,5,6}); // Call should block and then return the second failed tile a third time
        assertEquals(2, tr.getFailures());
        
        trDone = job.getNextLocation();
        assertNull(trDone);
        assertEquals(3, job.getFailures()); // 1 on first failed tile, 2 on second.
        
        verify(tri);
    }
    
    /**
     * Check that repeated failures kill the job.
     * 
     * Note, this test is depends on timing.  Using a debugger may alter its behaviour.
     * 
     * @throws Exception
     */
    public void testGetNextRequestWithMaxRetry() throws Exception {
        TileRangeIterator tri = createMock(TileRangeIterator.class);
        
        expect(tri.nextMetaGridLocation()).andReturn(new long[] {1,2,3});
        expect(tri.nextMetaGridLocation()).andReturn(new long[] {4,5,6});
        expect(tri.nextMetaGridLocation()).andReturn(new long[] {7,8,9});
        expect(tri.nextMetaGridLocation()).andReturn(null).anyTimes();
        replay(tri);
        
        SeedJob job = (SeedJob) initNextLocation(tri);
        
        GWCTask task = job.getTasks()[0];

        reset(task); {
            expect(task.getJob()).andStubReturn(job);
            task.terminateNicely();
            expectLastCall().once();
            expect(task.getState()).andStubReturn(STATE.RUNNING);
        } replay(task);
        
        TileRequest tr;
        
        
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {1,2,3});
        assertEquals(0, tr.getFailures());
        
        job.failure(task, tr, new RuntimeException("Just a test"));
        Thread.sleep(1500); // Make sure we give it time to get through the queue.
        
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {1,2,3});
        assertEquals(1, tr.getFailures());
        
        job.failure(task, tr, new RuntimeException("Just a test"));
        Thread.sleep(1500); // Make sure we give it time to get through the queue.
        
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {1,2,3});
        assertEquals(2, tr.getFailures());
        
        job.failure(task, tr, new RuntimeException("Just a test"));
        Thread.sleep(1500); // Make sure we give it time to get through the queue.
        
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {4,5,6}); // Previous tile should have failed out.
        assertEquals(0, tr.getFailures());
        
        job.failure(task, tr, new RuntimeException("Just a test"));
        Thread.sleep(1500); // Make sure we give it time to get through the queue.
        
        tr = job.getNextLocation();
        assertTileRequestAt(tr, new long[] {4,5,6}); // Failed tile shouldn't have made it through yet so it should get a new one
        assertEquals(1, tr.getFailures());
        
        // This should kill the task
        job.failure(task, tr, new RuntimeException("Just a test"));
        
        job.threadStopped(task);
        
        verify(task);
    }

    /**
     * Create a mock task, and expect a call to the breeder's createSeedTask method returning the created task
     * @param mockBreeder an EasyMock mock of a TileBreeder in its record phase
     * @return a mock SeedTask in its record phase
     */
    private SeedTask createMockTask(ThreadedTileBreeder mockBreeder) {
        final SeedTask task = createMock(SeedTask.class);
        final Capture<SeedJob> jobCap = new Capture<SeedJob>();
        expect(mockBreeder.createSeedTask(capture(jobCap))).andReturn(task).once();
        expect(task.getJob()).andStubAnswer(new IAnswer<SeedJob>(){
            // The task should report that it belongs to the captured job
            public SeedJob answer() throws Throwable {
                return jobCap.getValue();
            }

        });
        return task;
    }
    
    @Override
    protected Job initNextLocation(TileRangeIterator tri) {
        ThreadedTileBreeder breeder = createMock(ThreadedTileBreeder.class);
        final SeedTask task = createMockTask(breeder);
        replay(task);
        replay(breeder);
        TileLayer tl = createMock(TileLayer.class);
        replay(tl);
        ThreadedSeedJob job = new ThreadedSeedJob(1,1, breeder, false, tri, tl, 3,750,5, false);

        return job;
    }

    @Override
    protected Job jobWithTaskStates(STATE... states) {
        ThreadedTileBreeder breeder = createMock(ThreadedTileBreeder.class);
        for(int i=0; i<states.length; i++){
            final SeedTask task = createMockTask(breeder);
            expect(task.getState()).andStubReturn(states[i]); // Have the task report itself as being in the desired state
            replay(task);
        }            
        replay(breeder);
        TileLayer tl = createMock(TileLayer.class);
        replay(tl);
        TileRangeIterator tri = createMock(TileRangeIterator.class);
        replay(tri);
        
        ThreadedSeedJob job = new ThreadedSeedJob(1,states.length, breeder, false, tri, tl, 1,1,4, false);
        
        return job;
    }

}
