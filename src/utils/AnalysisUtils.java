package utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import entity.Job;
import entity.PeriodicTask;

public class AnalysisUtils {
	public static final int MAX_PRIORITY = 1000;

	public double getValue(Job job) {
		double Vmin = job.task.Vmin;
		double Vmax = job.task.Vmax;

		long start = job.startTime;

		long v1 = job.delta - job.task.theta;
		long delta = job.delta;
		long v2 = Math.min(delta + job.task.theta, job.deadline);

		double sign1=job.task.theta;
		switch (job.task.valueseed) {
			case 0:
			{
				if (v1 < start && start < delta)
				{   double x=1-(delta-start)/sign1;
					double value0= (Vmax - Vmin)*Math.pow(x,1) + Vmin;
					return value0;
				}

				else if (start == delta)
					return Vmax;
				else if (delta < start && start < v2)
				{   double x=1-(start-delta)/sign1;
					double value0= (Vmax - Vmin)*Math.pow(x,1) + Vmin;
					return value0;
				}
				else if(start>job.deadline)
					return 0;
				else
					return Vmin;
			}

			case 1:
			{
				if (v1 < start && start < delta)
				{   double x=(delta-start)/sign1;
					double sigmoid=2/ ( 1 + Math.exp(x*100));
					double value1= (Vmax - Vmin)*sigmoid + Vmin;
					return value1;
				}
				else if (start == delta)
					return Vmax;
				else if (delta < start && start < v2)
				{double x=(start-delta)/sign1;
					double sigmoid=2/ ( 1 + Math.exp(x*100));
					double value1= (Vmax - Vmin)*sigmoid + Vmin;
					return value1;
				}
				else if(start>job.deadline)
					return 0;
				else
					return Vmin;
			}

			case 2:
			{
				if (v1 < start && start < delta)
					return Vmin;
				else if (start == delta)
					return Vmax;
				else if (delta < start && start < v2)
				{double x=(start-delta)/sign1;
				double sigmoid=2/ ( 1 + Math.exp(x*100));
				double value2= (Vmax - Vmin)*sigmoid + Vmin;
				return value2;
				}
				else if(start>job.deadline)
					return 0;
				else
					return Vmin;
			}

			case 3:
			{
				if (v1 < start && start < delta)
				{   double x=(delta-start)/sign1;
					double sigmoid=2/ ( 1 + Math.exp(x*100));
					double value3= (Vmax - Vmin)*sigmoid + Vmin;
					return value3;
				}
				else if (start == delta)
					return Vmax;
				else if (delta < start && start < v2)
					return Vmin;
				else if(start>job.deadline)
					return 0;
				else
					return Vmin;
			}

			case 4:
			{
				if (v1 < start && start < delta)
				{    double x=(start-v1)/sign1;
					double sinx=Math.abs(Math.sin(x*20.42035));
					double value4=(Vmax - Vmin)*sinx*x+ Vmin;
					if(value4>Vmax)
						System.out.println(" ");
					return value4;
				}
				else if (start == delta)
					return Vmax;
				else if (delta < start && start < v2)
				{    double x=(v2-start)/sign1;
					double sinx=Math.abs(Math.sin(x*20.42035));
					double value4=(Vmax - Vmin)*sinx*x+ Vmin;
					if(value4>Vmax)
						System.out.println(" ");
					return value4;
				}
				else if(start>job.deadline)
					return 0;
				else
					return Vmin;
			}

			case 5:
			{
				if (v1 < start && start < delta)
				{   double x=1-(delta-start)/sign1;
					double value5= (Vmax - Vmin)*Math.pow(x,2) + Vmin;
					return value5;
				}

				else if (start == delta)
					return Vmax;
				else if (delta < start && start < v2)
				{   double x=1-(start-delta)/sign1;
					double value5= (Vmax - Vmin)*Math.pow(x,2) + Vmin;
					return value5;
				}
				else if(start>job.deadline)
					return 0;
				else
					return Vmin;
			}

		}
		return Vmin;
	}

	public Pair<List<Job>, Long> getJobsInHyperPeriod(List<PeriodicTask> tasks) {
		Long[] periods = tasks.stream().map(t -> t.period).toArray(Long[]::new);
		long hyperPeriod = lcm_of_array_elements(periods);

		// System.out.println("hyper-period: " + hyperPeriod);

		List<Job> jobs = new ArrayList<>();

		for (int i = 0; i < tasks.size(); i++) {
			long numberOfJobs = hyperPeriod / tasks.get(i).period;

			for (int j = 0; j < numberOfJobs; j++) {
				Job job = new Job(j, tasks.get(i), hyperPeriod);
				jobs.add(job);
			}
		}

		return new Pair<List<Job>, Long>(jobs, hyperPeriod);
	}

	private long lcm_of_array_elements(Long[] element_array) {
		long lcm_of_array_elements = 1;
		int divisor = 2;

		while (true) {
			int counter = 0;
			boolean divisible = false;

			for (int i = 0; i < element_array.length; i++) {
				if (element_array[i] == 0) {
					return 0;
				} else if (element_array[i] < 0) {
					element_array[i] = element_array[i] * (-1);
				}
				if (element_array[i] == 1) {
					counter++;
				}

				if (element_array[i] % divisor == 0) {
					divisible = true;
					element_array[i] = element_array[i] / divisor;
				}
			}

			if (divisible) {
				lcm_of_array_elements = lcm_of_array_elements * divisor;
			} else {
				divisor++;
			}

			if (counter == element_array.length) {
				return lcm_of_array_elements;
			}
		}
	}

	public long[] initResponseTime(ArrayList<PeriodicTask> tasks) {
		long[] response_times = new long[tasks.size()];

		tasks.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
		long[] Ri = new long[tasks.size()];

		for (int i = 0; i < tasks.size(); i++) {

			PeriodicTask t = tasks.get(i);
			Ri[i] = t.Ri = t.WCET;

		}
		return response_times;
	}

	public boolean isSystemSchedulable(ArrayList<PeriodicTask> tasks, long[] Ris) {
		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).deadline < Ris[i])
				return false;
		}
		return true;
	}

	public void cloneList(long[] oldList, long[] newList) {
		for (int i = 0; i < oldList.length; i++) {
			newList[i] = oldList[i];
		}
	}

	public boolean isArrayContain(int[] array, int value) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == value)
				return true;
		}
		return false;
	}

	public static void main(String[] args) {
//		System.out.println(GLPK.glp_version());

		List<Integer> numbers = new ArrayList<>();

		for (int number = 1440000; number <= 1440000; number++) {
			int count = 0;

			for (int i = 1; i <= number; ++i) {
				if (number % i == 0 && i >= 10)
					count++;

			}

			if (count >= 20)
				numbers.add(number);
		}

		for (int j = 0; j < numbers.size(); j++) {
			int number = numbers.get(j);
			int count = 0;
			System.out.print("Factors of " + number + " are: ");

			for (int i = 1; i <= number; ++i) {
				if (number % i == 0 && i >= 10) {
					System.out.print(i + " ");
					count++;
				}
			}

			System.out.println("\n LCM: " + number + " " + "count: " + count);
		}

	}

}

/**
 * Factors of 720 are: 10 12 15 16 18 20 24 30 36 40 45 48 60 72 80 90 120 144
 * 180 240 360 720 LCM: 720 count: 22 Factors of 840 are: 10 12 14 15 20 21 24
 * 28 30 35 40 42 56 60 70 84 105 120 140 168 210 280 420 840 LCM: 840 count: 24
 * Factors of 900 are: 10 12 15 18 20 25 30 36 45 50 60 75 90 100 150 180 225
 * 300 450 900 LCM: 900 count: 20 Factors of 960 are: 10 12 15 16 20 24 30 32 40
 * 48 60 64 80 96 120 160 192 240 320 480 960 LCM: 960 count: 21 Factors of 1008
 * are: 12 14 16 18 21 24 28 36 42 48 56 63 72 84 112 126 144 168 252 336 504
 * 1008 LCM: 1008 count: 22 Factors of 1080 are: 10 12 15 18 20 24 27 30 36 40
 * 45 54 60 72 90 108 120 135 180 216 270 360 540 1080 LCM: 1080 count: 24
 * Factors of 1200 are: 10 12 15 16 20 24 25 30 40 48 50 60 75 80 100 120 150
 * 200 240 300 400 600 1200 LCM: 1200 count: 23 Factors of 1260 are: 10 12 14 15
 * 18 20 21 28 30 35 36 42 45 60 63 70 84 90 105 126 140 180 210 252 315 420 630
 * 1260 LCM: 1260 count: 28 Factors of 1320 are: 10 11 12 15 20 22 24 30 33 40
 * 44 55 60 66 88 110 120 132 165 220 264 330 440 660 1320 LCM: 1320 count: 25
 * Factors of 1344 are: 12 14 16 21 24 28 32 42 48 56 64 84 96 112 168 192 224
 * 336 448 672 1344 LCM: 1344 count: 21 Factors of 1440 are: 10 12 15 16 18 20
 * 24 30 32 36 40 45 48 60 72 80 90 96 120 144 160 180 240 288 360 480 720 1440
 * LCM: 1440 count: 28 Factors of 1512 are: 12 14 18 21 24 27 28 36 42 54 56 63
 * 72 84 108 126 168 189 216 252 378 504 756 1512 LCM: 1512 count: 24 Factors of
 * 1560 are: 10 12 13 15 20 24 26 30 39 40 52 60 65 78 104 120 130 156 195 260
 * 312 390 520 780 1560 LCM: 1560 count: 25 Factors of 1584 are: 11 12 16 18 22
 * 24 33 36 44 48 66 72 88 99 132 144 176 198 264 396 528 792 1584 LCM: 1584
 * count: 23 Factors of 1620 are: 10 12 15 18 20 27 30 36 45 54 60 81 90 108 135
 * 162 180 270 324 405 540 810 1620 LCM: 1620 count: 23 Factors of 1680 are: 10
 * 12 14 15 16 20 21 24 28 30 35 40 42 48 56 60 70 80 84 105 112 120 140 168 210
 * 240 280 336 420 560 840 1680 LCM: 1680 count: 32 Factors of 1728 are: 12 16
 * 18 24 27 32 36 48 54 64 72 96 108 144 192 216 288 432 576 864 1728 LCM: 1728
 * count: 21 Factors of 1764 are: 12 14 18 21 28 36 42 49 63 84 98 126 147 196
 * 252 294 441 588 882 1764 LCM: 1764 count: 20 Factors of 1800 are: 10 12 15 18
 * 20 24 25 30 36 40 45 50 60 72 75 90 100 120 150 180 200 225 300 360 450 600
 * 900 1800 LCM: 1800 count: 28 Factors of 1848 are: 11 12 14 21 22 24 28 33 42
 * 44 56 66 77 84 88 132 154 168 231 264 308 462 616 924 1848 LCM: 1848 count:
 * 25 Factors of 1872 are: 12 13 16 18 24 26 36 39 48 52 72 78 104 117 144 156
 * 208 234 312 468 624 936 1872 LCM: 1872 count: 23 Factors of 1890 are: 10 14
 * 15 18 21 27 30 35 42 45 54 63 70 90 105 126 135 189 210 270 315 378 630 945
 * 1890 LCM: 1890 count: 25 Factors of 1920 are: 10 12 15 16 20 24 30 32 40 48
 * 60 64 80 96 120 128 160 192 240 320 384 480 640 960 1920 LCM: 1920 count: 25
 * Factors of 1980 are: 10 11 12 15 18 20 22 30 33 36 44 45 55 60 66 90 99 110
 * 132 165 180 198 220 330 396 495 660 990 1980 LCM: 1980 count: 29
 */
