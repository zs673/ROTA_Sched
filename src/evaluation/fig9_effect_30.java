package evaluation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import entity.PeriodicTask;
import entity.Job;
import entity.result;
import entity.ETS;
import generationTools.SimpleSystemGenerator;
import org.apache.commons.math3.util.Pair;
import schedule.*;
import utils.AnalysisUtils;
import schedule.GASchedule;

public class fig9_effect_30 {

    static int minT = 1;
    static int maxT = 20;
    static int totalTasks = 15;
    static double totalUtil = 0.7;
    static double valueRange = 0.5;
    static int LCM = 1440;

    static boolean isPeriodLogUni = true;
    static int seed = 10;

    static int NoS = 10;

    public static void main(String args[]) {
        // EP1_SchedulabilityTest();
        // EP2_IOPerformance();
        EP3_Interference();                 // test Acceptance Rate U=0.6 Pr=0.3 Pe = 0-2
        //EP4_Interference();
    }


    private static void  EP3_Interference()
    {
        int max = 1000;;
        List<List<Result>> fps = new ArrayList<>();
        List<List<Result>> gpio = new ArrayList<>();
        List<List<Result>> ga = new ArrayList<>();
        List<List<Result>> stat = new ArrayList<>();
        List<List<Result>> ri = new ArrayList<>();

        List<List<Double>> Fps=new ArrayList<>();
        List<List<Double>> Gpio=new ArrayList<>();
        List<List<Double>> Ga=new ArrayList<>();
        List<List<Double>> Stat=new ArrayList<>();
        List<List<Double>> Ri=new ArrayList<>();
        double overrate=0;
        double overmax=0;
        int NoT=12;

        for (overmax = 3.0; overmax <= 3.0; overmax += 0.4) {
            overrate = 0.3;
            SimpleSystemGenerator generator = new SimpleSystemGenerator(minT, maxT, LCM, NoT, NoT * 0.05, isPeriodLogUni, valueRange, seed, true);
            List<Result> fps_res = new ArrayList<>();
            List<Result> gpio_res = new ArrayList<>();
            List<Result> ga_res = new ArrayList<>();
            List<Result> stat_res = new ArrayList<>();
            List<Result> ri_res = new ArrayList<>();

            List<Double> fps_num = new ArrayList<>();
            List<Double> gpio_num = new ArrayList<>();
            List<Double> ga_num = new ArrayList<>();
            List<Double> stat_num = new ArrayList<>();
            List<Double> ri_num = new ArrayList<>();

            int count = 0;

            while (count < 1000) {
                System.out.println("NoT: " + NoT + " times: " + count + " over rate: " + overrate + " over max: " + overmax + "," + fps_res.size() + " " + gpio_res.size()+" " + ga_res.size()+" "+ stat_res.size() + " " + ri_res.size());

                List<PeriodicTask> tasks = generator.generateTasks();
                List<List<Job> >init_jobs = new ArrayList<List<Job>>();
                Pair<List<Job>, Long> pair = new AnalysisUtils().getJobsInHyperPeriod(tasks);
                long hyperperiod = pair.getValue();

                if (fps_res.size() < max) {
                    result result_fps = new FPS_Schedule().schedule(tasks);
                    if (result_fps != null) {
                        result res_0 = rwcet(result_fps.jobs, overrate, overmax, hyperperiod, result_fps.etss);
                        init_jobs.add(result_fps.jobs);
                        Result res = new Result(res_0.pfs);
                        fps_res.add(res);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double num = Double.parseDouble(df.format(res_0.jobs.size() * 1.0 / (result_fps.jobs.size() * 1.0)));
                        fps_num.add(num);
                    }
                }
                if (gpio_res.size() < max) {
                    result result_gpio = new GPIOCP().schedule(tasks,true,0.2);
                    if (result_gpio != null) {
                        result res_0 = rwcet(result_gpio.jobs, overrate, overmax, hyperperiod, result_gpio.etss);
                        init_jobs.add(result_gpio.jobs);
                        Result res = new Result(res_0.pfs);
                        gpio_res.add(res);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double num = Double.parseDouble(df.format(res_0.jobs.size() * 1.0 / (result_gpio.jobs.size() * 1.0)));
                        gpio_num.add(num);
                    }
                }

                if (stat_res.size() < max) {
                    result result_static = new StaticSchedule().schedule(tasks, true);
                    if (result_static != null) {
                        result res_0 = rwcet(result_static.jobs, overrate, overmax, hyperperiod, result_static.etss);
                        init_jobs.add(result_static.jobs);
                        Result res = new Result(res_0.pfs);
                        stat_res.add(res);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double num = Double.parseDouble(df.format(res_0.jobs.size() * 1.0 / (result_static.jobs.size() * 1.0)));
                        stat_num.add(num);
                    }
                }

                if (ga_res.size() < max && init_jobs.size()>0) {
                    result result_ga = new GASchedule().schedule(tasks, init_jobs,new Random(seed));
                    if (result_ga != null) {
                        result res_0 = rwcet(result_ga.jobs, overrate, overmax, hyperperiod, result_ga.etss);
                        Result res = new Result(res_0.pfs);
                        ga_res.add(res);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double num = Double.parseDouble(df.format(res_0.jobs.size() * 1.0 / (result_ga.jobs.size() * 1.0)));
                        ga_num.add(num);
                    }
                }

                if (ri_res.size() < max) {
                    result result_ri = new RIsche().schedule(tasks);
                    if (result_ri != null) {
                        result res_1 = rwcet(result_ri.jobs, overrate, overmax, hyperperiod, result_ri.etss);
                        Result res1 = new Result(res_1.pfs);
                        ri_res.add(res1);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double num = Double.parseDouble(df.format(res_1.jobs.size() * 1.0 / (result_ri.jobs.size() * 1.0)));
                        ri_num.add(num);
                    }
                }

                count = Math.min(Math.min(ga_res.size(),Math.min(fps_res.size(),gpio_res.size())), Math.min(stat_res.size(),ri_res.size()));
            }

            fps.add(fps_res);
            gpio.add(gpio_res);
            ga.add(ga_res);
            stat.add(stat_res);
            ri.add(ri_res);

            Fps.add(fps_num);
            Gpio.add(gpio_num);
            Ga.add(ga_num);
            Stat.add(stat_num);
            Ri.add(ri_num);
        }

        // NoT, value for each method: fps, gpiocp, static, ga
        List<List<List<Double>>> numOfExacts = new ArrayList<>();
        List<List<List<Double>>> values = new ArrayList<>();
        List<List<List<Double>>> allocte = new ArrayList<>();

        for (int i = 0; i < ri.size(); i++) {
            //value for each method: fps, gpiocp, static, ga
            List<List<Double>> oneSettingNum = new ArrayList<>();
            List<List<Double>> oneSettingVal = new ArrayList<>();
            List<List<Double>> oneSettingfail = new ArrayList<>();


            List<Result> fps1 = fps.get(i);
            List<Result> gpio1 = gpio.get(i);
            List<Result> ga1 = ga.get(i);
            List<Result> static1 = stat.get(i);
            List<Result> ri1 = ri.get(i);

            List<Double> fps1Res = flatList(fps1, 0);
            List<Double> gpio1Res = flatList(gpio1, 0);
            List<Double> ga1Res = flatList(ga1, 0);
            List<Double> static1Res = flatList(static1, 0);
            List<Double> ri1Res = flatList(ri1, 0);

            List<Double> fps2Res = flatList(fps1, 1);
            List<Double> gpio2Res = flatList(gpio1, 1);
            List<Double> ga2Res = flatList(ga1, 1);
            List<Double> static2Res = flatList(static1, 1);
            List<Double> ri2Res = flatList(ri1, 1);

            List<Double> Fps1Res = Fps.get(i);
            List<Double> Gpio1Res = Gpio.get(i);
            List<Double> Ga1Res = Ga.get(i);
            List<Double> Static1Res = Stat.get(i);
            List<Double> Ri1Res = Ri.get(i);

            oneSettingfail.add(Fps1Res);
            oneSettingfail.add(Gpio1Res);
            oneSettingfail.add(Ga1Res);
            oneSettingfail.add(Static1Res);
            oneSettingfail.add(Ri1Res);


            oneSettingNum.add(fps1Res);
            oneSettingNum.add(gpio1Res);
            oneSettingNum.add(ga1Res);
            oneSettingNum.add(static1Res);
            oneSettingNum.add(ri1Res);

            oneSettingVal.add(fps2Res);
            oneSettingVal.add(gpio2Res);
            oneSettingVal.add(ga2Res);
            oneSettingVal.add(static2Res);
            oneSettingVal.add(ri2Res);

            numOfExacts.add(oneSettingNum);
            values.add(oneSettingVal);
            allocte.add(oneSettingfail);
        }
        System.out.println("Number of success jobs");
        for ( int i = 0; i < allocte.size(); i++) {
            System.out.println("Not:"+4+i);

            List<List<Double>> oneSetting = allocte.get(i);

            for (int j = 0; j < oneSetting.size(); j++) {
                for (int k = 0; k < oneSetting.get(j).size(); k++) {
                    System.out.print(oneSetting.get(j).get(k) + " ");
                }
                System.out.println();
            }
        }

        System.out.println("\n\n Number of exact jobs");
        for (int i = 0; i < numOfExacts.size(); i++) {
            System.out.println("Not:"+4+i);
            List<List<Double>> oneSetting = numOfExacts.get(i);

            for (int j = 0; j < oneSetting.size(); j++) {
                for (int k = 0; k < oneSetting.get(j).size(); k++) {
                    System.out.print(oneSetting.get(j).get(k) + " ");
                }
                System.out.println();
            }

        }

        System.out.println("\n\n Values");
        for (int i = 0; i < values.size(); i++) {
            System.out.println("Not:"+4+i);
            List<List<Double>> oneSetting = values.get(i);
            for (int j = 0; j < oneSetting.size(); j++) {
                for (int k = 0; k < oneSetting.get(j).size(); k++) {
                    System.out.print(oneSetting.get(j).get(k) + " ");
                }
                System.out.println();
            }
        }
    }
    private static void  EP4_Interference()
    {
        int max = 1000;
        List<List<Result>> fps = new ArrayList<>();
        List<List<Result>> gpio = new ArrayList<>();
        List<List<Result>> ga = new ArrayList<>();
        List<List<Result>> stat = new ArrayList<>();
        List<List<Result>> ri = new ArrayList<>();

        List<List<Double>> Fps=new ArrayList<>();
        List<List<Double>> Gpio=new ArrayList<>();
        List<List<Double>> Ga=new ArrayList<>();
        List<List<Double>> Stat=new ArrayList<>();
        List<List<Double>> Ri=new ArrayList<>();
        double overrate=0.3;
        double overmax=0.3;
        for (int NoT = 4; NoT <= 16; NoT += 2) {
            SimpleSystemGenerator generator = new SimpleSystemGenerator(minT, maxT, LCM, NoT, NoT * 0.05, isPeriodLogUni, valueRange, seed, true);
            List<Result> fps_res = new ArrayList<>();
            List<Result> gpio_res = new ArrayList<>();
            List<Result> ga_res = new ArrayList<>();
            List<Result> stat_res = new ArrayList<>();
            List<Result> ri_res = new ArrayList<>();

            List<Double> fps_num = new ArrayList<>();
            List<Double> gpio_num = new ArrayList<>();
            List<Double> ga_num = new ArrayList<>();
            List<Double> stat_num = new ArrayList<>();
            List<Double> ri_num = new ArrayList<>();

            int count = 0;

            while (count < 1000) {
                System.out.println("NoT: " + NoT + " times: " + count + " over rate: " + overrate + " over max: " + overmax + "," + stat_res.size() + " " + ri_res.size());
                List<List<Job>>init_jobs = new ArrayList<List<Job>>();
                List<PeriodicTask> tasks = generator.generateTasks();
                Pair<List<Job>, Long> pair = new AnalysisUtils().getJobsInHyperPeriod(tasks);
                long hyperperiod = pair.getValue();

                if (fps_res.size() < max) {
                    result result_fps = new FPS_Schedule().schedule(tasks);
                    if (result_fps != null) {
                        result res_0 = rwcet(result_fps.jobs, overrate, overmax, hyperperiod, result_fps.etss);
                        init_jobs.add(result_fps.jobs);
                        Result res = new Result(res_0.pfs);
                        fps_res.add(res);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double num = Double.parseDouble(df.format(res_0.jobs.size() * 1.0 / (result_fps.jobs.size() * 1.0)));
                        fps_num.add(num);
                    }
                }

                if (gpio_res.size() < max) {
                    result result_gpio = new GPIOCP().schedule(tasks,true,0.2);
                    if (result_gpio != null) {
                        result res_0 = rwcet(result_gpio.jobs, overrate, overmax, hyperperiod, result_gpio.etss);
                        init_jobs.add(result_gpio.jobs);
                        Result res = new Result(res_0.pfs);
                        gpio_res.add(res);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double num = Double.parseDouble(df.format(res_0.jobs.size() * 1.0 / (result_gpio.jobs.size() * 1.0)));
                        gpio_num.add(num);
                    }
                }

                if (stat_res.size() < max) {
                    result result_static = new StaticSchedule().schedule(tasks, true);
                    if (result_static != null) {
                        result res_0 = rwcet(result_static.jobs, overrate, overmax, hyperperiod, result_static.etss);
                        init_jobs.add(result_static.jobs);
                        Result res = new Result(res_0.pfs);
                        stat_res.add(res);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double num = Double.parseDouble(df.format(res_0.jobs.size() * 1.0 / (result_static.jobs.size() * 1.0)));
                        stat_num.add(num);
                    }
                }

                if (ga_res.size() < max && init_jobs.size() != 0) {
                    result result_ga = new GASchedule().schedule(tasks, init_jobs,new Random(seed));
                    if (result_ga != null) {
                        result res_0 = rwcet(result_ga.jobs, overrate, overmax, hyperperiod, result_ga.etss);
                        Result res = new Result(res_0.pfs);
                        ga_res.add(res);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double num = Double.parseDouble(df.format(res_0.jobs.size() * 1.0 / (result_ga.jobs.size() * 1.0)));
                        ga_num.add(num);
                    }
                }



                if (ri_res.size() < max) {
                    result result_ri = new RIsche().schedule(tasks);
                    if (result_ri != null) {
                        result res_1 = rwcet(result_ri.jobs, overrate, overmax, hyperperiod, result_ri.etss);
                        Result res1 = new Result(res_1.pfs);
                        ri_res.add(res1);
                        DecimalFormat df = new DecimalFormat("#.##");
                        Double num = Double.parseDouble(df.format(res_1.jobs.size() * 1.0 / (result_ri.jobs.size() * 1.0)));
                        ri_num.add(num);
                    }
                }

                count = Math.min(Math.min(ga_res.size(),Math.min(fps_res.size(),gpio_res.size())), Math.min(stat_res.size(),ri_res.size()));
            }

            fps.add(fps_res);
            gpio.add(gpio_res);
            stat.add(stat_res);
            ga.add(ga_res);
            ri.add(ri_res);

            Fps.add(fps_num);
            Gpio.add(gpio_num);
            Ga.add(ga_num);
            Stat.add(stat_num);
            Ri.add(ri_num);
        }

        // NoT, value for each method: fps, gpiocp, static, ga
        List<List<List<Double>>> numOfExacts = new ArrayList<>();
        List<List<List<Double>>> values = new ArrayList<>();
        List<List<List<Double>>> allocte = new ArrayList<>();

        for (int i = 0; i < ri.size(); i++) {
            //value for each method: fps, gpiocp, static, ga
            List<List<Double>> oneSettingNum = new ArrayList<>();
            List<List<Double>> oneSettingVal = new ArrayList<>();
            List<List<Double>> oneSettingfail = new ArrayList<>();


            List<Result> fps1 = fps.get(i);
            List<Result> gpio1 = gpio.get(i);
            List<Result> ga1 = ga.get(i);
            List<Result> static1 = stat.get(i);
            List<Result> ri1 = ri.get(i);

            List<Double> fps1Res = flatList(fps1, 0);
            List<Double> gpio1Res = flatList(gpio1, 0);
            List<Double> ga1Res = flatList(ga1, 0);
            List<Double> static1Res = flatList(static1, 0);
            List<Double> ri1Res = flatList(ri1, 0);

            List<Double> fps2Res = flatList(fps1, 1);
            List<Double> gpio2Res = flatList(gpio1, 1);
            List<Double> ga2Res = flatList(ga1, 1);
            List<Double> static2Res = flatList(static1, 1);
            List<Double> ri2Res = flatList(ri1, 1);

            List<Double> Fps1Res = Fps.get(i);
            List<Double> Gpio1Res = Gpio.get(i);
            List<Double> Ga1Res = Ga.get(i);
            List<Double> Static1Res = Stat.get(i);
            List<Double> Ri1Res = Ri.get(i);

            oneSettingfail.add(Fps1Res);
            oneSettingfail.add(Gpio1Res);
            oneSettingfail.add(Ga1Res);
            oneSettingfail.add(Static1Res);
            oneSettingfail.add(Ri1Res);


            oneSettingNum.add(fps1Res);
            oneSettingNum.add(gpio1Res);
            oneSettingNum.add(ga1Res);
            oneSettingNum.add(static1Res);
            oneSettingNum.add(ri1Res);

            oneSettingVal.add(fps2Res);
            oneSettingVal.add(gpio2Res);
            oneSettingVal.add(ga2Res);
            oneSettingVal.add(static2Res);
            oneSettingVal.add(ri2Res);

            numOfExacts.add(oneSettingNum);
            values.add(oneSettingVal);
            allocte.add(oneSettingfail);
        }
        System.out.println("Number of success jobs");
        for ( int i = 0; i < allocte.size(); i++) {
            System.out.println("Not:"+4+i);

            List<List<Double>> oneSetting = allocte.get(i);

            for (int j = 0; j < oneSetting.size(); j++) {
                for (int k = 0; k < oneSetting.get(j).size(); k++) {
                    System.out.print(oneSetting.get(j).get(k) + " ");
                }
                System.out.println();
            }
        }

        System.out.println("\n\n Number of exact jobs");
        for (int i = 0; i < numOfExacts.size(); i++) {
            System.out.println("Not:"+4+i);
            List<List<Double>> oneSetting = numOfExacts.get(i);

            for (int j = 0; j < oneSetting.size(); j++) {
                for (int k = 0; k < oneSetting.get(j).size(); k++) {
                    System.out.print(oneSetting.get(j).get(k) + " ");
                }
                System.out.println();
            }

        }

        System.out.println("\n\n Values");
        for (int i = 0; i < values.size(); i++) {
            System.out.println("Not:"+4+i);
            List<List<Double>> oneSetting = values.get(i);
            for (int j = 0; j < oneSetting.size(); j++) {
                for (int k = 0; k < oneSetting.get(j).size(); k++) {
                    System.out.print(oneSetting.get(j).get(k) + " ");
                }
                System.out.println();
            }
        }
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

    private static List<Double> flatList(List<Result> list, int index) {
        List<Double> flat = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            Result s = list.get(i);
            for (int j = 0; j < s.res.size(); j++) {
                List<Double> pf = s.res.get(j);
                flat.add(pf.get(index));
            }
        }

        return flat;
    }

}



