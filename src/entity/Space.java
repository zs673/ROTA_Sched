package entity;

import java.util.ArrayList;
import java.util.List;

public class Space {
	public long start;
	public long end;
	public long capcity;

	public List<Job> allocatableJobs = new ArrayList<>();
	public List<Job> Jobsinrange = new ArrayList<>();

	public Space(long start, long end) {

		this.start = start;
		this.end = end;
		this.capcity = end - start;
	}
	
	@Override
	public String toString() {
		return "start: " + start + ", end: " + end + ", capcity: " + capcity;
	}

}