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

import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.ImageMime;
import org.geowebcache.storage.TileRange;

import junit.framework.TestCase;
import static org.easymock.classextension.EasyMock.*;

import static org.geowebcache.util.ConveyorTileMatcher.conveyorAt;

public class SeedTaskTest extends TestCase {

    TileBreeder breeder;
    SeedJob job;
    SeedTask task;
    TileLayer layer;
    TileRange range;

    protected void stubJob(){
        expect(job.getBreeder()).andStubReturn(breeder);
        expect(job.isReseed()).andStubReturn(false);
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
    
    protected void setUp() throws Exception {
        super.setUp();
        
        breeder = createMock(TileBreeder.class);
        layer = createMock(TileLayer.class);
        range = createMock(TileRange.class);
        job = createMock(SeedJob.class);
        
        stubRange();
        replay(range);
        
        stubLayer();
        replay(layer);
        
        stubJob();
        replay(job);
        
        task = new SeedTask(0, job);
    }

    
    public void testSimple() throws Exception {
        
        reset(layer); {
            stubLayer();
            layer.seedTile(conveyorAt(1,2,3), eq(true));
            expectLastCall().once();
        } replay(layer);
        
        reset(job); {
            stubJob();
            
            job.threadStarted(task);
            expectLastCall().once();
            
            List<long[]> gridLocs = new LinkedList<long[]>();
            
            gridLocs.add(new long[] {1,2,3});
            
            for(long[] gridLoc: gridLocs){
                // Mock the TileRequest
                TileRequest treq = createMock(TileRequest.class);
                expect(treq.getGridLoc()).andStubReturn(gridLoc);
                expect(treq.getX()).andStubReturn(gridLoc[0]);
                expect(treq.getY()).andStubReturn(gridLoc[1]);
                expect(treq.getZoom()).andStubReturn(gridLoc[2]);
                replay(treq);
                
                // Return it once when asked.
                expect(job.getNextLocation()).andReturn(treq);
            }
            expect(job.getNextLocation()).andReturn(null).atLeastOnce();
            
            job.threadStopped(task);
            expectLastCall().once();
            
        } replay(job);
        
        task.doAction();
        
        verify(job);
        
    }
}
