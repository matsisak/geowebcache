package org.geowebcache.seed.threaded;

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
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.TileRangeIterator;
import org.geowebcache.util.MockWMSSourceHelper;

import static org.easymock.classextension.EasyMock.*;
import static org.geowebcache.TestHelpers.createRequest;

public class ThreadedTruncateJobTest extends AbstractJobTest {

@Override
protected Job initNextLocation(TileRangeIterator tri) {
    ThreadedTileBreeder breeder = createMock(ThreadedTileBreeder.class);
    replay(breeder);
    TileLayer tl = createMock(TileLayer.class);
    replay(tl);
    ThreadedTruncateJob job = new ThreadedTruncateJob(1, breeder, tri, tl, false);
    
    GWCTask task = createMock(GWCTask.class);
    expect(task.getJob()).andReturn(job).anyTimes();
    replay(task);
    job.threads[0] = task;

    return job;
}

@Override
protected Job jobWithTaskStates(STATE... states) {
    ThreadedTileBreeder breeder = createMock(ThreadedTileBreeder.class);
    replay(breeder);
    TileLayer tl = createMock(TileLayer.class);
    replay(tl);
    TileRangeIterator tri = createMock(TileRangeIterator.class);
    replay(tri);
    
    ThreadedTruncateJob job = new ThreadedTruncateJob(1, breeder, tri, tl, false);
    
    // Replace the thread with mock.
    GWCTask task = createMock(GWCTask.class);
    expect(task.getState()).andReturn(states[0]).anyTimes();
    expect(task.getJob()).andReturn(job).anyTimes();
    replay(task);
    job.threads[0] = task;

    
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

    ThreadedTileBreeder breeder;
    StorageBroker storageBroker;
    WMSLayer tl;

    tl = TestHelpers.createWMSLayer("image/png");

    final String gridSetId = tl.getGridSubsets().iterator().next();
    final int zoomLevel = 3;
    SeedRequest req = createRequest(tl, TYPE.TRUNCATE, zoomLevel, zoomLevel);

    final GridSubset gridSubset = tl.getGridSubset(gridSetId);

    final long[] coveredGridLevels = gridSubset.getCoverage(zoomLevel);

    storageBroker = createMock(StorageBroker.class);
    
    TileRange tr = ThreadedTileBreeder.createTileRange(req, tl);
    TileRangeIterator trIter = new TileRangeIterator(tr, tl.getMetaTilingFactors());
    
    // Delete should be called with the tile range once.
    expect(storageBroker.delete(tr)).andReturn(true).once();
    replay(storageBroker);
    
    breeder = createMock(ThreadedTileBreeder.class);
    expect(breeder.getStorageBroker()).andReturn(storageBroker).anyTimes();
    replay(breeder);

    ThreadedTruncateJob job = new ThreadedTruncateJob(1,breeder, trIter, tl, false);
    
    job.runSynchronously();

    verify(storageBroker);
}


}
