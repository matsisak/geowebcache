/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Arne Kepp / The Open Planning Project 2008 
 */
package org.geowebcache.rest;

import org.geowebcache.GeoWebCacheException;

public abstract class GWCTask {
    
    int threadCount = 1;
    
    int threadOffset = 0;
    
    abstract void doAction() throws GeoWebCacheException;

    void setThreadInfo(int threadCount, int threadOffset) {
        this.threadCount = threadCount;
        this.threadOffset = threadOffset;
    }
}