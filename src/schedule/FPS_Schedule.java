package schedule;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import entity.Job;
import entity.PeriodicTask;
import utils.AnalysisUtils;
import entity.result;

public class FPS_Schedule {

	public  result schedule(List<PeriodicTask> tasks) {

		Pair<List<Job>, Long> pair = new AnalysisUtils().getJobsInHyperPeriod(tasks);

		List<Job> jobsInHP = pair.getKey();

		List<Job> jobs = new ArrayList<Job>(jobsInHP);
		List<Job> exectued = new ArrayList<>();
        Long system_time=0L;
		while (jobs.size() > 0) {
			long mostRecentRelease = getCurrentTime(jobs);

			List<Job> releasedJobs = getReleasedJobs(jobs, mostRecentRelease);

			Job j = releasedJobs.get(0);
			j.startTime = Math.max(system_time,mostRecentRelease);

			system_time = Math.max(system_time,mostRecentRelease)+j.task.WCET;

			exectued.add(j);
			jobs.remove(j);

		}

		assert (exectued.size() == jobsInHP.size());

		// check deadline miss
		for (int i = 0; i < exectued.size(); i++) {
			if (exectued.get(i).startTime + exectued.get(i).task.WCET > exectued.get(i).deadline) {
				return null;
			}
		}

		DecimalFormat df = new DecimalFormat("#.##");

		int numOfExact = 0;
		double totalValue = 0;

		for (int i = 0; i < exectued.size(); i++) {
			Job j = exectued.get(i);
			assert (j.startTime >= 0);
			if (j.delta == j.startTime)
				numOfExact++;
			totalValue += new AnalysisUtils().getValue(j);
		}

		double exact = Double.parseDouble(df.format((double) numOfExact / (double) exectued.size()));
		totalValue = (double) totalValue / (double) exectued.stream().mapToDouble(j -> j.task.Vmax).sum();
		totalValue = Double.parseDouble(df.format(totalValue));

		List<List<Double>> pfs = new ArrayList<>();
		List<Double> pf = new ArrayList<>();
		pf.add(exact);
		pf.add(totalValue);
		pfs.add(pf);

		return new result(pfs,exectued);
	}

	private long getCurrentTime(List<Job> jobs) {
		long time = jobs.stream().mapToLong(j -> j.releaseTime).min().getAsLong();
		return time;
	}

	private List<Job> getReleasedJobs(List<Job> jobs, long time) {
		List<Job> released = new ArrayList<>();

		for (int i = 0; i < jobs.size(); i++) {
			if (jobs.get(i).releaseTime <= time)
				released.add(jobs.get(i));
		}

		released.sort((j1, j2) -> Long.compare(j1.deadline, j2.deadline));

		return released;
	}

}
