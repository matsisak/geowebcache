package org.geowebcache.util;

import java.util.Arrays;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.geowebcache.conveyor.ConveyorTile;

public class ConveyorTileMatcher implements IArgumentMatcher {
    final long[] xyz;
    
    /**
     * Matches a ConveyorTile at a particular grid location.
     * @param x
     * @param y
     * @param zoom
     * @return
     */
    public static ConveyorTile conveyorAt(long x, long y, long zoom) {
        EasyMock.reportMatcher(new ConveyorTileMatcher(x,y,zoom));
        return null;
    }

    
    public ConveyorTileMatcher(long x, long y, long zoom) {
        super();
        this.xyz = new long[]{x,y,zoom};
    }

    public boolean matches(Object argument) {
        if (!(argument instanceof ConveyorTile)) {
            return false;
        }
        ConveyorTile conv = (ConveyorTile) argument;
        return Arrays.equals(xyz, conv.getTileIndex());
    }
    
    public void appendTo(StringBuffer buffer) {
        buffer.append("conveyorAt(");
        buffer.append(Arrays.toString(xyz));
        buffer.append(")");

    }

}
