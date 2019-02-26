package cmu.varviz.slicing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdk.internal.org.objectweb.asm.tree.analysis.BasicValue;
import jdk.internal.org.objectweb.asm.Type;

/**
 * Value to represent data dependencies.
 * 
 * @author Jens Meinicke
 *
 */
public class DataDependencyValue extends BasicValue {

	
	Set<IDependency> dependencies = new HashSet<>();

	DataDependencyValue(BasicValue value, IDependency... dependencies) {
		super(value != null ? value.getType() : BasicValue.UNINITIALIZED_VALUE.getType());
		
		for (IDependency dependency : dependencies) {
			this.dependencies.add(dependency);
		}
		if (value instanceof DataDependencyValue) {
			this.dependencies.addAll(((DataDependencyValue) value).dependencies);
		}
	}
	
	DataDependencyValue(BasicValue value1, BasicValue value2) {
		super(value1 != null ? value1.getType() : BasicValue.UNINITIALIZED_VALUE.getType());
		if (value1 instanceof DataDependencyValue) {
			this.dependencies.addAll(((DataDependencyValue) value1).dependencies);
		}
		if (value2 instanceof DataDependencyValue) {
			this.dependencies.addAll(((DataDependencyValue) value2).dependencies);
		}
	}
	
	public DataDependencyValue(Type type, List<? extends BasicValue> values, IDependency dependencies) {
		super(type);
		for (BasicValue value : values) {
			if (value instanceof DataDependencyValue) {
				this.dependencies.addAll(((DataDependencyValue) value).dependencies);
			}
		}
		this.dependencies.add(dependencies);
	}

	@Override
	public String toString() {
		return super.toString() + " " + dependencies;
	}
}

