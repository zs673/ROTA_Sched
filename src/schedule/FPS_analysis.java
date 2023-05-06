package schedule;

import java.util.ArrayList;
import java.util.List;

import entity.PeriodicTask;

public class FPS_analysis {

	public boolean schedule(List<PeriodicTask> tasks) {

		List<PeriodicTask> taskForAnalysis = new ArrayList<PeriodicTask>(tasks);

		taskForAnalysis.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));

		long[] responseTime = getResponseTime(taskForAnalysis);

		return isSchedulable(responseTime, taskForAnalysis);
	}

	private long[] getResponseTime(List<PeriodicTask> taskForAnalysis) {

		long[] responseTime = new long[taskForAnalysis.size()];

		for (int i = 0; i < taskForAnalysis.size(); i++) {
			PeriodicTask task = taskForAnalysis.get(i);

			long B = getB(task, taskForAnalysis);
			long R = B;

			boolean isEqual = false, missDeadline = false;

			while (!isEqual) {
				isEqual = true;

				long newR = getW(R, B, task, taskForAnalysis);

				if (newR != R)
					isEqual = false;
				if (newR > task.deadline)
					missDeadline = true;

				R = newR;

				if (missDeadline)
					break;

			}

			responseTime[i] = R + task.WCET;
		}

		return responseTime;
	}

	private long getW(long R, long B, PeriodicTask task, List<PeriodicTask> tasks) {
		R = R == 0 ? 1 : R;

		long interference = 0;

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).priority > task.priority) {
				interference += Math.ceil((double) R / (double) tasks.get(i).period) * tasks.get(i).WCET;
			}
		}

		return B + interference;
	}

	private long getB(PeriodicTask task, List<PeriodicTask> tasks) {

		long maxC = 0;

		for (int i = 0; i < tasks.size(); i++) {
			int Prio = tasks.get(i).priority;
			if (task.priority > Prio)
				maxC = Math.max(maxC, tasks.get(i).WCET);

		}

		return maxC;
	}

	private boolean isSchedulable(long[] responseTime, List<PeriodicTask> taskForAnalysis) {

		for (int i = 0; i < taskForAnalysis.size(); i++) {
			if (taskForAnalysis.get(i).deadline < responseTime[i])
				return false;
		}
		return true;
	}

}
