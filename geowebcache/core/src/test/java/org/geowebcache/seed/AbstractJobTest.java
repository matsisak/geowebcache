package org.geowebcache.seed;

import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.seed.Job;

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
 * Return a Job with mock GWCTasks that report the given states when their getState methods are called.
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

}