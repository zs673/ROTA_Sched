package ga;

import java.util.ArrayList;
import java.util.List;

import entity.Job;
import entity.Space;
import utils.AnalysisUtils;

public class FitnessFunction {

	private int compareStartTime(Job j1, Job j2) {
		int result = Long.compare(j1.startTime, j2.startTime);

		if (result == 0) {
			return -Long.compare(j1.task.priority, j2.task.priority);
		} else
			return result;
	}

	public List<Double> getFitness(List<Job> allJobs, Configuration config) {
		assert (config.startTimes.size() == allJobs.size());

		List<Job> jobs = new ArrayList<Job>(allJobs);

		for (int i = 0; i < jobs.size(); i++) {
			long startTime = config.startTimes.get(i);
			jobs.get(i).startTime = startTime;
		}
		jobs.sort((j1, j2) -> compareStartTime(j1, j2));

		// for the first job
		Job first = jobs.get(0);
		Job second = jobs.get(1);

		if (first.delta < first.startTime)
			first.startTime = first.delta;
		if (first.delta > first.startTime && first.delta + first.task.WCET <= second.startTime)
			first.startTime = first.delta;
		if (first.delta > first.startTime && first.delta + first.task.WCET > second.startTime && first.startTime + first.task.WCET < second.startTime)
			first.startTime = second.startTime - first.task.WCET;

		// for the middle ones.
		for (int i = 1; i < jobs.size() - 1; i++) {
			Job preJ = jobs.get(i - 1);
			Job lateJ = jobs.get(i + 1);
			Job job = jobs.get(i);

			if (preJ.startTime + preJ.task.WCET > job.startTime)
				job.startTime = preJ.startTime + preJ.task.WCET;
			// we have some space to adjust between preJ and J.
			// if the delta of J is in the adjust range
			if (job.delta < job.startTime && job.delta >= preJ.startTime + preJ.task.WCET)
				job.startTime = job.delta;
			// if the delta of J is before the end time of previous J
			if (job.delta < job.startTime && job.delta < preJ.startTime + preJ.task.WCET)
				job.startTime = preJ.startTime + preJ.task.WCET;
			// if the delta of J is after the start time of J and is before the
			// start time of lateJ, and can be shifted
			if (job.delta > job.startTime && job.delta + job.task.WCET <= lateJ.startTime)
				job.startTime = job.delta;
			// At last, if the delta of J is after the start time of
			// lateJ, and can be shifted
			if (job.delta > job.startTime && job.delta + job.task.WCET > lateJ.startTime && job.startTime + job.task.WCET < lateJ.startTime)
				job.startTime = lateJ.startTime - job.task.WCET;
		}

		// for the last job
		Job last = jobs.get(jobs.size() - 1);
		Job secondLast = jobs.get(jobs.size() - 2);

		if (secondLast.startTime + secondLast.task.WCET > last.startTime)
			last.startTime = secondLast.startTime + secondLast.task.WCET;
		if (last.delta < last.startTime && last.delta >= secondLast.startTime + secondLast.task.WCET)
			last.startTime = last.delta;
		if (last.delta < last.startTime && last.delta < secondLast.startTime + secondLast.task.WCET)
			last.startTime = secondLast.startTime + secondLast.task.WCET;
		if (last.startTime < last.delta)
			last.startTime = last.delta;

		// find all tasks that miss deadline
		List<Job> deadlineMissingJobs = new ArrayList<>();
		for (int i = 0; i < jobs.size(); i++) {
			if (jobs.get(i).startTime + jobs.get(i).task.WCET > jobs.get(i).deadline) {
				deadlineMissingJobs.add(jobs.get(i));
				jobs.remove(i);
				i--;
			}
		}

		assert (jobs.size() + deadlineMissingJobs.size() == allJobs.size());

		// allocate for deadline missed tasks
		List<Job> allocated = jobs;
		List<Job> toAlloc = deadlineMissingJobs;
		List<Job> finalJobs = allocate(allocated, toAlloc);

		if (finalJobs == null) {
			List<Double> fitness = new ArrayList<>();
			fitness.add(Double.MAX_VALUE);
			fitness.add(Double.MAX_VALUE);
			return fitness;
		}

		assert (jobs.size() == finalJobs.size());

		// get fitness
		int numOfExact = 0;
		double totalValue = 0;

		for (int i = 0; i < finalJobs.size(); i++) {
			Job j = finalJobs.get(i);
			assert (j.startTime >= 0);
			if (j.delta == j.startTime)
				numOfExact++;
			totalValue += new AnalysisUtils().getValue(j);
		}

		double exact = finalJobs.size() - numOfExact;

		// DecimalFormat df = new DecimalFormat("#.##");
		totalValue = finalJobs.stream().mapToDouble(j -> j.task.Vmax).sum() - totalValue;

		// update configuration
		for (int i = 0; i < allJobs.size(); i++) {
			Job j = allJobs.get(i);

			long startTime = finalJobs.get(finalJobs.indexOf(j)).startTime;
			config.startTimes.set(i, startTime);
		}

		List<Double> fitness = new ArrayList<>();
		fitness.add(exact);
		fitness.add(totalValue);
		return fitness;
	}

	private List<Job> allocate(List<Job> allocated, List<Job> toAlloc) {
		List<Space> spaces = getFreeSpace(allocated, toAlloc, allocated.get(0).hyperPeriod);

		fitAllocatableTasks(allocated, toAlloc, spaces);

//		spaces.sort((s1, s2) -> Long.compare(s1.start, s2.start));
//
//		for (int i = 0; i < toAlloc.size(); i++) {
//			boolean result = allocJob(toAlloc.get(i), allocated, toAlloc, spaces);
//
//			if (!result)
//				return null;
//			else
//				i--;
//		}

		if (toAlloc.size() > 0) {
			return null;
		} else
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

		// long restRequiredSpace = job.task.WCET -
		// solution.stream().mapToLong(s -> s.capcity).sum() + lastS.capcity;

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

				// if (spaces.size() == 1)
				// System.err.print("We have a spaces size = 1");
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

	private void fitAllocatableTasks(List<Job> allocated, List<Job> toAlloc, List<Space> spaces) {

		toAlloc.sort((j1, j2) -> -Long.compare(j1.task.priority, j2.task.priority));

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

		allocated.sort((j1, j2) -> Long.compare(j1.startTime, j2.startTime));

		for (int i = 0; i < allocated.size(); i++) {
			Job j = allocated.get(i);
			long startJ = j.startTime;
			long endJ = j.startTime + j.task.WCET;

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

	// private boolean allocJob(Job job, List<Job> allocated, List<Job> toAlloc,
	// List<Space> spaces) {
	// allocated.sort((j1, j2) -> Long.compare(j1.startTime, j2.startTime));
	//
	// /*
	// * Get spaces in range
	// */
	// long release = job.releaseTime;
	// long deadline = job.deadline;
	//
	// for (int j = 0; j < spaces.size(); j++) {
	// Space s = spaces.get(j);
	// long start = s.start;
	// long end = s.end;
	//
	// if (release >= end || deadline <= start) {
	// } else
	// job.spaceInRange.add(s);
	// }
	//
	// if (job.spaceInRange.size() == 0)
	// return false;
	//
	// /*
	// * Check whether spaces is able to fit the job
	// */
	// long startTime = Math.max(job.spaceInRange.get(0).start,
	// job.releaseTime);
	// long endTime = Math.min(job.spaceInRange.get(job.spaceInRange.size() -
	// 1).end, job.deadline);
	//
	// long sumSpace = 0;
	// for (int j = 0; j < job.spaceInRange.size(); j++) {
	// Space s = job.spaceInRange.get(j);
	// if (j == 0)
	// sumSpace += s.end - startTime;
	// else if (j == job.spaceInRange.size() - 1)
	// sumSpace += endTime - s.start;
	// else
	// sumSpace += s.end - s.start;
	// }
	//
	// if (sumSpace < job.task.WCET)
	// return false;
	//
	// /*
	// * Get the solution with minimized impact to timing-accurate task
	// */
	// List<Space> solution = getSpacesForAllocation(job, allocated, spaces);
	// if (solution == null)
	// return false;
	//
	// /*
	// * Allocate Job by shifting the allocated jobs.
	// */
	// job.startTime = solution.get(0).start;
	//
	// /*
	// * Update task
	// */
	// toAlloc.remove(job);
	// allocated.add(job);
	//
	// /*
	// * Update space
	// */
	// for (int i = 0; i < solution.size() - 1; i++) {
	// spaces.remove(solution.get(i));
	// }
	//
	// Space lastS = solution.get(solution.size() - 1);
	// long restRequiredSpace = job.task.WCET - solution.stream().mapToLong(s ->
	// s.capcity).sum() + lastS.capcity;
	//
	// if (restRequiredSpace == lastS.capcity)
	// spaces.remove(lastS);
	// else {
	// lastS.start += restRequiredSpace;
	// lastS.capcity = lastS.end - lastS.start;
	// }
	//
	// return true;
	// }
	//
	// private List<Space> getSpacesForAllocation(Job job, List<Job>
	// allocateJobs, List<Space> allSpaces) {
	// List<List<Space>> solutions = new ArrayList<>();
	//
	// for (int i = 0; i < job.spaceInRange.size() - 1; i++) {
	// long space = 0;
	// int index = i;
	//
	// while (space < job.task.WCET && index < job.spaceInRange.size()) {
	// space += Math.min(job.deadline, job.spaceInRange.get(index).end) -
	// Math.max(job.releaseTime, job.spaceInRange.get(index).start);
	// index++;
	// }
	//
	// if (space >= job.task.WCET) {
	// List<Space> spaces = new ArrayList<>();
	// for (int j = i; j < index; j++) {
	// spaces.add(job.spaceInRange.get(j));
	// }
	// solutions.add(spaces);
	//
	// if (spaces.size() == 1)
	// System.err.print("We have a spaces size = 1");
	// }
	// }
	//
	// List<Integer> weight = new ArrayList<>();
	//
	// for (int i = 0; i < solutions.size(); i++) {
	// List<Space> oneSolution = solutions.get(i);
	// // long lastShift = ;
	//
	// boolean isFeasible = true;
	// int scarifice = 0;
	// long requiredSpace = job.task.WCET;
	//
	// outerloop: for (int j = 0; j < oneSolution.size() - 1; j++) {
	// Space s1 = oneSolution.get(j);
	// Space s2 = oneSolution.get(j + 1);
	//
	// requiredSpace -= oneSolution.get(j).capcity;
	//
	// List<Job> jobsInBetween = getJobsInBtween(s1, s2, allocateJobs);
	// for (int k = 0; k < jobsInBetween.size(); k++) {
	// Job shiftedJob = jobsInBetween.get(k);
	// if (shiftedJob.startTime + requiredSpace + shiftedJob.task.WCET >
	// shiftedJob.deadline) {
	// isFeasible = false;
	// break outerloop;
	// }
	//
	// if (shiftedJob.delta == shiftedJob.startTime)
	// scarifice++;
	// }
	// }
	//
	// if (!isFeasible) {
	// solutions.remove(i);
	// i--;
	// } else {
	// weight.add(scarifice);
	// }
	// }
	//
	// if (solutions.size() == 0)
	// return null;
	//
	// assert (solutions.size() == weight.size());
	//
	// int index = -1;
	// int minWeight = Integer.MAX_VALUE;
	//
	// for (int i = 0; i < solutions.size(); i++) {
	// if (minWeight > weight.get(i)) {
	// minWeight = weight.get(i);
	// index = i;
	// }
	// }
	//
	// return solutions.get(index);
	// }
	//
	// private List<Job> getJobsInBtween(Space s1, Space s2, List<Job>
	// allocatedJobs) {
	// long start = Math.min(s1.start, s2.start);
	// long end = Math.max(s1.end, s2.end);
	//
	// List<Job> jobsInBetween = new ArrayList<>();
	//
	// for (int i = 0; i < allocatedJobs.size(); i++) {
	// if (allocatedJobs.get(i).startTime >= start &&
	// allocatedJobs.get(i).startTime + allocatedJobs.get(i).task.WCET <= end)
	// jobsInBetween.add(allocatedJobs.get(i));
	// }
	//
	// return jobsInBetween;
	// }
	//
	// private void fitAllocatableTasks(List<Job> allocated, List<Job> toAlloc,
	// List<Space> spaces) {
	//
	// toAlloc.sort((j1, j2) -> -Long.compare(j1.task.priority,
	// j2.task.priority));
	//
	// for (int i = 0; i < toAlloc.size(); i++) {
	// Job job = toAlloc.get(i);
	//
	// List<Space> allowedSpaces = job.allocatableSpace;
	//
	// if (allowedSpaces.size() == 0) {
	// continue;
	// }
	//
	// allowedSpaces.sort((s1, s2) -> compareSpace(s1, s2));
	//
	// Space allocS = allowedSpaces.get(0);
	// job.startTime = allocS.start;
	// allocS.start += job.task.WCET;
	// allocS.capcity = allocS.end - allocS.start;
	// job.allocatableSpace.clear();
	//
	// allocated.add(job);
	// toAlloc.remove(job);
	// i--;
	//
	// if (allocS.start < allocS.end) {
	// allocS.allocatableJobs.remove(job);
	// for (int j = 0; j < allocS.allocatableJobs.size(); j++) {
	// Job waitingJob = allocS.allocatableJobs.get(j);
	//
	// boolean canAlloc = true;
	//
	// if (allocS.end <= waitingJob.releaseTime || allocS.start >=
	// waitingJob.deadline)
	// canAlloc = false;
	//
	// long possibleStart = Math.max(allocS.start, waitingJob.releaseTime);
	// long possibleEnd = Math.min(allocS.end, waitingJob.deadline);
	// if (possibleEnd - possibleStart < waitingJob.task.WCET) {
	// canAlloc = false;
	// }
	//
	// if (!canAlloc) {
	// waitingJob.allocatableSpace.remove(allocS);
	// allocS.allocatableJobs.remove(waitingJob);
	// }
	// }
	// } else {
	// for (int j = 0; j < allocS.allocatableJobs.size(); j++) {
	// Job waitingJob = allocS.allocatableJobs.get(j);
	// waitingJob.allocatableSpace.remove(allocS);
	// }
	// allocS.allocatableJobs.clear();
	// spaces.remove(allocS);
	// }
	// }
	// }
	//
	// private int compareSpace(Space s1, Space s2) {
	// int result = Integer.compare(s1.allocatableJobs.size(),
	// s2.allocatableJobs.size());
	//
	// if (result == 0) {
	// return Long.compare(s1.capcity, s2.capcity);
	// } else
	// return result;
	// }
	//
	// private List<Space> getFreeSpace(List<Job> allocated, List<Job>
	// toAllocate, long hyperperiod) {
	// List<Space> spaces = new ArrayList<>();
	// long startTime = 0;
	//
	// for (int i = 0; i < allocated.size(); i++) {
	// Job j = allocated.get(i);
	// long startJ = j.startTime;
	// long endJ = j.startTime + j.task.WCET;
	//
	// if (startTime < startJ) {
	// Space s = new Space(startTime, startJ);
	// spaces.add(s);
	//
	// startTime = endJ;
	// } else {
	// startTime = endJ;
	// }
	// }
	//
	// if (startTime < hyperperiod) {
	// Space s = new Space(startTime, hyperperiod);
	// spaces.add(s);
	//
	// startTime = hyperperiod;
	// }
	//
	// for (int i = 0; i < spaces.size(); i++) {
	// Space s = spaces.get(i);
	//
	// for (int j = 0; j < toAllocate.size(); j++) {
	// Job job = toAllocate.get(j);
	//
	// if (s.end <= job.releaseTime || s.start >= job.deadline)
	// continue;
	//
	// long possibleStart = Math.max(s.start, job.releaseTime);
	// long possibleEnd = Math.min(s.end, job.deadline);
	//
	// if (possibleEnd - possibleStart >= job.task.WCET) {
	// s.allocatableJobs.add(job);
	// job.allocatableSpace.add(s);
	// }
	// }
	// }
	//
	// return spaces;
	// }

}
