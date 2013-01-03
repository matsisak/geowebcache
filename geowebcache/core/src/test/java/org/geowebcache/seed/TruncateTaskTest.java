/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geowebcache.seed;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.easymock.IAnswer;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.ImageMime;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileRange;

import junit.framework.TestCase;
import static org.easymock.classextension.EasyMock.*;
import static org.easymock.classextension.EasyMockSupport.*;

import static org.geowebcache.util.ConveyorTileMatcher.conveyorAt;
import static org.geowebcache.util.TileRequestMatcher.tileRequestAt;;

public class TruncateTaskTest extends TestCase {

    TileBreeder breeder;
    TruncateJob job;
    TruncateTask task;
    TileLayer layer;
    TileRange range;
    StorageBroker broker;

    protected void stubJob(){
        expect(job.getBreeder()).andStubReturn(breeder);
        expect(job.getLayer()).andStubReturn(layer);
        expect(job.getRange()).andStubReturn(range);
        expect(job.getThreadCount()).andStubReturn(1l);
    }
    protected void stubLayer(){
        expect(layer.getName()).andStubReturn("testLayer");
        expect(layer.getMetaTilingFactors()).andStubReturn(new int[] {1,1});
    }
    protected void stubRange(){
        expect(range.tileCount()).andStubReturn(10l);
        expect(range.getParameters()).andStubReturn(Collections.<String,String>emptyMap());
        expect(range.getGridSetId()).andStubReturn("testGridSet");
        expect(range.getMimeType()).andStubReturn(ImageMime.png);
    }
    protected void stubBreeder(){
        expect(breeder.getStorageBroker()).andStubReturn(broker);
    }
    
    protected void stubBroker(){
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        
        breeder = createMock(TileBreeder.class);
        layer = createMock(TileLayer.class);
        range = createMock(TileRange.class);
        job = createMock(TruncateJob.class);
        broker = createMock(StorageBroker.class);
        
        stubBreeder();
        replay(breeder);
        
        stubRange();
        replay(range);
        
        stubLayer();
        replay(layer);
        
        stubJob();
        replay(job);
        
        stubBroker();
        replay(broker);
        
        task = new TruncateTask(0, job);
    }

    
    public void testSimple() throws Exception {
        
        reset(broker); {
            stubBroker();
            expect(broker.delete(range)).andReturn(true).once();
        } replay(broker);
        
        reset(job); {
            stubJob();
            
            job.threadStarted(task);
            expectLastCall().once();
            
            job.threadStopped(task);
            expectLastCall().once();
            
        } replay(job);
        
        task.doAction();
        
        verify(job);
        verify(broker);
        
    }
    
    public void testFailure() throws Exception {
        
        Exception e = new RuntimeException("test");
        
        reset(broker); {
            stubBroker();
            expect(broker.delete(range)).andThrow(e);
        } replay(broker);
        
        reset(job); {
            stubJob();
            
            job.threadStarted(task);
            expectLastCall().once();
            
            job.threadStopped(task);
            expectLastCall().once();
            
        } replay(job);
        
        task.doAction();
        
        verify(job);
        verify(broker);
       
    }
}
