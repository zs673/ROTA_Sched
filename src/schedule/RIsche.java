package schedule;

import java.security.PublicKey;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import entity.Space;
import entity.Job;
import entity.PeriodicTask;
import utils.AnalysisUtils;
import entity.ETS;
import entity.result;

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


public class RIsche {
    public List<Space> globalSpace = new ArrayList<>();
    public List<ETS> ETSs = new ArrayList<>();
    public result schedule(List<PeriodicTask> tasks) {
        int numOfExact = 0;
        double totalValue = 0;

        Pair<List<Job>, Long> pair = new AnalysisUtils().getJobsInHyperPeriod(tasks);

        List<Job> jobs = pair.getKey();
        long hyperperiod = pair.getValue();
    /****GET THE LIST OF JOB HAVE MOST VALUE ***/
        List<List<Job>> graphs = generateDependencyGraph(jobs);
        List<List<Job>> decomposedGraphs= decomposeGraphs(graphs);
    /*****************allocate the job*************/
        List<Job> allocatedJobs = Allocte(decomposedGraphs, hyperperiod);

        /*if allocate failed ,try allocte the fail job first*/
        if (allocatedJobs.size()<jobs.size() ) {
            ETSs.clear();
            globalSpace.clear();
            for(int i=0;i<jobs.size();i++)
            {
                jobs.get(i).startTime=-1;
                jobs.get(i).ETSInRange.clear();
                jobs.get(i).spaceInRange.clear();
                jobs.get(i).allocatableETS.clear();
                jobs.get(i).allocatableSpace.clear();
                jobs.get(i).ETSInRange0.clear();
            }
        /** change the decomposedGraphs,make the failed job to the exact job list **/
            decomposedGraphs=changeGraphs(decomposedGraphs,allocatedJobs);
            allocatedJobs = Allocte(decomposedGraphs, hyperperiod);
            if (allocatedJobs.size()<jobs.size() )
            {
                return null;
            }
        }

    /********** get numOfExact and totalValue ************/
        for (int i = 0; i < allocatedJobs.size(); i++) {
            Job j = allocatedJobs.get(i);
            assert (j.startTime >= 0);
            if (j.delta == j.startTime)
                numOfExact++;
            totalValue += new AnalysisUtils().getValue(j);

        }

        DecimalFormat df = new DecimalFormat("#.##");
        totalValue = (double) totalValue / (double) jobs.stream().mapToDouble(j -> j.task.Vmax).sum();

        double exact = Double.parseDouble(df.format((double) numOfExact / (double) jobs.size()));
        totalValue = Double.parseDouble(df.format(totalValue));

        /**
         * Check correctness
         */
        allocatedJobs.sort((j1, j2) -> Long.compare(j1.startTime, j2.startTime));

        assert (allocatedJobs.size() == jobs.size());
        for (int i = 0; i < allocatedJobs.size(); i++) {
            Job job = allocatedJobs.get(i);

            assert (job.startTime >= job.releaseTime);
            assert (job.startTime + job.task.WCET <= job.deadline);

            if (i == 0)
                assert (job.startTime + job.task.WCET <= allocatedJobs.get(1).startTime);
            else if (i == allocatedJobs.size() - 1)
                assert (job.startTime >= allocatedJobs.get(i - 1).startTime + allocatedJobs.get(i - 1).task.WCET);
            else {
                assert (job.startTime + job.task.WCET <= allocatedJobs.get(i + 1).startTime);
                assert (job.startTime >= allocatedJobs.get(i - 1).startTime + allocatedJobs.get(i - 1).task.WCET);
            }
        }

        List<List<Double>> pfs = new ArrayList<>();
        List<Double> pf = new ArrayList<>();
        pf.add(exact);
        pf.add(totalValue);
        pfs.add(pf);

        return new result(pfs,allocatedJobs,ETSs);

    }
    private   List<List<Job>>changeGraphs( List<List<Job>>decomposedGraphs,List<Job> failed)
    { /*get the most of fail job no correlation*/
        List<List<Job>> graphs = generateDependencyGraph(failed);
        List<List<Job>> decomposedGraphs0= decomposeGraphs(graphs);
        failed=decomposedGraphs0.get(0);

        /* adjust the exact job make the fail job be exact  */
        List<Job> allocated = new ArrayList<>(decomposedGraphs.get(0));
        List<Job> toAlloc= new ArrayList<>(decomposedGraphs.get(1));
        List<List<Job>> newdecomposedGraphs=new ArrayList<>();
        for(int k=0;k<failed.size();k++)
        {
            Job f = failed.get(k);
            toAlloc.remove(f);

            /*move out the job related to fail job*/
            for (int i = 0; i < allocated.size(); i++)
            {
                Job j = allocated.get(i);
                if (j.delta>=f.delta&& j.idealFinish <=f.idealFinish)  //j in i interference
                {allocated.remove(j);
                    toAlloc.add(j);
                    i--;
                    continue;
                }
                if (j.delta<=f.idealFinish&& j.idealFinish >=f.idealFinish)  //i in j interference
                {allocated.remove(j);
                    toAlloc.add(j);
                    i--;
                    continue;
                }
                if (j.delta>=f.delta&&j.delta <=f.idealFinish)  //j after i interference
                {allocated.remove(j);
                    toAlloc.add(j);
                    i--;
                    continue;
                }
                if(j.delta<=f.delta&&j.idealFinish>=f.delta)  //j before i interference
                {allocated.remove(j);
                    toAlloc.add(j);
                    i--;
                }
            }

        }
        allocated.addAll(failed);
        allocated.sort((s1,s2)->Long.compare(s1.delta,s2.delta));
        newdecomposedGraphs.add(allocated);
        newdecomposedGraphs.add(toAlloc);

        return newdecomposedGraphs;
    }

    /**************************** Allocte ****************************/
    private List<Job> Allocte(List<List<Job>> decomposedGraphs, long hyperperiod) {

        List<Job> allocated = new ArrayList<>(decomposedGraphs.get(0));         /* get the exact job list*/
        List<Job> toAlloc = new ArrayList<>(decomposedGraphs.get(1));           /* get the job list need to allocte*/

        /** distribute job to sever  **/
        ETSs = getFreeETS(allocated, toAlloc, hyperperiod);                     /* allocte the exact job list and generate the init ETS list*/
        fitAllocatableTasks(allocated, toAlloc, ETSs);

            /*allocte fail*/
        if (toAlloc.size() > 0)
            return toAlloc;

        /** Optimise the start time of jobs in value-based sever for improved I/O accuracy **/
        ETSs.sort((c1, c2) -> Long.compare(c1.start, c2.start));
        for (int i = 0; i < ETSs.size(); i++) {
            ETS ets = ETSs.get(i);
            if (ets.exact)
                continue;
            else if (ets.allocatableJobs.size() != 0)    // For value-based servers
                movejob(ets);
        }

        /** compute all sever's alph*/
        ETSs.sort((c1, c2) -> Long.compare(c1.start, c2.start));
        Getalph(hyperperiod);

        assert (allocated.size() == decomposedGraphs.get(0).size() + decomposedGraphs.get(1).size());
        return allocated;
    }

    private List<ETS> getFreeETS(List<Job> allocated, List<Job> toAllocate, long hyperperiod)
    {
        List<ETS> etss = new ArrayList<>();
        long startTime = 0;
        /*  initialize the startTime of exact job */
        for (int i = 0; i < allocated.size(); i++)
        {
            allocated.get(i).startTime = allocated.get(i).delta;
        }
        allocated.sort((j1, j2) -> Long.compare(j1.startTime, j2.startTime));

        /*sever creation*/

        for (int i = 0; i < allocated.size(); i++)
        {
            Job j = allocated.get(i);
            long startJ = j.delta;
            long endJ = j.idealFinish;

            if (startTime < startJ)
            {
                /*create value sever*/
                ETS s = new ETS(startTime, startJ, false);
                etss.add(s);
                Space sp1 = new Space(startTime, startJ);
                globalSpace.add(sp1);
                s.residue.add(sp1);
                /*create exact sever*/
                ETS s1 = new ETS(startJ, endJ, true);
                etss.add(s1);
                Space sp2 = new Space(startJ, endJ);
                s1.sofjob.add(sp2);
                globalSpace.add(sp2);
                s1.allocatableJobs.add(j);
                j.allocatableETS.add(s1);
                j.allocatableSpace.add(sp2);
                sp2.allocatableJobs.add(j);
                startTime = endJ;
            }
            else if (startTime == startJ)
            {
                /*create exact sever and join exact job*/
                ETS s2 = new ETS(startJ, endJ, true);
                etss.add(s2);
                Space sp3 = new Space(startJ, endJ);
                s2.sofjob.add(sp3);
                globalSpace.add(sp3);
                s2.allocatableJobs.add(j);
                j.allocatableETS.add(s2);
                j.allocatableSpace.add(sp3);
                sp3.allocatableJobs.add(j);
                startTime = endJ;
            }
            else{
                System.out.println("time error");
                System.exit(-1);
            }
        }

        /*use the last space*/
        if (startTime < hyperperiod)
        {
            ETS s = new ETS(startTime, hyperperiod, false);
            Space sp1 = new Space(startTime, hyperperiod);
            globalSpace.add(sp1);
            s.residue.add(sp1);
            etss.add(s);
            startTime = hyperperiod;
        }

        /* consider which job the value sever can join*/
        for (int i = 0; i < etss.size(); i++)
        {
            ETS ets = etss.get(i);
            if (ets.exact)
            {
                continue;
            }
            Space s=ets.residue.get(0);
            for (int j = 0; j < toAllocate.size(); j++)
            {
                Job job = toAllocate.get(j);
                boolean m=mayjion(s,job);
                if (m)
                {
                    ets.Jobinrange.add(job);
                    job.ETSInRange.add(ets);
                    job.ETSInRange0.add(ets);
                }
            }
        }

        return etss;
    }

    private void fitAllocatableTasks(List<Job> allocated, List<Job> toAlloc, List<ETS> etss)
    {
        // etss -> should be value-based server only.
        etss.sort((s1, s2) -> compareETS(s1, s2));
        for (int i = 0; i < etss.size(); i++)
        {
            ETS ets = etss.get(i);
            ets.Jobinrange.sort((j1, j2) -> comparatorForCandP(j1, j2));

            /* consider all job that can be allocte to the sever,and chose the feasible job join the sever  */
            for (int k = 0; k < ets.Jobinrange.size(); k++)
            {
                ets.Jobinrange.sort((j1, j2) -> comparatorForCandP(j1, j2));
                Job j = ets.Jobinrange.get(k);

                /*job don`t been alloted to other sever*/
                if (j.allocatableETS.size() == 0)
                {   /*join the job to sever*/
                    boolean m = joinjob(ets, j);
                    /*judge the success of join job*/
                    if (m)
                    {
                        allocated.add(j);
                        toAlloc.remove(j);
                    }else {
                        j.ETSInRange.remove(ets);
                        ets.Jobinrange.remove(j);
                    }
                    k--;
                }
            }
        }

        /*consider all job move to a better ets*/
        for (int i = 0; i < etss.size(); i++)
        {
            ETS ets = etss.get(i);
            for (int k = 0; k < ets.allocatableJobs.size(); k++)
            {
                List<Job> jobs = ets.allocatableJobs;
                jobs.sort((j1, j2) -> Long.compare(j1.delta,j2.delta));
                Job j = jobs.get(k);
                int num=betterest(j);    //find the best est`s num in List
                if(num==-1)
                    continue;
                move_ets(j,ets,j.ETSInRange0.get(num));
                k--;
            }
        }
    }


    /* Secondary function for allocte*/
    private int compareETS(ETS s1, ETS s2)
    {
        int result = Long.compare(s1.start, s2.start);

        if (result == 0)
        {
            System.out.print("ETS error 1 ");
            // System.exit(-1);
            return Long.compare(s1.budget, s2.budget);
        }
        else
            return result;
    }

    private int comparatorForCandP(Job j1, Job j2)
    {
        /*  compare job by the number of sever that the job may join,then by the deadline  */
        int result = Integer.compare(j1.ETSInRange.size(), j2.ETSInRange.size());
        if (result == 0)
        {
            return -Long.compare(j1.task.WCET, j2.task.WCET);
        } else
            return result;
    }

    private int betterest(Job job)
    {
        /**
         * 遍历job中ETSInRange（可分配的到的ETS）中，看是否存在一个能获得更高Value的ETS
         * **/
        Long start = job.startTime;
        job.startTime=findstrat(job.allocatableETS.get(0),job);
        if(job.startTime==0L)
            job.startTime=start;
        List<ETS> consider = job.ETSInRange0;
        /*compute the value in the ets that job in*/
        Long capacity = Capacity(job,job.allocatableETS.get(0));
        double max1 = maxvalue(job, capacity);
        double max2 = 0;
        int num1=job.allocatableETS.get(0).allocatableJobs.size();

        int k=-1;   //about value
        int n=-1;   //about num of job

        for (int i = 0; i < consider.size(); i++)
        {

            job.startTime=findstrat(consider.get(i),job); //find the start in the may join ets
            if(job.startTime==0L)
            {
                job.startTime=start;
                continue;
            }

            /*consider num of job in may join ets*/
            if(consider.get(i).allocatableJobs.size()+1<num1-1)
            {
                num1=consider.get(i).allocatableJobs.size();
                n=i;
            }

            /*consider value of job in may join ets*/
            capacity = Capacity(job, consider.get(i));
            max2 = maxvalue(job, capacity);
            if (max2 > max1)
            {
                max1=max2;
                k=i;
            }
            job.startTime=start;
        }
        if(k==-1)
            return n;
        return k;
    }

    private boolean allocJob(Job job, List<Job> allocated, List<Job> toAlloc, List<Space> spaces) {
        allocated.sort((j1, j2) -> Long.compare(j1.startTime, j2.startTime));
        spaces.sort((j1,j2)->Long.compare(j1.start,j2.start));
        /*
         * Get spaces in range
         */

        // if(spaces.size()!=0&&spaces.get(0).start==71)
        //  System.out.print("jion error X ");

        long release = job.releaseTime;
        long deadline = job.deadline;

        for (int j = 0; j < spaces.size(); j++) {
            Space s = spaces.get(j);
            long start = s.start;
            long end = s.end;

            if (release >= end || deadline <= start) {
            } else
                job.spaceInRange.add(s);
        }

        if (job.spaceInRange.size() == 0)
            return false;

        /*
         * Check whether spaces is able to fit the job
         */
        long startTime = Math.max(job.spaceInRange.get(0).start, job.releaseTime);
        long endTime = Math.min(job.spaceInRange.get(job.spaceInRange.size() - 1).end, job.deadline);

        long sumSpace = 0;
        for (int j = 0; j < job.spaceInRange.size(); j++) {
            Space s = job.spaceInRange.get(j);
            if (j == 0)
                sumSpace += s.end - startTime;
            else if (j == job.spaceInRange.size() - 1)
                sumSpace += endTime - s.start;
            else
                sumSpace += s.end - s.start;
        }

        if (sumSpace < job.task.WCET)
            return false;

        /*
         * Get the solution with minimized impact to timing-accurate task
         */
        List<Space> solution = getSpacesForAllocation(job, allocated, spaces);
        if (solution == null)
            return false;

        // TODO: update GA side

        /*
         * Allocate Job by shifting the allocated jobs.
         */
        job.startTime = Math.max(solution.get(0).start, job.releaseTime);

        long requiredSpace = job.task.WCET;

        List<Job> shifedJobs = new ArrayList<>();

        for (int i = 0; i < solution.size() - 1; i++) {
            Space s1 = solution.get(i);
            Space s2 = solution.get(i + 1);

            requiredSpace -= Math.min(solution.get(i).end, job.deadline) - Math.max(solution.get(i).start, job.releaseTime);

            List<Job> jobsInBetween = getJobsInBtween(s1, s2, allocated);
            for (int k = 0; k < jobsInBetween.size(); k++) {
                Job shiftedJob = jobsInBetween.get(k);
                if (!shifedJobs.contains(shiftedJob)) {
                    shiftedJob.startTime += requiredSpace;
                    shifedJobs.add(shiftedJob);
                }

            }
        }

        /*
         * Update task
         */
        toAlloc.remove(job);
        allocated.add(job);

        /*
         * Update space
         */

        Space first = solution.get(0);

        long totalNeed = job.task.WCET - (first.end - job.startTime);

        if (first.start < job.startTime) {
            first.end = job.startTime;
            first.capcity = first.end - first.start;
        } else
        { spaces.remove(first);
            globalSpace.remove(first);}



        for (int i = 1; i < solution.size() - 1; i++) {
            totalNeed -= solution.get(i).capcity;
            spaces.remove(solution.get(i));
            globalSpace.remove(solution.get(i));
        }

        Space lastS = solution.get(solution.size() - 1);

//		long restRequiredSpace = job.task.WCET - solution.stream().mapToLong(s -> s.capcity).sum() + lastS.capcity;

        if (totalNeed == lastS.capcity)
        { spaces.remove(lastS);
            globalSpace.remove(lastS);}
        else {
            lastS.start += totalNeed;
            lastS.capcity = lastS.end - lastS.start;
        }

        return true;
    }

    private void adjustETS(ETS ets)
    {
        while (ets.residue.size()!=0)
        {Space s=ets.residue.get(0);
            ets.residue.remove(s);
            globalSpace.remove(s);
        }

        while (ets.sofjob.size()!=0)
        {Space s=ets.sofjob.get(0);
            ets.sofjob.remove(s);
            globalSpace.remove(s);
        }

        long startTime = ets.start;
        ets.allocatableJobs.sort((j1, j2) -> Long.compare(j1.startTime, j2.startTime));
        /*space creation*/

        for (int i = 0; i < ets.allocatableJobs.size(); i++)
        {
            Job j = ets.allocatableJobs.get(i);
            j.allocatableSpace.clear();

            long startJ = j.startTime;
            long endJ = j.startTime+j.task.WCET;

            if (startTime < startJ)
            {
                /*create residue*/
                Space sp1 = new Space(startTime, startJ);
                globalSpace.add(sp1);
                ets.residue.add(sp1);

                /*create sofjob*/
                Space sp2 = new Space(startJ, endJ);
                ets.sofjob.add(sp2);
                globalSpace.add(sp2);
                j.allocatableSpace.add(sp2);
                sp2.allocatableJobs.add(j);
                startTime = endJ;
            }

            else if (startTime == startJ)
            {
                /*create sofjob and link with ets*/
                Space sp3 = new Space(startJ, endJ);
                ets.sofjob.add(sp3);
                globalSpace.add(sp3);
                j.allocatableSpace.add(sp3);
                sp3.allocatableJobs.add(j);
                startTime = endJ;
            }

            else{
                System.out.println("est adjust error");
                System.exit(-1);
            }


        }

        /*use the last space*/
        if (startTime < ets.end)
        {
            Space sp1 = new Space(startTime, ets.end);
            globalSpace.add(sp1);
            ets.residue.add(sp1);
            startTime = ets.end;
        }

    }

    private List<Space> getSpacesForAllocation(Job job, List<Job> allocateJobs, List<Space> allSpaces) {
        List<List<Space>> solutions = new ArrayList<>();
        //   if(allSpaces.get(0).start==71)
        //      System.out.print("jion error X ");
        for (int i = 0; i < job.spaceInRange.size() - 1; i++) {
            long space = 0;
            int index = i;

            while (space < job.task.WCET && index < job.spaceInRange.size()) {
                space += Math.min(job.deadline, job.spaceInRange.get(index).end) - Math.max(job.releaseTime, job.spaceInRange.get(index).start);
                index++;
            }

            if (space >= job.task.WCET) {
                List<Space> spaces = new ArrayList<>();
                for (int j = i; j < index; j++) {
                    spaces.add(job.spaceInRange.get(j));
                }
                solutions.add(spaces);

            }
        }

        List<Integer> weight = new ArrayList<>();

        for (int i = 0; i < solutions.size(); i++) {
            List<Space> oneSolution = solutions.get(i);
            // long lastShift = ;

            boolean isFeasible = true;
            int scarifice = 0;
            long requiredSpace = job.task.WCET;

            outerloop: for (int j = 0; j < oneSolution.size() - 1; j++) {
                Space s1 = oneSolution.get(j);
                Space s2 = oneSolution.get(j + 1);

                requiredSpace -= Math.min(s1.end, job.deadline) - Math.max(s1.start, job.releaseTime);

                List<Job> jobsInBetween = getJobsInBtween(s1, s2, allocateJobs);
                for (int k = 0; k < jobsInBetween.size(); k++) {
                    Job shiftedJob = jobsInBetween.get(k);
                    if (shiftedJob.startTime + requiredSpace + shiftedJob.task.WCET > shiftedJob.deadline) {
                        isFeasible = false;
                        break outerloop;
                    }

                    if (shiftedJob.delta == shiftedJob.startTime)
                        scarifice++;
                }

            }

            if (!isFeasible) {
                solutions.remove(i);
                i--;
            } else {
                weight.add(scarifice);
            }
        }

        if (solutions.size() == 0)
            return null;

        assert (solutions.size() == weight.size());

        int index = -1;
        int minWeight = Integer.MAX_VALUE;

        for (int i = 0; i < solutions.size(); i++) {
            if (minWeight > weight.get(i)) {
                minWeight = weight.get(i);
                index = i;
            }
        }
        //  if(allSpaces.get(0).start>solutions.get(index).get(0).start)
        //      System.out.print("jion error X ");
        return solutions.get(index);


    }

    private List<Job> getJobsInBtween(Space s1, Space s2, List<Job> allocatedJobs) {
        long start = Math.min(s1.start, s2.start);
        long end = Math.max(s1.end, s2.end);

        List<Job> jobsInBetween = new ArrayList<>();

        for (int i = 0; i < allocatedJobs.size(); i++) {
            if (allocatedJobs.get(i).startTime >= start && allocatedJobs.get(i).startTime + allocatedJobs.get(i).task.WCET <= end)
                jobsInBetween.add(allocatedJobs.get(i));
        }

        return jobsInBetween;
    }


    /********  find the better ets and move to it  ***********/

    public Long Capacity(Job j,ETS ets)
    {
        /**
         * 在不干扰其他任务的情况下，且尽可能去多的一个好的value的情况下，job能移动的范围
         * **/
        Long capa = 0L;
        /*search the nearest residue after job*/
        int k=-1;
        for (int i = 0; i < ets.residue.size(); i++)
            if (j.startTime + j.task.WCET == ets.residue.get(i).start)
                k=i;

        /*the nearest space after job is sofjob*/
        if(k==-1)         // find the correct one by considering the deadline of jobs and the budget of the server.
            return capa;

        capa=Math.min(ets.residue.get(k).capcity,j.lastStartTime-j.startTime-j.task.WCET);
        /*find the best value of j in range of capa*/
        if(capa>0)
            capa=bestmove(j,capa);

        return capa;
    }

    private Long findstrat(ETS ets,Job j)
    {ets.residue.sort((s1, s2) -> Long.compare(s1.start, s2.start));

        /*search the first residue can join the job*/
        for (int i = 0; i < ets.residue.size(); i++)
        {Space s=ets.residue.get(0);
            boolean m=mayjion(s,j);
            if(m)
                return s.start;
        }
        return 0L;
    }

    /*get the max value,may get*/
    private double maxvalue(Job j,Long capa)
    {
        Long start=j.startTime;
        double max=j.task.Vmin;
        double max1=j.task.Vmin;
        /*compute the max of value*/
        for(Long i=0L;i<=capa;i++)
        {
            j.startTime=start+i;
            max1=new AnalysisUtils().getValue(j);
            if(max1>max)
            {
                max=max1;
            }
        }
        j.startTime=start;

        return max;
    }

    public boolean mayjion(Space s,Job j)
    {
        /*the time of job running out of the space of residue*/
        if (s.end <= j.releaseTime || s.start >= j.deadline)
            return false;

        /*the residue enough space*/
        long possibleStart = Math.max(s.start, j.releaseTime);
        long possibleEnd = Math.min(s.end, j.deadline);
        if (possibleEnd - possibleStart >= j.task.WCET)
            return true;

        return false;
    }

    private void move_ets(Job job,ETS ets0,ETS toets)
    {
        ets0.allocatableJobs.remove(job);
        ets0.Jobinrange.add(job);
        job.allocatableETS.remove(ets0);

        Space s1=job.allocatableSpace.get(0);
        s1.allocatableJobs.clear();

        ets0.residue.sort((s3, s4) -> Long.compare(s3.start, s4.start));
        /*adjust the space in ets0 after the job move out*/
        int k = -1;

        /* consider space before s1*/
        for (int i = 0; i < ets0.residue.size(); i++)
            if (s1.start == ets0.residue.get(i).end)
                k=i;
        if (k == -1)     //the space before s1 is sofjob
        {
            ets0.residue.add(s1);
            ets0.sofjob.remove(s1);
            ets0.residue.sort((s3, s4) -> Long.compare(s3.start, s4.start));
        }
        else            // the space before s1 is residue
        {
            s1.start=ets0.residue.get(k).start;
            s1.capcity+=ets0.residue.get(k).capcity;
            globalSpace.remove(ets0.residue.get(k));

            ets0.residue.add(s1);
            ets0.sofjob.remove(s1);
            ets0.residue.remove(ets0.residue.get(k));
            ets0.residue.sort((s3, s4) -> Long.compare(s3.start, s4.start));
        }

        /* consider space after s1*/
        k = -1;
        for (int i = 0; i < ets0.residue.size(); i++)
            if (s1.end == ets0.residue.get(i).start)
                k=i;
        if (k != -1) // the space after s1 is residue
        {
            s1.end=ets0.residue.get(k).end;
            s1.capcity+=ets0.residue.get(k).capcity;
            globalSpace.remove(ets0.residue.get(k));

            ets0.residue.remove(ets0.residue.get(k));
            ets0.residue.sort((s3, s4) -> Long.compare(s3.start, s4.start));
        }
        job.allocatableSpace.clear();

        joinjob(toets,job);

    }

    private boolean joinjob(ETS ets, Job j) {
        ets.residue.sort((s1, s2) -> Long.compare(s1.start, s2.start));

        /*search the first residue can join the job*/
        for (int i = 0; i < ets.residue.size(); i++)
        {
            Space s = ets.residue.get(i);

            /*the time of job running out of the space of residue*/
            if (s.end <= j.releaseTime || s.start >= j.deadline)
                continue;

            /*the residue enough space*/
            long possibleStart = Math.max(s.start, j.releaseTime);
            long possibleEnd = Math.min(s.end, j.deadline);
            if (possibleEnd - possibleStart >= j.task.WCET)
            {
                /*allot run sever*/
                ets.Jobinrange.remove(j);
                ets.allocatableJobs.add(j);
                j.allocatableETS.add(ets);

                /*allot run space*/
                Space s1 = new Space(possibleStart, possibleStart + j.task.WCET);
                globalSpace.add(s1);

                j.startTime = possibleStart;
                j.allocatableSpace.add(s1);
                s1.allocatableJobs.add(j);

                ets.sofjob.add(s1);

                /*manage the space*/
                if (s1.start > s.start)     //have residue in s before s1
                {
                    Space sp1 = new Space(s.start, s1.start);
                    globalSpace.add(sp1);
                    ets.residue.add(sp1);
                }
                if (s1.end < s.end)    //have residue in s after s1
                {
                    Space sp2 = new Space(s1.end, s.end);
                    globalSpace.add(sp2);
                    ets.residue.add(sp2);
                }
                ets.residue.remove(s);
                globalSpace.remove(s);
                globalSpace.sort((S1, S2) -> Long.compare(S1.start, S2.start));

                return true;
            }
        }

        /* move job to jion the job*/
        ets.residue.sort((s1,s2)->Long.compare(s1.start,s2.start));
        boolean m=allocJob(j,ets.allocatableJobs,ets.Jobinrange,ets.residue);
        j.spaceInRange.clear();

        if(m)
        {
            j.allocatableETS.add(ets);
            adjustETS(ets);
        }
        return m;
    }

    /********************** Move job for better value ********************/
    public void movejob(ETS ets)
    {
        /**
         * 在ets内部移动如果存在一个使得value更高的开始时间，便移动
         * **/
        ets.residue.sort((s1, s2) -> Long.compare(s1.start, s2.start));
        ets.allocatableJobs.sort((s1, s2) -> Long.compare(s1.startTime, s2.startTime));

        for (int i = ets.allocatableJobs.size()-1; i >=0; i--)
        {
            Job j = ets.allocatableJobs.get(i);

            Long capacity = Capacity(j,ets);
            // find the time range and use a method to iterate through each time point to find the best accuracy
            if(j.startTime+capacity>j.lastStartTime)
                System.out.print(" ");
            if (capacity <= 0)
                continue;
            if (capacity > 0)
                MOVE(ets, j,  capacity);
        }

        /*move the no value-increase job*/
        while(ets.needmoveagain.size()!=0)
        {
            ets.needmoveagain.sort((s1, s2) -> Long.compare(s1.startTime, s2.startTime));
            Job job=ets.needmoveagain.get(0);
            MOVEagain(ets,job);
            ets.needmoveagain.remove(job);
        }

        /*adjust the sever`s start = the start time of first job in sever*/
        adjust(ets);
    }

    /*move from rear to front*/
    public boolean MOVE(ETS ets, Job j, Long move)
    {
        List<Job> jobs = ets.allocatableJobs;
        int i, k;

        /* search for the nearest residue space after job*/
        k = 0;
        for (i = 0; i < ets.residue.size(); i++)
            if (j.startTime + j.task.WCET > ets.residue.get(i).start)
                k++;
        Space msp = ets.residue.get(k);

        /* move the msp */
        msp.start = msp.start + move;
        msp.capcity = msp.capcity - move;
        if (msp.start == msp.end)
        {
            ets.residue.remove(msp);
            globalSpace.remove(msp);
        }

        /* move job*/
        j.startTime = j.startTime + move;
        j.allocatableSpace.get(0).start = j.allocatableSpace.get(0).start + move;
        j.allocatableSpace.get(0).end = j.allocatableSpace.get(0).end + move;

        /* adjust the change of space after move job**/
        Space sp = new Space(j.allocatableSpace.get(0).start - move, j.allocatableSpace.get(0).start);
        k = -1;
        for (i = 0; i < ets.residue.size(); i++)
            if (sp.start == ets.residue.get(i).end)
                k=i;
        if (k == -1)     //the space before np is sofjob
        {
            ets.residue.add(sp);
            globalSpace.add(sp);
            ets.residue.sort((s1, s2) -> Long.compare(s1.start, s2.start));
        }
        else            // the space before np is residue
        {
            ets.residue.get(k).end = sp.end;
            ets.residue.get(k).capcity = ets.residue.get(k).capcity + sp.capcity;
        }
        return true;
    }

    public boolean MOVEagain(ETS ets, Job j)
    {
        int i, k;
        /* search for the nearest residue space befor job*/
        k = -1;
        for (i = 0; i < ets.residue.size(); i++)
            if (j.startTime == ets.residue.get(i).end)
                k=i;

        if(k==-1)   //the neares space before job is sofjob ,can`t move
            return true;

        Space msp = ets.residue.get(k);
        Long move=Math.min(msp.capcity,j.startTime-j.releaseTime);

        /* move the msp */
        msp.end = msp.end - move;
        msp.capcity = msp.capcity-move;
        if (msp.start == msp.end)
        {
            ets.residue.remove(msp);
            globalSpace.remove(msp);
        }

        /* move job*/
        j.startTime = j.startTime - move;
        j.allocatableSpace.get(0).start = j.allocatableSpace.get(0).start - move;
        j.allocatableSpace.get(0).end = j.allocatableSpace.get(0).end - move;
        Space sp = new Space(j.allocatableSpace.get(0).end, j.allocatableSpace.get(0).end+move);

        /* adjust the change of space after move job*/
        /* search the residue after np */
        k = -1;
        for (i = 0; i < ets.residue.size(); i++)
            if (sp.end == ets.residue.get(i).start)
                k=i;

        if (k == -1)  // the space after np is sofjob
        {
            ets.residue.add(sp);
            globalSpace.add(sp);
            ets.residue.sort((s1, s2) -> Long.compare(s1.start, s2.start));
        }
        else   // the space after np is residue
        {
            ets.residue.get(k).start= sp.start;
            ets.residue.get(k).capcity = ets.residue.get(k).capcity + sp.capcity;
        }
        return true;
    }

    /*get the best distance of move*/
    private Long bestmove(Job j,Long capa)
    {
        Long start=j.startTime;
        Long besttime=capa;
        double max=j.task.Vmin;
        double max1=j.task.Vmin;
        /*compute the max of value*/
        for(Long i=0L;i<=capa;i++)
        {
            j.startTime=start+i;
            max1=new AnalysisUtils().getValue(j);
            if(max1>max)
            {
                max=max1;
                besttime=i;
            }
        }
        j.startTime=start;

        /*if no change of value ,move capa*/
        if(max==j.task.Vmin)
        {
            j.allocatableETS.get(0).needmoveagain.add(j);
            besttime=capa;
        }
        return besttime;
    }

    public void adjust(ETS ets)
    {
        ets.allocatableJobs.sort((s1, s2) -> Long.compare(s1.startTime, s2.startTime));
        ets.residue.sort((s1, s2) -> Long.compare(s1.start, s2.start));

        /*adjust the sever`s start =the start time of first job in sever */
        if (ets.start != ets.allocatableJobs.get(0).startTime)
        {
            /*move out the first residue to serve*/
            Space s = ets.residue.get(0);
            ets.before.add(s);
            globalSpace.add(s);
            ets.residue.remove(s);
            globalSpace.remove(s);
            ets.start = ets.allocatableJobs.get(0).startTime;
            ets.budget = ets.budget - s.capcity;
        }
    }

    /********************** Move job END********************/


    /********************** CALCULATION ALPHA ********************/
    private void Getalph(long hyperperiod)
    {
    /** from front to back**/

    /**
     * 从前往后计算每一个ETS的后一个ETS的before，并将该ETS的结束时间往后移动后一个ETS的before
     * **/
        for (int i = 0; i < ETSs.size() - 1; i++)
        {
            ETS ets1 = ETSs.get(i);
            ETS ets2 = ETSs.get(i + 1);
            if(ets1.allocatableJobs.size() == 0)
                continue;

            Long min1=0L;
            /**find the min1(limited by the sever after ets1) of sever ets1 **/
            /*the ets2 is empty*/
            if (ets2.allocatableJobs.size() == 0)
            {
                min1=ets2.budget;
                /*the server after empty sever have before*/
                if(i+2< ETSs.size()&&ETSs.get(i + 2).before.size()!=0)
                    min1+=ETSs.get(i + 2).before.get(0).capcity;
                ETSs.remove(ets2);
            }
            /*the capcity of est2.before*/
            else if (ets2.before.size() == 0)
                min1 = 0L;
            else min1= ets2.before.get(0).capcity;
            ets1.end+=min1;
            ets1.budget+=min1;
        }

        /*compute the finally sever*/
        ETS ets = ETSs.get(ETSs.size()-1);
        if(ets.alph==-1)
        {
            Job job0=ets.allocatableJobs.get(ets.allocatableJobs.size()-1);
            Long min1 = hyperperiod-job0.startTime-job0.task.WCET;
            ets.end+=min1;
            ets.budget+=min1;
            /* find the max interference of jobs in sever ets1*/
            for (int k = ets.allocatableJobs.size()-1; k >0; k--) {
                Job job= ets.allocatableJobs.get(k);
                Job job1=ets.allocatableJobs.get(k-1);
                job.Max_interference = Math.min(min1,job.lastStartTime - job.startTime);
                min1=job.Max_interference+job.startTime-job1.startTime-job1.task.WCET;
            }
            if(ets.allocatableJobs.size()==1)
                min1=Math.min(job0.lastStartTime-job0.startTime,min1);
            ets.alph = min1;
        }

        if(ETSs.get(0).allocatableJobs.size()==0)
            ETSs.remove(ETSs.get(0));

        /** from back to front**/
        /**
         * 从后往前，叠加式计算前一个ets的容错空间
         * 每一个ETS的容错空间取决于其内部所有JOB的最早结束时间与该ETS的end以及后一个ETS的alph
         * **/
        for (int i=ETSs.size()-1;i>1;i--)
        {
            ETS ets1 = ETSs.get(i);
            ETS ets2 = ETSs.get(i -1);

            int j=0;  /*the job in ets2 have latest deadline*/
            for(int k=0;k<ets2.allocatableJobs.size();k++)
                if(ets2.allocatableJobs.get(k).deadline<ets2.allocatableJobs.get(j).deadline)
                    j=k;

            Job job=ets2.allocatableJobs.get(j);
            Long lastdeadline=Math.min(job.deadline,ets1.start+ets1.alph);
            if(lastdeadline>ets2.end) {
                ets2.alph = lastdeadline - ets2.end;
                ets2.end = lastdeadline;
                ets2.budget = ets2.end - ets2.start;
            }

            /* find the max interference of jobs in sever ets2*/
            Job lastjob=ets2.allocatableJobs.get(ets2.allocatableJobs.size()-1);
            Long min1=Math.min(lastjob.lastStartTime-lastjob.startTime,ets2.end-lastjob.startTime-lastjob.rWCET);
            for (int k = ets2.allocatableJobs.size()-1; k >0; k--) {
                Job job0= ets2.allocatableJobs.get(k);
                Job job1=ets2.allocatableJobs.get(k-1);
                job0.Max_interference = Math.min(min1,job0.lastStartTime - job0.startTime);
                min1=job0.Max_interference+job0.startTime-job1.startTime-job1.task.WCET;
            }

            ets1.allocatableJobs.get(0).Max_interference=min1;
            ets2.alph=min1;
        }

        /*compute the first sever*/
        ETS ets0 = ETSs.get(0);
        ETS ets1 = ETSs.get(1);
        int j=0;
        for(int k=0;k<ets1.allocatableJobs.size();k++)
            if(ets1.allocatableJobs.get(k).deadline<ets1.allocatableJobs.get(j).deadline)
                j=k;

        Job job=ets1.allocatableJobs.get(j);

        Long lastdeadline=Math.min(job.deadline-ets0.budget,ets1.start+ets1.alph);
        if(lastdeadline>ets.end) {
            ets0.alph += lastdeadline - ets0.end;
            ets0.end = lastdeadline;
            ets0.budget = ets0.end - ets0.start;
        }
    }
    /**************************** Alloct End ****************************/

    /**************************** Graph Decompose ****************************/

    private List<List<Job>> decomposeGraphs(List<List<Job>> graphs) {
        List<Job> saveJobs = new ArrayList<>();
        List<Job> discardJobs = new ArrayList<>();

        for (int i = 0; i < graphs.size(); i++) {
            List<Job> oneGraph = graphs.get(i);

            List<List<Job>> decomposedGraph = decomposeOneGraph(oneGraph);

            saveJobs.addAll(decomposedGraph.get(0));
            discardJobs.addAll(decomposedGraph.get(1));
        }

        List<List<Job>> decomposedJobs = new ArrayList<>();
        decomposedJobs.add(saveJobs);
        decomposedJobs.add(discardJobs);

        assert (graphs.stream().mapToInt(t -> t.size()).sum() == saveJobs.size() + discardJobs.size());

        return decomposedJobs;
    }

    private List<List<Job>> decomposeOneGraph(List<Job> oneGraph) {
        List<Job> saveJobs = new ArrayList<>();
        List<Job> discardJobs = new ArrayList<>();

        List<Job> graph = new ArrayList<>(oneGraph);
        graph.sort((j1, c2) -> comparatorForPsiCandP(j1, c2));

        boolean keepGoing = false;
        for (int i = 0; i < graph.size(); i++) {
            if (graph.get(i).interferingJobs.size() > 0) {
                keepGoing = true;
                break;
            }
        }

        while (keepGoing) {
            Job j = graph.get(0);
            j.interferingJobs.clear();
            graph.remove(0);
            discardJobs.add(j);

            for (int i = 0; i < graph.size(); i++) {
                graph.get(i).interferingJobs.remove(j);
            }

            graph.sort((j1, c2) -> comparatorForPsiCandP(j1, c2));

            keepGoing = false;
            for (int i = 0; i < graph.size(); i++) {
                if (graph.get(i).interferingJobs.size() > 0) {
                    keepGoing = true;
                    break;
                }
            }
        }

        saveJobs.addAll(graph);

        List<List<Job>> decomposedJobs = new ArrayList<>();
        decomposedJobs.add(saveJobs);
        decomposedJobs.add(discardJobs);

        return decomposedJobs;
    }

    private int comparatorForPsiCandP(Job j1, Job j2) {
        if (intervalue(j1) > intervalue(j2))
            return -1;
        else if (intervalue(j1) < intervalue(j2))
            return 1;
        else {
            int result = Long.compare(j1.task.WCET, j2.task.WCET);
            if (result != 0)
                return result;
            else {
                return Long.compare(j1.task.priority, j2.task.priority);
            }
        }
    }

    private double intervalue(Job j1)
    {double valuesum=0;
        for(int i=0;i<j1.interferingJobs.size();i++)
        {
          Job job=j1.interferingJobs.get(i);
          valuesum+=job.task.Vmax;

        }
        return valuesum;
    }

    /**************************** Graph Decompose ****************************/

    /**************************** Graph Generation ****************************/

    private List<List<Job>> generateDependencyGraph(List<Job> jobs) {

        jobs.sort((c1, c2) -> Long.compare(c1.delta, c2.delta));

        for (int i = 0; i < jobs.size(); i++) {
            Job jobA = jobs.get(i);
            for (int j = 0; j < jobs.size(); j++) {
                if (i != j) {
                    Job jobB = jobs.get(j);
                    boolean overleap = true;
                    if (jobA.idealFinish <= jobB.delta || jobA.delta >= jobB.idealFinish)
                        overleap = false;

                    if (overleap) {
                        if (!jobA.interferingJobs.contains(jobB))
                            jobA.interferingJobs.add(jobB);
                        if (!jobB.interferingJobs.contains(jobA))
                            jobB.interferingJobs.add(jobA);
                    }

                }
            }
        }

        List<Job> forGraphs = new ArrayList<Job>(jobs);

        List<List<Job>> graphs = new ArrayList<>();

        while (forGraphs.size() > 0) {
            List<Job> graph = new ArrayList<>();

            graph.add(forGraphs.get(0));
            forGraphs.remove(0);

            for (int i = 0; i < graph.size(); i++) {
                Job job = graph.get(i);
                List<Job> interferingJobs = job.interferingJobs;
                for (int j = 0; j < interferingJobs.size(); j++) {
                    Job interferingJ = interferingJobs.get(j);
                    if (!graph.contains(interferingJ)) {
                        graph.add(interferingJ);
                        forGraphs.remove(interferingJ);
                    }
                }
            }

            graphs.add(graph);
        }

        int numberOfJobs = graphs.stream().mapToInt(t -> t.size()).sum();

        assert (numberOfJobs == jobs.size());

        return graphs;
    }

    /**************************** Graph Generation ****************************/


}







