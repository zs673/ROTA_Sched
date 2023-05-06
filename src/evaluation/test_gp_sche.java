package evaluation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import entity.ETS;
import entity.Job;
import entity.PeriodicTask;
import entity.result;
import generationTools.SimpleSystemGenerator;
import org.apache.commons.math3.util.Pair;
import schedule.GPIOCP;
import schedule.StaticSchedule;
import schedule.RIsche;
import utils.AnalysisUtils;

public class test_gp_sche {

    static int minT = 1;
    static int maxT = 20;
    static int totalTasks = 15;
    static double totalUtil = 0.7;
    static double valueRange = 0.5;
    static int LCM = 1440;

    static boolean isPeriodLogUni = true;
    static int seed = 10;

    static int NoS = 1000;

    public static void main(String args[]) {
        EP1_SchedulabilityTest();

    }

    public static void EP1_SchedulabilityTest() {
        List<Integer> best_sched = new ArrayList<>();
        List<Integer> fps_sched = new ArrayList<>();
        List<Integer> gpiocp_sched = new ArrayList<>();
        List<Integer> static_sched = new ArrayList<>();
        List<Integer> ri_sched = new ArrayList<>();


        for (int NoT = 4; NoT <= 16; NoT+=2) {
            SimpleSystemGenerator generator = new SimpleSystemGenerator(minT, maxT, LCM, NoT, NoT * 0.05, isPeriodLogUni, valueRange, seed, true);

            int best_schedulables = 0;
            int gpiocp_schedulables = 0;
            int static_schedulables = 0;
            int ri_schedulables = 0;

            for (int j = 0; j < NoS; j++) {
                System.out.println("NoT: " + NoT + " times: " + j);

                List<PeriodicTask> tasks = generator.generateTasks();
                List<List<Job>> init_jobs = new ArrayList<List<Job>>();
                Pair<List<Job>, Long> pair = new AnalysisUtils().getJobsInHyperPeriod(tasks);
                long hyperperiod = pair.getValue();


                result result_gpiocp = new GPIOCP().schedule(tasks, true, 0.2);
                result res_0 = rwcet(result_gpiocp.jobs, 0, 0, hyperperiod, result_gpiocp.etss);
                if (res_0.jobs.size() == result_gpiocp.jobs.size()) {
                    init_jobs.add(result_gpiocp.jobs);
                    gpiocp_schedulables++;
                }

                result result_static = new StaticSchedule().schedule(tasks, true);
                if (result_static != null) {
                    init_jobs.add(result_static.jobs);
                    static_schedulables++;
                }


                result result_ri = new RIsche().schedule(tasks);
                if (result_ri != null) {
                    ri_schedulables++;
                }
            }

            best_sched.add(best_schedulables);
            gpiocp_sched.add(gpiocp_schedulables);
            static_sched.add(static_schedulables);
            ri_sched.add(ri_schedulables);


        }

        System.out.println("Schedulability");
        System.out.println(best_sched);
        System.out.println(gpiocp_sched);
        System.out.println(static_sched);
        System.out.println(ri_sched);
    }

    private static result rwcet(List<Job> jobs,double overrate,double overmax,Long hyperperiod,List<ETS>etss) {
        jobs.sort((s1,s2)->Long.compare(s1.startTime,s2.startTime));
        List<Job> alloct=new ArrayList<Job>();
        List<Job> failed=new ArrayList<Job>();

        /* create timing defect */
        Random r=new Random();
        double num_over=jobs.size()*overrate;
        for (int i=0;i<num_over;i++)
        {
            int j=r.nextInt(jobs.size());
            while (jobs.get(j).rWCET!=jobs.get(j).task.WCET)
                j=r.nextInt(jobs.size());
            jobs.get(j).rWCET=Math.round(jobs.get(j).task.WCET*(1+overmax));
            if(jobs.get(j).rWCET==jobs.get(j).task.WCET)
                jobs.get(j).rWCET++;
        }

        /*for the schedule don`t have sever*/
        if(etss==null)
        {
            for(int i=0;i<jobs.size()-1;i++)
            {
                Job job0=jobs.get(i);
                Job job1=jobs.get(i+1);
                /* simulation the execute*/
                if(job0.startTime+job0.rWCET>hyperperiod||job0.startTime+job0.rWCET>job0.deadline)
                {
                    failed.add(job0);
                    job1.startTime=Math.max(job1.startTime,job0.startTime+job0.rWCET);
                    continue;
                }
                alloct.add(job0);
                job1.startTime=Math.max(job1.startTime,job0.startTime+job0.rWCET);
            }

            Job lastjob=jobs.get(jobs.size()-1);
            if(lastjob.startTime+lastjob.rWCET>lastjob.deadline)
                failed.add(lastjob);
            else alloct.add(lastjob);
        }
        /*for the schedule have sever*/
        else {
            etss.sort((s1,s2)->Long.compare(s1.start,s2.start));

            /* simulation the execute*/
            for(int i=0;i<etss.size();i++)
            {
                ETS ets=etss.get(i);
                if(ets.allocatableJobs.size()>0)
                {   /*find the really start of sever*/
                    ets.allocatableJobs.get(0).startTime=Math.max(ets.allocatableJobs.get(0).startTime,ets.start);
                    /* simulation the execute of sever*/
                    for(int j=0;j<ets.allocatableJobs.size()-1;j++)
                    {
                        Job job0=ets.allocatableJobs.get(j);
                        Job job1=ets.allocatableJobs.get(j+1);
                        if(job0.startTime>job0.deadline)
                        {
                            failed.add(job0);
                            job1.startTime=job0.startTime;
                            continue;
                        }

                        if(job0.startTime+job0.rWCET>ets.end||job0.startTime+job0.rWCET>job0.deadline)
                        {
                            failed.add(job0);
                            job1.startTime=Math.max(job1.startTime,Math.min(job0.deadline,ets.end));
                            continue;
                        }

                        alloct.add(job0);
                        job1.startTime=Math.max(job1.startTime,job0.startTime+job0.rWCET);
                    }
                    /* consider the last job`s execute and compute the next sever`s start time*/
                    Job lastjob1=ets.allocatableJobs.get(ets.allocatableJobs.size()-1);
                    if(i!=etss.size()-1)
                    {
                        ETS ets1=etss.get(i+1);
                        if(lastjob1.startTime>lastjob1.deadline)
                        {
                            failed.add(lastjob1);
                            ets1.start=lastjob1.startTime;
                        }
                        else if(lastjob1.startTime+lastjob1.rWCET>lastjob1.deadline||lastjob1.startTime+lastjob1.rWCET>ets.end)
                        {
                            failed.add(lastjob1);
                            ets1.start=Math.min(lastjob1.deadline,ets.end);
                        }
                        else
                        {
                            alloct.add(lastjob1);
                            ets1.start=lastjob1.startTime+lastjob1.rWCET;
                        }
                    }
                    /*the last sever`s last job*/
                    else {
                        if(lastjob1.startTime+lastjob1.rWCET>lastjob1.deadline||lastjob1.startTime+lastjob1.rWCET>ets.end)
                            failed.add(lastjob1);
                        else
                            alloct.add(lastjob1);
                    }
                }
            }

            for(int i=0;i<etss.size();i++)
            {
                ETS ets=etss.get(i);
                for (int k=0;k<ets.allocatableJobs.size();k++)
                    if(ets.allocatableJobs.get(k).startTime>ets.end)
                        System.out.print(" ");
            }
        }

        double totalValue=0;
        int numOfExact=0;

        for (int i = 0; i < alloct.size(); i++)
        {
            Job j = alloct.get(i);
            assert (j.startTime >= 0);
            if (j.delta == j.startTime)
                numOfExact++;
            totalValue += new AnalysisUtils().getValue(j);
        }
        DecimalFormat df = new DecimalFormat("#.##");
        totalValue = (double) totalValue / (double) jobs.stream().mapToDouble(j -> j.task.Vmax).sum();
        double exact = Double.parseDouble(df.format((double) numOfExact / (double) jobs.size()));
        totalValue = Double.parseDouble(df.format(totalValue));
        List<List<Double>> pfs1 = new ArrayList<>();
        List<Double> pf = new ArrayList<>();
        pf.add(exact);
        pf.add(totalValue);
        pfs1.add(pf);

        for(int k=0;k<alloct.size()-1;k++)
            if(alloct.get(k).startTime+alloct.get(k).rWCET>alloct.get(k+1).startTime)
                System.exit(1);

        if(etss==null)
            return new result(pfs1,alloct);
        return new result(pfs1,alloct,etss);
    }


}

