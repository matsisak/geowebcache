package org.geowebcache.storage;

import java.util.Arrays;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

public class TileObjectMatcher implements IArgumentMatcher {
    final long[] xyz;
    
    /**
     * Matches a TileObject at a particular grid location.
     * @param x
     * @param y
     * @param zoom
     * @return
     */
    public static TileObject tileObjectAt(long x, long y, long zoom) {
        EasyMock.reportMatcher(new TileObjectMatcher(x,y,zoom));
        return null;
    }

    
    public TileObjectMatcher(long x, long y, long zoom) {
        super();
        this.xyz = new long[]{x,y,zoom};
    }

    public boolean matches(Object argument) {
        if (!(argument instanceof TileObject)) {
            return false;
        }
        TileObject to = (TileObject) argument;
        return Arrays.equals(xyz, to.xyz);
    }
    
    public void appendTo(StringBuffer buffer) {
        buffer.append("tileObjectAt(");
        buffer.append(Arrays.toString(xyz));
        buffer.append(")");

    }

}
