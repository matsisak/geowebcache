package org.geowebcache.seed.threaded;

import org.easymock.Capture;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMock;
import org.geowebcache.TestHelpers;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;
import org.geowebcache.layer.wms.WMSSourceHelper;
import org.geowebcache.seed.AbstractJobTest;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.seed.GWCTask.TYPE;
import org.geowebcache.seed.Job;
import org.geowebcache.seed.SeedRequest;
import org.geowebcache.seed.TruncateJob;
import org.geowebcache.seed.TruncateTask;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.TileRangeIterator;
import org.geowebcache.util.MockWMSSourceHelper;

import static org.easymock.classextension.EasyMock.*;
import static org.geowebcache.TestHelpers.createRequest;

public class ThreadedTruncateJobTest extends AbstractJobTest {

private ThreadedTileBreeder makeMockBreeder(TruncateTask mockTask) {
    final Capture<TruncateJob> jobCap = new Capture<TruncateJob>();
    ThreadedTileBreeder breeder = createMock(ThreadedTileBreeder.class);
    expect(breeder.createTruncateTask(capture(jobCap))).andReturn(mockTask);
    expect(mockTask.getJob()).andStubAnswer(new IAnswer<Job>(){

        public Job answer() throws Throwable {
            return jobCap.getValue();
        }});
    return breeder;
}

@Override
protected Job initNextLocation(TileRangeIterator tri) {
    final TruncateTask task = createMock(TruncateTask.class);
    final ThreadedTileBreeder breeder = makeMockBreeder(task);
    replay(task);
    replay(breeder);
    
    TileLayer tl = createMock(TileLayer.class);
    replay(tl);
    
    ThreadedTruncateJob job = new ThreadedTruncateJob(1, breeder, tri, tl, false);
    
    job.threads[0] = task;

    return job;
}

@Override
protected Job jobWithTaskStates(STATE... states) {
    final TruncateTask task = createMock(TruncateTask.class);
    final ThreadedTileBreeder breeder = makeMockBreeder(task);
    expect(task.getState()).andReturn(states[0]).anyTimes();
    replay(task);
    replay(breeder);
    
    TileLayer tl = createMock(TileLayer.class);
    replay(tl);
    TileRangeIterator tri = createMock(TileRangeIterator.class);
    replay(tri);
    
    ThreadedTruncateJob job = new ThreadedTruncateJob(1, breeder, tri, tl, false);
    
    return job;

}

@Override
public void testGetStateDone() throws Exception {
    // Only single task case needs to be tested 
}

@Override
public void testGetStateReady() throws Exception {
    // Only single task case needs to be tested 
}

@Override
public void testGetStateRunning() throws Exception {
    // Only single task case needs to be tested 
}

@Override
public void testGetStateDead() throws Exception {
    // Only single task case needs to be tested 
}

@Override
public void testGetStateUnset() throws Exception {
    // Only single task case needs to be tested 
}

/**
 * Make sure that the correct calls are made when Truncating
 * 
 * @throws Exception
 */
public void testSeedStoredTiles() throws Exception {

    StorageBroker storageBroker;
    WMSLayer tl;

    tl = TestHelpers.createWMSLayer("image/png");

    final String gridSetId = tl.getGridSubsets().iterator().next();
    final int zoomLevel = 3;
    SeedRequest req = createRequest(tl, TYPE.TRUNCATE, zoomLevel, zoomLevel);

    final GridSubset gridSubset = tl.getGridSubset(gridSetId);

    storageBroker = createMock(StorageBroker.class);
    
    TileRange tr = ThreadedTileBreeder.createTileRange(req, tl);
    TileRangeIterator trIter = new TileRangeIterator(tr, tl.getMetaTilingFactors());

    
    final TruncateTask task = createMock(TruncateTask.class);
    final ThreadedTileBreeder breeder = makeMockBreeder(task);
    expect(task.getState()).andStubReturn(STATE.READY);
    expectDoActionInternal(task);
    expectDispose(task);
    replay(task);
    replay(breeder);
    replay(storageBroker);
  

    ThreadedTruncateJob job = new ThreadedTruncateJob(1,breeder, trIter, tl, false);
    assertSame(job.getTasks()[0], task);
    
    job.runSynchronously();

    verify(storageBroker);
}


}
