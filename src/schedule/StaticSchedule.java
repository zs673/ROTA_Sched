package schedule;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import entity.Job;
import entity.PeriodicTask;
import entity.Space;
import utils.AnalysisUtils;
import entity.result;

public class StaticSchedule {

	public result schedule(List<PeriodicTask> tasks, boolean priorityFirst) {
		int numOfExact = 0;
		double totalValue = 0;

		Pair<List<Job>, Long> pair = new AnalysisUtils().getJobsInHyperPeriod(tasks);

		List<Job> jobs = pair.getKey();
		long hyperperiod = pair.getValue();

		List<List<Job>> graphs = generateDependencyGraph(jobs);

		List<List<Job>> decomposedGraphs = decomposeGraphs(graphs);

		List<Job> allocatedJobs = LCCD(decomposedGraphs, hyperperiod, priorityFirst);

		if (allocatedJobs == null)
			return null;
		else {
			for (int i = 0; i < allocatedJobs.size(); i++) {
				Job j = allocatedJobs.get(i);
				assert (j.startTime >= 0);
				if (j.delta == j.startTime)
					numOfExact++;
				totalValue += new AnalysisUtils().getValue(j);

			}
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

		return new result(pfs,allocatedJobs);
	}

	/**************************** LCCD ****************************/

	private List<Job> LCCD(List<List<Job>> decomposedGraphs, long hyperperiod, boolean priorityFirst) {

		List<Job> allocated = new ArrayList<>(decomposedGraphs.get(0));
		List<Job> toAlloc = new ArrayList<>(decomposedGraphs.get(1));

		List<Space> spaces = getFreeSpace(allocated, toAlloc, hyperperiod);

		fitAllocatableTasks(allocated, toAlloc, spaces, priorityFirst);

		spaces.sort((c1, c2) -> Long.compare(c1.start, c2.start));

		for (int i = 0; i < toAlloc.size(); i++) {
			boolean result = allocJob(toAlloc.get(i), allocated, toAlloc, spaces);

			if (!result)
				return null;
			else
				i--;
		}

		assert (allocated.size() == decomposedGraphs.get(0).size() + decomposedGraphs.get(1).size());
		return allocated;
	}

	private boolean allocJob(Job job, List<Job> allocated, List<Job> toAlloc, List<Space> spaces) {
		allocated.sort((j1, j2) -> Long.compare(j1.startTime, j2.startTime));

		/*
		 * Get spaces in range
		 */
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
			spaces.remove(first);
		
		

		for (int i = 1; i < solution.size() - 1; i++) {
			totalNeed -= solution.get(i).capcity;
			spaces.remove(solution.get(i));
		}

		Space lastS = solution.get(solution.size() - 1);
		
//		long restRequiredSpace = job.task.WCET - solution.stream().mapToLong(s -> s.capcity).sum() + lastS.capcity;

		if (totalNeed == lastS.capcity)
			spaces.remove(lastS);
		else {
			lastS.start += totalNeed;
			lastS.capcity = lastS.end - lastS.start;
		}

		return true;
	}

	private List<Space> getSpacesForAllocation(Job job, List<Job> allocateJobs, List<Space> allSpaces) {
		List<List<Space>> solutions = new ArrayList<>();

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

	private void fitAllocatableTasks(List<Job> allocated, List<Job> toAlloc, List<Space> spaces, boolean priorityFirst) {

		toAlloc.sort((j1, j2) -> comparatorForCandP(j1, j2, priorityFirst));

		for (int i = 0; i < toAlloc.size(); i++) {
			Job job = toAlloc.get(i);

			List<Space> allowedSpaces = job.allocatableSpace;

			if (allowedSpaces.size() == 0) {
				continue;
			}

			allowedSpaces.sort((s1, s2) -> compareSpace(s1, s2));

			allocated.add(job);
			toAlloc.remove(job);
			i--;

			// TODO: update GA side
			Space allocS = allowedSpaces.get(0);
			job.startTime = Math.max(allocS.start, job.releaseTime);
			job.allocatableSpace.clear();

			if (job.startTime == allocS.start) {
				allocS.start += job.task.WCET;
				allocS.capcity = allocS.end - allocS.start;
			} else {
				if (allocS.end > job.startTime + job.task.WCET) {
					Space newSpace = new Space(job.startTime + job.task.WCET, allocS.end);
					spaces.add(newSpace);

					for (int j = 0; j < allocS.allocatableJobs.size(); j++) {
						Job jj = allocS.allocatableJobs.get(j);

						if (newSpace.end <= jj.releaseTime || newSpace.start >= jj.deadline)
							continue;

						long possibleStart = Math.max(newSpace.start, jj.releaseTime);
						long possibleEnd = Math.min(newSpace.end, jj.deadline);

						if (possibleEnd - possibleStart >= jj.task.WCET) {
							newSpace.allocatableJobs.add(jj);
							jj.allocatableSpace.add(newSpace);
						}

					}
				}

				allocS.end = job.startTime;
				allocS.capcity = allocS.end - allocS.start;
			}

			if (allocS.start < allocS.end) {
				allocS.allocatableJobs.remove(job);
				for (int j = 0; j < allocS.allocatableJobs.size(); j++) {
					Job waitingJob = allocS.allocatableJobs.get(j);

					boolean canAlloc = true;

					if (allocS.end <= waitingJob.releaseTime || allocS.start >= waitingJob.deadline)
						canAlloc = false;

					long possibleStart = Math.max(allocS.start, waitingJob.releaseTime);
					long possibleEnd = Math.min(allocS.end, waitingJob.deadline);
					if (possibleEnd - possibleStart < waitingJob.task.WCET) {
						canAlloc = false;
					}

					if (!canAlloc) {
						waitingJob.allocatableSpace.remove(allocS);
						allocS.allocatableJobs.remove(waitingJob);
						j--;
					}
				}
			} else {
				for (int j = 0; j < allocS.allocatableJobs.size(); j++) {
					Job waitingJob = allocS.allocatableJobs.get(j);
					waitingJob.allocatableSpace.remove(allocS);
				}
				allocS.allocatableJobs.clear();
				spaces.remove(allocS);
			}

		}
	}

	private int compareSpace(Space s1, Space s2) {
		int result = Integer.compare(s1.allocatableJobs.size(), s2.allocatableJobs.size());

		if (result == 0) {
			return Long.compare(s1.capcity, s2.capcity);
		} else
			return result;
	}

	private List<Space> getFreeSpace(List<Job> allocated, List<Job> toAllocate, long hyperperiod) {
		List<Space> spaces = new ArrayList<>();
		long startTime = 0;

		for (int i = 0; i < allocated.size(); i++) {
			allocated.get(i).startTime = allocated.get(i).delta;
		}
		allocated.sort((j1, j2) -> Long.compare(j1.startTime, j2.startTime));

		for (int i = 0; i < allocated.size(); i++) {
			Job j = allocated.get(i);
			long startJ = j.delta;
			long endJ = j.idealFinish;

			if (startTime < startJ) {
				Space s = new Space(startTime, startJ);
				spaces.add(s);

				startTime = endJ;
			} else {
				startTime = endJ;
			}
		}

		if (startTime < hyperperiod) {
			Space s = new Space(startTime, hyperperiod);
			spaces.add(s);

			startTime = hyperperiod;
		}

		for (int i = 0; i < spaces.size(); i++) {
			Space s = spaces.get(i);

			for (int j = 0; j < toAllocate.size(); j++) {
				Job job = toAllocate.get(j);

				if (s.end <= job.releaseTime || s.start >= job.deadline)
					continue;

				long possibleStart = Math.max(s.start, job.releaseTime);
				long possibleEnd = Math.min(s.end, job.deadline);

				if (possibleEnd - possibleStart >= job.task.WCET) {
					s.allocatableJobs.add(job);
					job.allocatableSpace.add(s);
				}
			}
		}

		return spaces;
	}

	private int comparatorForCandP(Job j1, Job j2, boolean deadlineFirst) {
		if (!deadlineFirst) {
			int result = -Long.compare(j1.task.WCET, j2.task.WCET);

			if (result == 0) {
				return -Long.compare(j1.task.priority, j2.task.priority);
			} else
				return result;
		} else {
			return -Long.compare(j1.task.priority, j2.task.priority);
		}
	}

	/**************************** LCCD End ****************************/

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
		if (j1.interferingJobs.size() > j2.interferingJobs.size())
			return -1;
		else if (j1.interferingJobs.size() < j2.interferingJobs.size())
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
