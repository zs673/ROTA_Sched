package tsn;

public class Run_RTA_TSN {

	public static void main(String args[]) {

		long t1 = System.currentTimeMillis();

		for (int i = 0; i < 100000; i++) {
			int[][] taskParameters = new int[13][];
			taskParameters[0] = new int[] { 267, 1000, 591, 12, 3 };
			taskParameters[1] = new int[] { 90, 1000, 695, 11, 1 };
			taskParameters[2] = new int[] { 120, 1000, 1000, 10, 2 };
			taskParameters[3] = new int[] { 120, 1000, 1000, 9, 2 };
			taskParameters[4] = new int[] { 120, 1000, 1000, 8, 2 };
			taskParameters[5] = new int[] { 48, 2000, 1203, 7, 1 };
			taskParameters[6] = new int[] { 5, 2000, 1967, 6, 1 };
			taskParameters[7] = new int[] { 532, 20000, 19801, 5, 6 };
			taskParameters[8] = new int[] { 1167, 50000, 25461, 4, 12 };
			taskParameters[9] = new int[] { 9247, 50000, 30622, 3, 93 };
			taskParameters[10] = new int[] { 2270, 100000, 72214, 2, 23 };
			taskParameters[11] = new int[] { 178414, 1000000, 649194, 1, 1785 };
			taskParameters[12] = new int[] { 82631, 1000000, 732163, 0, 827 };

			boolean isSchedulable = new RTA_TSN().schedulabilityTest(taskParameters);

			if (i % 1000 == 0)
				System.out.println(i);
		}

		long t2 = System.currentTimeMillis() - t1;

		System.out.println("time: " + ((double) t2 / (double) 1000));

	}
}
