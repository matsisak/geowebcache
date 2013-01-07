package org.geowebcache.util;

import java.util.Arrays;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.geowebcache.storage.TileObject;

public class TileObjectMatcher implements IArgumentMatcher {
    final long[] xyz;
    final String blobFormat;
    
    /**
     * Matches a ConveyorTile at a particular grid location.
     * @param x
     * @param y
     * @param zoom
     * @return
     */
    public static TileObject tileObjectAt(long x, long y, long zoom, String blobFormat) {
        EasyMock.reportMatcher(new TileObjectMatcher(x,y,zoom,blobFormat));
        return null;
    }

    
    public TileObjectMatcher(long x, long y, long zoom, String blobFormat) {
        super();
        this.xyz = new long[]{x,y,zoom};
        this.blobFormat = blobFormat;
    }

    public boolean matches(Object argument) {
        if (!(argument instanceof TileObject)) {
            return false;
        }
        TileObject to = (TileObject) argument;
        
        // Must be at same location
        if (!Arrays.equals(xyz, to.getXYZ())) return false;
        
        if (blobFormat==null){
            // Blob should be empty
            return to.getBlobFormat()==null && (to.getBlob()==null || to.getBlob().getSize()==0);
        } else {
            // Blob should be set
            return to.getBlobFormat()==blobFormat && to.getBlob()!=null && to.getBlob().getSize()>=0;
        }
        
    }
    
    public void appendTo(StringBuffer buffer) {
        buffer.append("tileObjectAt(");
        buffer.append(Arrays.toString(xyz));
        buffer.append(")");

    }

}
