package org.geowebcache.seed;

import static org.easymock.classextension.EasyMock.*;

import java.lang.Thread.State;

import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.seed.Job;
import org.geowebcache.storage.TileRangeIterator;

import junit.framework.TestCase;

/**
 * Tests for common behaviour of classes implementing the Job interface.
 *
 */
public abstract class AbstractJobTest extends TestCase {

public AbstractJobTest() {
    super();
}

public AbstractJobTest(String name) {
    super(name);
}

/**
 * Return a Job with a single EasyMocked task and initialised with the provided TileRangeIterator
 * @param tri
 * @return
 */
protected abstract Job initNextLocation(TileRangeIterator tri);

/**
 * Assert that a TileRequest is at the specified grid location
 * @param tr TileRequest to compare
 * @param gridLoc expected grid location {x,y,zoom}
 */
public static void assertTileRequestAt(TileRequest tr, long[] gridLoc){
    assertTrue(String.format("Expected: TileRequest at <%d, %d, %d>, Result: TileRequest at <%d, %d, %d>", gridLoc[0], gridLoc[1], gridLoc[2], tr.getX(), tr.getY(), tr.getZoom()),
            tr.getX()==gridLoc[0] && tr.getY()==gridLoc[1] && tr.getZoom()==gridLoc[2]);
}

/**
 * Test that getNextRequest behaves as expected
 * @throws Exception
 */
public void testGetNextRequest() throws Exception {
    TileRangeIterator tri = createMock(TileRangeIterator.class);
    
    expect(tri.nextMetaGridLocation()).andReturn(new long[] {1,2,3});
    expect(tri.nextMetaGridLocation()).andReturn(new long[] {4,5,6});
    expect(tri.nextMetaGridLocation()).andReturn(null).anyTimes();
    replay(tri);
    
    Job job = initNextLocation(tri);
    
    TileRequest tr;
    tr = job.getNextLocation();
    assertTileRequestAt(tr, new long[] {1,2,3});
    tr = job.getNextLocation();
    assertTileRequestAt(tr, new long[] {4,5,6});
    tr = job.getNextLocation();
    assertNull(tr);
    tr = job.getNextLocation();
    assertNull(tr);
}

/**
 * Return a Job with EasyMock GWCTasks that report the given states when their getState methods are called.
 * @param states
 * @return
 */
protected abstract Job jobWithTaskStates(STATE... states);

/**
 * Assert that a job with tasks in the given states has the expected state.
 * @param expected The state expected of the job
 * @param states The states of the tasks in the job
 */
protected void assertGetState(STATE expected, STATE... states) {
    Job job = jobWithTaskStates(states);
    assertEquals(expected, job.getState());
}

/**
 * Test that getState gives correct results for a single task.
 * @throws Exception
 */
public void testGetStateSingle() throws Exception {
    
    assertGetState(STATE.READY, 
            STATE.READY);

    assertGetState(STATE.DEAD, 
            STATE.DEAD);

    assertGetState(STATE.RUNNING, 
            STATE.RUNNING);

    assertGetState(STATE.READY, 
            STATE.UNSET);

    assertGetState(STATE.DONE, 
            STATE.DONE);
}

/**
 * Test that getState returns DONE when expected.
 * @throws Exception
 */
public void testGetStateDone() throws Exception {
    assertGetState(STATE.DONE, 
            STATE.DONE, STATE.DONE);

    assertGetState(STATE.DONE, 
            STATE.DONE, STATE.READY);

    assertGetState(STATE.DONE, 
            STATE.READY, STATE.DONE);

    assertGetState(STATE.DONE, 
            STATE.DONE, STATE.UNSET);

    assertGetState(STATE.DONE, 
            STATE.UNSET, STATE.DONE);
    
}

/**
 * Test that getState returns READY when expected
 * @throws Exception
 */
public void testGetStateReady() throws Exception {
    assertGetState(STATE.READY, 
            STATE.READY, STATE.READY);

    assertGetState(STATE.READY, 
            STATE.UNSET, STATE.UNSET);

    assertGetState(STATE.READY, 
            STATE.READY, STATE.UNSET);

    assertGetState(STATE.READY, 
            STATE.UNSET, STATE.READY);
    
}

/**
 * Test that getState returns RUNNING when expected
 * @throws Exception
 */
public void testGetStateRunning() throws Exception {
    assertGetState(STATE.RUNNING, 
            STATE.RUNNING, STATE.RUNNING);

    assertGetState(STATE.RUNNING, 
            STATE.RUNNING, STATE.DONE);
    assertGetState(STATE.RUNNING, 
            STATE.RUNNING, STATE.UNSET);
    assertGetState(STATE.RUNNING, 
            STATE.RUNNING, STATE.READY);
    
}

/**
 * Test that getState returns DEAD when expected
 * @throws Exception
 */
public void testGetStateDead() throws Exception {
    assertGetState(STATE.DEAD, 
            STATE.DEAD, STATE.DEAD);

    assertGetState(STATE.DEAD, 
            STATE.DEAD, STATE.RUNNING);
    assertGetState(STATE.DEAD, 
            STATE.RUNNING, STATE.DEAD);
    
    assertGetState(STATE.DEAD, 
            STATE.DEAD, STATE.DONE);
    assertGetState(STATE.DEAD, 
            STATE.DONE, STATE.DEAD);
    
    assertGetState(STATE.DEAD, 
            STATE.DEAD, STATE.READY);
    assertGetState(STATE.DEAD, 
            STATE.READY, STATE.DEAD);
    
    assertGetState(STATE.DEAD, 
            STATE.DEAD, STATE.UNSET);
    assertGetState(STATE.DEAD, 
            STATE.UNSET, STATE.DEAD);
    
}

/**
 * Test that getState returns UNSET when expected
 * @throws Exception
 */
public void testGetStateUnset() throws Exception {
    // This is never expected
}

/**
 * Test the terminate method
 * @throws Exception
 */
public void testTerminate() throws Exception {
    Job job = jobWithTaskStates(STATE.DONE, STATE.RUNNING, STATE.RUNNING, STATE.UNSET, STATE.READY);
    
    for(GWCTask task: job.getTasks()){
        STATE s = task.getState();
        reset(task);
        
        expect(task.getState()).andReturn(s).anyTimes();
        if(s != STATE.DEAD && s != STATE.DONE){
            task.terminateNicely();
            expectLastCall().once();
        }
        replay(task);
    }
    
    job.terminate();
    
    for(GWCTask task: job.getTasks()){
        verify(task);
    }
}

}