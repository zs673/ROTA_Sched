package entity;

import java.util.ArrayList;
import java.util.List;

public class Job {

	public PeriodicTask task;
	public long hyperPeriod;

	public long releaseTime;
	public long deadline;
	public long lastStartTime;
	public long delta;
	public long idealFinish;
	public int numberOfRelease;

	public long startQ;
	public long endQ;

	public long startTime = -1;
	public double qulaity = -1;
	public long rWCET;
	public long Max_interference=0;
	public List<Job> interferingJobs = new ArrayList<>();
	public List<Space> allocatableSpace = new ArrayList<>();
	public List<Space> spaceInRange = new ArrayList<>();
	public List<ETS>   allocatableETS= new ArrayList<>();

	public List<ETS>   ETSInRange= new ArrayList<>();
	public List<ETS>   ETSInRange0= new ArrayList<>();

	public Job(int numberOfRelease, PeriodicTask task, long hyperPeriod) {
		this.numberOfRelease = numberOfRelease;
		this.releaseTime = task.period * numberOfRelease;
		this.deadline = releaseTime + task.deadline;
		this.lastStartTime = this.deadline - task.WCET;
		this.delta = releaseTime + task.delta;
		this.idealFinish = this.delta + task.WCET;

		this.task = task;

		this.startQ = delta - this.task.theta;
		this.endQ = delta + this.task.theta;

		this.hyperPeriod = hyperPeriod;
		this.rWCET=this.task.WCET;
	}

	@Override
	public String toString() {
		return "ID: " + task.id + "_" + numberOfRelease + ", releaseTime: " + releaseTime + ", deadline: " + deadline + ", delta: " + delta + ", idealFinish: "
				+ idealFinish + " Start: " + startTime + ", finish: " + (startTime + task.WCET);
	}

	public void clear () {
		startTime = -1;
		qulaity = -1;

		interferingJobs = new ArrayList<>();
		allocatableSpace = new ArrayList<>();
		spaceInRange = new ArrayList<>();
	}
}