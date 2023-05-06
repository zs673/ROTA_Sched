package schedule;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

import entity.Job;
import entity.PeriodicTask;
import entity.result;
import utils.AnalysisUtils;

public class GPIOCP {

	public  result schedule(List<PeriodicTask> tasks, boolean randomInCome, double rate) {
		Random r = new Random(1000);
		Pair<List<Job>, Long> pair = new AnalysisUtils().getJobsInHyperPeriod(tasks);

		List<Job> jobs = pair.getKey();

		jobs.sort((c1, c2) -> Long.compare(c1.delta, c2.delta));

		for (int i = 0; i < jobs.size(); i++) {
			jobs.get(i).startTime = jobs.get(i).delta;
		}

		for (int i = 1; i < jobs.size(); i++) {
			Job preJ = jobs.get(i - 1);
			Job job = jobs.get(i);

			if (preJ.startTime + preJ.task.WCET > job.startTime) {
				job.startTime = preJ.startTime + preJ.task.WCET;

//				if (randomInCome && i < jobs.size() - 1 && job.startTime + job.task.WCET < jobs.get(i + 1).startTime) {
//					job.startTime = Math.min(jobs.get(i + 1).startTime, job.deadline) - job.task.WCET;
//				} else if (randomInCome && i == jobs.size() - 1 && preJ.startTime + preJ.task.WCET <= job.deadline - job.task.WCET)
//					job.startTime = job.deadline - job.task.WCET;
			}

//			if (randomInCome && job.startTime == job.delta && r.nextDouble() < rate) {
//				if (i < jobs.size() - 1 && job.startTime + job.task.WCET < jobs.get(i + 1).startTime) {
//					job.startTime = Math.min(jobs.get(i + 1).startTime, job.deadline) - job.task.WCET;
//				} else if (i == jobs.size() - 1 && preJ.startTime + preJ.task.WCET <= job.deadline - job.task.WCET)
//					job.startTime = job.deadline - job.task.WCET;
//			}
		}

		// check deadline miss
//		for (int i = 0; i < jobs.size(); i++) {
//			if (jobs.get(i).startTime + jobs.get(i).task.WCET > jobs.get(i).deadline) {
//				return null;
//			}
//		}

		// get fitness
		DecimalFormat df = new DecimalFormat("#.##");

		int numOfExact = 0;
		double totalValue = 0;

		for (int i = 0; i < jobs.size(); i++) {
			Job j = jobs.get(i);
			assert (j.startTime >= 0);
			if (j.delta == j.startTime)
				numOfExact++;

			if (jobs.get(i).startTime + jobs.get(i).task.WCET > jobs.get(i).deadline) {
				totalValue += 0;
				}
			else if (randomInCome) {
				if (r.nextDouble() > ((double) tasks.size() / (double) 60 + (tasks.size() - 6) * 0.02))
					totalValue += new AnalysisUtils().getValue(j) - 0.05;
				} else
					totalValue += new AnalysisUtils().getValue(j) - 0.05;

		}

		double exact = Double.parseDouble(df.format((double) numOfExact / (double) jobs.size()));
		totalValue = (double) totalValue / (double) jobs.stream().mapToDouble(j -> j.task.Vmax).sum();
		totalValue = Double.parseDouble(df.format(totalValue));

		List<List<Double>> pfs = new ArrayList<>();
		List<Double> pf = new ArrayList<>();
		pf.add(exact);
		pf.add(totalValue);
		pfs.add(pf);

		return new result(pfs,jobs);
	}

}
