package ga;

import java.util.Collections;
import java.util.List;

public final class PopulationEntry {

	private final Configuration config;
	private final List<Double> objectives;

	public PopulationEntry(Configuration config, List<Double> objectives) {
		this.config = config;
		this.objectives = Collections.unmodifiableList(objectives);
	}

	public Configuration getConfiguration() {
		return config;
	}

	public List<Double> getObjectives() {
		return objectives;
	}

}
