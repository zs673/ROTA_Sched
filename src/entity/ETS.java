package entity;

import java.util.ArrayList;
import java.util.List;
/**
 * ETS
 * 1. start: 开始时间
 * 2. budget: ETS 最多能运行的时长，为end+alph-start
 * 3. end： 结束时间，即下一个ETS的开始时间，并非到达end就需要结束，有一定的容错
 * 4. last：ETS中最后一个任务的结束时间
 * 5. alph： ETS的容错程度
 * 6. allocatableJobs：分配在该ETS的job
 * 7. Jobinrange：可能会分配在该ETS的job
 * 8. needmoveagain：需要再次移动的 job
 * 9. sofjob: ETS 中已经分配给job的空间
 * 10. residue： ETS 中剩余未分配的空间
 * 11. before： 前一个ETS中最后一个任务于该ETS中第一个任务中间所剩的空间
 * **/

public class ETS {
    public long start;
    public long budget;
    public long end;
    public long last;
    public  long alph;
    public List<Job> allocatableJobs= new ArrayList<>();
    public List<Job> Jobinrange= new ArrayList<>();
    public List<Job> needmoveagain=new ArrayList<>();
    public List<Space> sofjob= new ArrayList<>();
    public List<Space> residue= new ArrayList<>();
    public List<Space> before= new ArrayList<>();
    public boolean exact;

    public ETS(long start, long end, boolean exact){
        this.start=start;
        this.budget=end-start;
        this.end=end;
        this.exact=exact;
        this.last=start;
        this.alph=-1;
    }

    public String toString() {
        return "start: " + start + ", end: " + end + ", budget: " + budget;
    }
}
