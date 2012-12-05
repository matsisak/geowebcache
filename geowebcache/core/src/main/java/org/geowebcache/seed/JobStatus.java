package org.geowebcache.seed;

import java.util.Collection;
import java.util.Collections;

/**
 * Status of a seeding job at a particular time
 *
 */
public class JobStatus {

    final private Collection<TaskStatus> taskStatuses;
    final private long time;
    final private long jobId;
    
    
    
    public JobStatus(Collection<TaskStatus> taskStatuses, long time, long jobId) {
        super();
        this.taskStatuses = taskStatuses;
        this.time = time;
        this.jobId = jobId;
    }
    
    
    public Collection<TaskStatus> getTaskStatuses() {
        return Collections.unmodifiableCollection(taskStatuses);
    }
    public long getTime() {
        return time;
    }
    public long getJobId() {
        return jobId;
    }
    
    
}
