package generationTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import entity.PeriodicTask;

public class SimpleSystemGenerator {

	public boolean isLogUni;
	public int maxT;
	public int minT;

	public int total_tasks;
	public double totalUtil;
	public double valueRange;

	public int lcm;
	public boolean useLCM;

	boolean print;
	Random ran;

	public SimpleSystemGenerator(int minT, int maxT, int lcm, int totalTasks, double totalUtil, boolean isPeriodLogUni, double valueRange, int seed,
			boolean useLCM) {
		this.minT = minT;
		this.maxT = maxT;
		this.totalUtil = totalUtil;
		this.total_tasks = totalTasks;
		this.isLogUni = isPeriodLogUni;
		this.valueRange = valueRange;
		this.print = false;
		this.ran = new Random(seed);

		this.lcm = lcm;
		this.useLCM = useLCM;
	}

	/*
	 * generate task sets for multiprocessor fully partitioned fixed-priority
	 * system
	 */
	public ArrayList<PeriodicTask> generateTasks() {
		ArrayList<PeriodicTask> tasks = null;
		while (tasks == null) {
			tasks = generateT();
		}
		Random r=new Random(1000);
		for(int i=0;i<tasks.size();i++)
		{
			tasks.get(i).Vmax=tasks.get(i).util*100*r.nextInt(100);
			tasks.get(i).Vmin=tasks.get(i).Vmax/(Math.abs(r.nextInt(8))+2);
		}
		return tasks;
	}

	private ArrayList<PeriodicTask> generateT() {
		int task_id = 1;
		ArrayList<PeriodicTask> tasks = new ArrayList<>(total_tasks);
		ArrayList<Long> periods = new ArrayList<>(total_tasks);
		
		List<Long> lcmPeriods = new ArrayList<>();

		for (long i = 1; i <= lcm; ++i) {
			if (lcm % i == 0 && i >= 10) {
				lcmPeriods.add(i);
			}
		}
		
//		System.out.println("total period: " + lcmPeriods.size() + ", " + lcmPeriods);

		/* generates random periods */
		while (true) {
			if (useLCM) {
				if (lcmPeriods.size() <= total_tasks) {
					System.err.println("not enough periods by LCM");
					System.exit(-1);
				}

				long period = lcmPeriods.get(ran.nextInt(lcmPeriods.size()));
				if (!periods.contains(period))
					periods.add(period);

			} else {
				if (!isLogUni) {
					long period = (ran.nextInt(maxT - minT) + minT) * 10;
					if (!periods.contains(period))
						periods.add(period);
				} else {
					double a1 = Math.log(minT);
					double a2 = Math.log(maxT + 1);
					double scaled = ran.nextDouble() * (a2 - a1);
					double shifted = scaled + a1;
					double exp = Math.exp(shifted);

					int result = (int) exp;
					result = Math.max(minT, result);
					result = Math.min(maxT, result);

					long period = result * 10;
					if (!periods.contains(period))
						periods.add(period);
				}
			}

			if (periods.size() >= total_tasks)
				break;
		}
		periods.sort((p1, p2) -> Double.compare(p1, p2));
		
//		System.out.println("Periods generated.");

		/* generate utils */
		UUnifastDiscard unifastDiscard = new UUnifastDiscard(totalUtil, total_tasks, 1000, ran);
		ArrayList<Double> utils = null;
		while (true) {
			utils = unifastDiscard.getUtils();

			double tt = 0;
			for (int i = 0; i < utils.size(); i++) {
				tt += utils.get(i);
			}

			if (utils != null)
				if (utils.size() == total_tasks && tt <= totalUtil)
					break;
		}
		if (print) {
			System.out.print("task utils: ");
			double tt = 0;
			for (int i = 0; i < utils.size(); i++) {
				tt += utils.get(i);
				System.out.print(tt + "   ");
			}
			System.out.println("\n total uitls: " + tt);
		}
		
//		System.out.println("Utilisation generated.");
Random valueseed=new Random(1000);
		/* generate sporadic tasks */
		for (int i = 0; i < utils.size(); i++) {
			long computation_time = (long) (periods.get(i) * utils.get(i));
			if (computation_time == 0) {
				return null;
			}
			long period = periods.get(i); // min 1000
			long valuePeriod = (long) (period * valueRange);
			long startingDelta = valuePeriod / 2;
			long endDetla = Math.min(period - valuePeriod / 2, period - computation_time);
			long delta = ran.nextInt((int) Math.abs(endDetla - startingDelta)) + Math.min(startingDelta, endDetla);
			long theta = valuePeriod / 2;

			PeriodicTask t = new PeriodicTask(-1, periods.get(i), periods.get(i), computation_time, task_id, delta, theta, -1, 1, utils.get(i),Math.abs(valueseed.nextInt()%5));
			task_id++;
			tasks.add(t);
		}

		new PriorityGeneator().assignPandQbyDMPO(tasks);
		tasks.sort((p1, p2) -> -Double.compare(p1.priority, p2.priority));

		return tasks;
	}

}
