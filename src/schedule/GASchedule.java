package schedule;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

import entity.Job;
import entity.PeriodicTask;
import ga.Configuration;
import ga.FitnessFunction;
import ga.MOEAD;
import ga.PopulationEntry;
import utils.AnalysisUtils;
import entity.result;
public class GASchedule {

	int population = 300;
	int iteration = 500;
	double mutationRate = 0.5;
	int TOURNAMENT_SIZE = 3;

	public result schedule(List<PeriodicTask> tasks,List<List<Job>>init_jobs, Random rng) {
		Pair<List<Job>, Long> pair = new AnalysisUtils().getJobsInHyperPeriod(tasks);

		List<Job> jobs = pair.getKey();
		long hyperperiod = pair.getValue();

		return runGAsolver(jobs, hyperperiod, init_jobs,rng);
	}

	private result runGAsolver(List<Job> jobs, long hyperPeriod, List<List<Job>>init_jobs,Random rng) {

		FitnessFunction of = new FitnessFunction();
		List<Configuration> initials = generateInitialPopulation(jobs, population, init_jobs, rng);

		MOEAD moead = new MOEAD(jobs, initials, of, iteration, mutationRate, TOURNAMENT_SIZE, rng);
		List<PopulationEntry> bestFront = moead.apply();

		for (int i = 0; i<jobs.size();i++)
		{
			jobs.get(i).startTime = bestFront.get(0).getConfiguration().startTimes.get(i);
		}
		List<List<Double>> pfs = new ArrayList<>();

		for (int i = 0; i < bestFront.size(); i++) {
			List<Double> pf = new ArrayList<>();
			for (int j = 0; j < bestFront.get(i).getObjectives().size(); j++) {
				pf.add(bestFront.get(i).getObjectives().get(j));
			}
			pfs.add(pf);
		}

		//System.out.println(start_time_all);
		//System.out.println(Wcet_all);
		//System.out.println(release_time_all);
		//System.out.println(last_start_time_all);

		boolean isFeasible = false;
		for (int i = 0; i < pfs.size(); i++) {
			if (pfs.get(i).get(0) != Double.MAX_VALUE && pfs.get(i).get(0) != Double.MAX_VALUE) {
				isFeasible = true;
				break;
			}
		}

		//List<Long> start_time_all = new ArrayList<>();
		//List<Long> Wcet_all = new ArrayList<>();
		//List<Long> release_time_all = new ArrayList<>();
		//List<Long> last_start_time_all= new ArrayList<>();

		for (int i = 0; i < jobs.size(); i++) {
			//start_time_all.add(jobs.get(i).startTime);
			//Wcet_all.add(jobs.get(i).task.WCET);
			//release_time_all.add(jobs.get(i).releaseTime);
			//last_start_time_all.add(jobs.get(i).lastStartTime);
			if (jobs.get(i).startTime > jobs.get(i).lastStartTime){
//				System.out.println("Wrong1__time exceed");
				isFeasible = false;
				break;
			}
			if(jobs.get(i).startTime < jobs.get(i).releaseTime){
//				System.out.println("Wrong2__start before release");
				isFeasible = false;
				break;
			}
		}

		if (isFeasible) {
			double maxNumber = 0;
			double maxValue = 0;

			DecimalFormat df = new DecimalFormat("#.##");

			double jobNumber = jobs.size();
			double totalValue = jobs.stream().mapToDouble(j -> j.task.Vmax).sum();

			for (int i = 0; i < pfs.size(); i++) {
				double numberV = Double.parseDouble(df.format((jobNumber - pfs.get(i).get(0)) / jobNumber));
				double qualityV = Double.parseDouble(df.format((totalValue - pfs.get(i).get(1)) / totalValue));

				maxNumber = Math.max(maxNumber, numberV);
				maxValue = Math.max(maxValue, qualityV);

				// pfs.get(i).set(0, numberV);
				// pfs.get(i).set(1, qualityV);
			}
			// pfs.sort((c1, c2) -> -Double.compare(c1.get(0), c2.get(0)));

			List<List<Double>> res = new ArrayList<>();
			List<Double> r = new ArrayList<>();
			r.add(maxNumber);
			r.add(maxValue-0.05);
			res.add(r);

			//System.out.println(res);
			List<List<Double>> pfs0 = new ArrayList<>();
			List<Double> pf = new ArrayList<>();
			pf.add(jobNumber);
			pf.add(totalValue);
			pfs0.add(pf);
			return new result(pfs0,jobs);
		} else
			return null;

	}

	private List<Configuration> generateInitialPopulation(List<Job> jobs, int population,List<List<Job>>init_jobs, Random rng) {
		List<Configuration> initial = new ArrayList<>();
		int num_init = init_jobs.size();
		for (int i = 0; i < population-num_init; i++) {
			List<Long> startTimes = new ArrayList<>();

			for (int j = 0; j < jobs.size(); j++) {
				long startTime = rng.nextInt((int) (jobs.get(j).endQ - jobs.get(j).startQ)) + jobs.get(j).startQ;
				startTimes.add(startTime);
			}

			Configuration config = new Configuration(startTimes);
			initial.add(config);
		}
		for (int i=0;i<num_init;i++) {
			List<Long> startTimes = new ArrayList<>();
			List<Job> job_list = init_jobs.get(i);
			for (int j = 0; j < jobs.size(); j++) {
				Job job = jobs.get(j);
				for (int k=0;k<jobs.size();k++) {
					if (job_list.get(k).task.id ==job.task.id && job_list.get(k).releaseTime ==job.releaseTime ) {
						long startTime = job_list.get(k).startTime;
						startTimes.add(startTime);
					}
				}
			}

			Configuration config = new Configuration(startTimes);
			initial.add(config);
		}
		return initial;
	}

}
