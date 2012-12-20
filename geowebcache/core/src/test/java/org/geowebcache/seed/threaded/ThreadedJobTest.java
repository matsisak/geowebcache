package org.geowebcache.seed.threaded;

import static org.easymock.classextension.EasyMock.*;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.seed.AbstractJobTest;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.seed.Job;

import org.geowebcache.storage.TileRangeIterator;


public class ThreadedJobTest extends AbstractJobTest {

    /**
     * Create a ThreadedJob with a set of mock tasks that pretend to be in the given states.
     * @param states
     * @return
     */
    @Override
    protected Job jobWithTaskStates(STATE... states) {
        ThreadedTileBreeder breeder = createMock(ThreadedTileBreeder.class);
        replay(breeder);
        TileLayer tl = createMock(TileLayer.class);
        replay(tl);
        TileRangeIterator tri = createMock(TileRangeIterator.class);
        replay(tri);
        
        ThreadedJob job = new ThreadedJob(0, breeder, tl, states.length, tri, false) {
            
        };
        job.threads=new GWCTask[states.length];
        for(int i=0; i<states.length; i++){
            GWCTask task = createMock(GWCTask.class);
            expect(task.getState()).andReturn(states[i]).anyTimes();
            replay(task);
            job.threads[i] = task;
        }
        
        return job;
    }

}
