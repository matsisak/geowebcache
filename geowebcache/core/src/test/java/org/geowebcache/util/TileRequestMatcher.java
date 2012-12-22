package org.geowebcache.util;

import java.util.Arrays;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.geowebcache.seed.TileRequest;

public class TileRequestMatcher implements IArgumentMatcher {
    final long[] xyz;
    
    /**
     * Matches a TileRequest at a particular grid location.
     * @param x
     * @param y
     * @param zoom
     * @return
     */
    public static TileRequest tileRequestAt(long x, long y, long zoom) {
        EasyMock.reportMatcher(new TileRequestMatcher(x,y,zoom));
        return null;
    }

    
    public TileRequestMatcher(long x, long y, long zoom) {
        super();
        this.xyz = new long[]{x,y,zoom};
    }

    public boolean matches(Object argument) {
        if (!(argument instanceof TileRequest)) {
            return false;
        }
        TileRequest tr = (TileRequest) argument;
        return Arrays.equals(xyz, tr.getGridLoc());
    }
    
    public void appendTo(StringBuffer buffer) {
        buffer.append("tileRequestAt(");
        buffer.append(Arrays.toString(xyz));
        buffer.append(")");

    }

}
