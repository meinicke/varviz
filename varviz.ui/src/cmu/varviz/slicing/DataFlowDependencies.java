package cmu.varviz.slicing;

import java.util.HashSet;
import java.util.Set;

public class DataFlowDependencies {

	private Set<IDependency> dependencies = new HashSet<>();

	public void addDependencies(Set<IDependency> dependencies) {
		this.dependencies.addAll(dependencies);

	}
	
	@Override
	public String toString() {
		return dependencies.toString();
	}

	public Set<IDependency> getDependencies() {
		return dependencies;
	}
}
