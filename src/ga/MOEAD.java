package ga;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import entity.Job;

/**
 * This class provide the abstraction towards various versions of MOEA\D
 * algorithms.
 */

public class MOEAD {
	List<Job> jobs;

	/********************* Configurable MOEA/D Parameters *********************/

	/** The size of the external population i.e., the Pareto Front */
	static int SIZE_OF_EP = 100;

	/******************
	 * Configurable MOEA/D Parameters Ends
	 ********************/

	/************************ Fixed MOEA/D Parameters *************************/

	/* Weight vectors */
	List<double[]> lambda;

	/* The neighborhood of each individual */
	// List<int[]> neighborhood;

	/*
	 * The fitness value of each individual calculated by a certain
	 * decomposition approach. This implementation supports the Weighted Sum and
	 * Tchebycheff decomposition approaches described in Zhang & Li paper.
	 */
	List<Double> fitness;

	/*
	 * The notation of Z in in Zhang & Li paper, stores the best results of each
	 * objective.
	 */
	// List<Double> idealPoint;

	/*
	 * The external population stores the non-dominated solutions (PF) found
	 * during the search. The size of this list is strictly limited to @param
	 * SIZE_OF_EP.
	 */
	List<PopulationEntry> externalPopulation;

	/********************** Fixed MOEA/D Parameters Ends **********************/

	/************************** Generic GA Parameters *************************/
	int populationSize;
	int numberOfObjectives;
	int tournamentSize;

	Random rng;
	List<Configuration> initialPopulation;

	public List<PopulationEntry> currentPopulation;

	FitnessFunction of;

	int iteration;
	double pm;

	/************************
	 * Generic GA Parameters Ends
	 ***********************/

	public MOEAD(List<Job> jobs, List<Configuration> initialPopulation, FitnessFunction of, int iteration, double pm, int tournamentSize, Random rng) {
		this.jobs = jobs;
		this.initialPopulation = initialPopulation;
		this.iteration = iteration;
		this.pm = pm;

		this.of = of;

		this.populationSize = initialPopulation.size();
		this.numberOfObjectives = 2;
		this.tournamentSize = tournamentSize;
		this.rng = rng;

		this.lambda = new ArrayList<double[]>(populationSize);
		this.fitness = new ArrayList<Double>(populationSize);
		this.externalPopulation = new ArrayList<PopulationEntry>();
	}

	/**
	 * Generates weights according to a uniform design of mixtures using the
	 * Hammersley low-discrepancy sequence generator. This algorithm is
	 * implemented by David Hadka from the MOEAFramework project at
	 * https://github.com/dhadka/MOEAFramework.
	 */
	void initializeUniformWeight() {
		for (int n = 0; n < populationSize; n++) {
			double a = 1.0 * n / (populationSize - 1);

			lambda.add(new double[] { 0, 1});
		}
		return;
	}

	/**
	 * Here we compute the fitness value for each individual in the given
	 * population via the defined decomposition approach.
	 */
	double getFitness(PopulationEntry individual, int index) {
		double Fitness = 0;

		for (int i = 0; i < individual.getObjectives().size(); i++) {
			Fitness += individual.getObjectives().get(i) * lambda.get(index)[i];
		}

		return Fitness;
	}

	void getFitnessAll(List<PopulationEntry> population) {
		for (int i = 0; i < populationSize; i++)
			fitness.add(getFitness(population.get(i), i));
	}

	/**
	 * Return true if @param individual1 strictly dominates @param individual2
	 * on each objective. Return false otherwise.
	 */
	boolean dominate(PopulationEntry individual1, PopulationEntry individual2) {
		boolean isDominate = false;

		if (individual1.getObjectives().size() != individual2.getObjectives().size()) {
			System.out.println("error");
		}

		for (int i = 0; i < individual1.getObjectives().size(); i++) {
			if (individual1.getObjectives().get(i) > individual2.getObjectives().get(i))
				return false;
			if (individual1.getObjectives().get(i) <= individual2.getObjectives().get(i))
				isDominate = true;
		}

		return isDominate;
	}

	/**
	 * Update the external population list. This list will be returned as the
	 * final optimization result. A new @param candidate can join into the list
	 * if and only if no members in the list can dominate the @param candidate.
	 * The members that are dominated by the @param candidate will be removed
	 * from the list.
	 */
	void updateExternalPopulation(PopulationEntry candidate) {

		if (externalPopulation.size() == 0)
			externalPopulation.add(candidate);
		else {
			boolean eligibleToJoin = true;

			/*
			 * remove the members that are dominated by the candidate and check
			 * the eligibility of the candidate.
			 */
			for (int i = 0; i < externalPopulation.size(); i++) {
				PopulationEntry member = externalPopulation.get(i);
				if (dominate(candidate, member)) {
					/* the candidate dominates a member */
					externalPopulation.remove(i);
					i--;
				} else if (dominate(member, candidate))
					/* the candidate is dominated by a member */
					eligibleToJoin = false;
			}

			if (eligibleToJoin) {
				if (externalPopulation.size() < SIZE_OF_EP)
					externalPopulation.add(candidate);
			}
		}

	}

	/**
	 * Step 1: Initialization: initialize everything, includes EP, weights,
	 * neighborhood and ideal point based on the given @param initialPopulation
	 * and @param of.
	 */
	List<PopulationEntry> initialize() {
		initializeUniformWeight();

		Stream<PopulationEntry> populationWithObjectiveValues = initialPopulation.stream().map((Configuration c) -> {

			List<Double> fitness = of.getFitness(jobs, c);

			return new PopulationEntry(c, fitness);
		});

		List<PopulationEntry> populationList = populationWithObjectiveValues.collect(Collectors.toList());

		getFitnessAll(populationList);
		for (int i = 0; i < populationList.size(); i++) {
			updateExternalPopulation(populationList.get(i));
		}

		return populationList;
	}

	void evolve() {
		/* for each individual in the current population */
		for (int i = 0; i < populationSize; i++) {

			int index = rng.nextInt(currentPopulation.size());
			double prevFit = fitness.get(index);

			PopulationEntry corssover1 = currentPopulation.get(index);
			PopulationEntry corssover2 = currentPopulation.get(rng.nextInt(currentPopulation.size()));

			Configuration c = hyperMutation(onePointCrossover(corssover1.getConfiguration(), corssover2.getConfiguration()), jobs);

			PopulationEntry newIndividualEntry = new PopulationEntry(c, of.getFitness(jobs, c));

			double newFit = 0;
			newFit += lambda.get(i)[0] * newIndividualEntry.getObjectives().get(0);
			newFit += lambda.get(i)[0] * newIndividualEntry.getObjectives().get(0);

			if (newFit > prevFit) {
				currentPopulation.set(index, newIndividualEntry);
				fitness.set(index, newFit);
			}

			/*
			 * Step 2.5 Update of EP. (1) Remove from EP all the individuals
			 * dominated by the new individual; (2) Add new individual to EP if
			 * no individuals in EP dominates it.
			 */
			updateExternalPopulation(newIndividualEntry);

		}
	}

	private Configuration hyperMutation(Configuration c, List<Job> jobs) {

		if (pm < 0.0 || pm > 1.0)
			throw new IllegalArgumentException();

		List<Long> startTimes = c.startTimes;

		for (int i = 0; i < startTimes.size(); i++) {
			if (rng.nextDouble() < pm) {
				long newStart = rng.nextInt((int) (jobs.get(i).endQ - jobs.get(i).startQ)) + jobs.get(i).startQ;
				startTimes.set(i, newStart);
			}
		}

		return new Configuration(startTimes);

	}

	private Configuration onePointCrossover(Configuration c1, Configuration c2) {
		assert (c1.startTimes.size() == c2.startTimes.size());

		int index = rng.nextInt(c1.startTimes.size());

		List<Long> corssed = new ArrayList<>();
		for (int i = 0; i < c1.startTimes.size(); i++) {
			if (i < index)
				corssed.add(c1.startTimes.get(i));
			else
				corssed.add(c2.startTimes.get(i));
		}

		// List<Long> crossed = c1.startTimes.subList(0, index);
		// crossed.addAll(c2.startTimes.subList(index, c2.startTimes.size()));

		assert (corssed.size() == c1.startTimes.size());

		return new Configuration(corssed);

	}

	/**
	 * The MOEA/D algorithm.
	 */
	public List<PopulationEntry> apply() {

		if (initialPopulation.size() < 2)
			throw new IllegalArgumentException();

		/* Step 1 Initialization */
		currentPopulation = initialize();

		assert (fitness.size() > 0 && lambda.size() > 0);

		// /* A recorder that stores all the evolution process. */
		// final List<List<PopulationEntry>> history = new ArrayList<>();
		// history.add(currentPopulation);

		int current_iteration = 0;
		/* Step 2 Update */
		while (current_iteration < iteration) {

//			if (current_iteration % 10 == 0) {
//				System.out.println("now " + current_iteration + " generations");
//			}

			evolve();
			current_iteration++;
		}

		return externalPopulation;
	}

}
