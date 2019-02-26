package cmu.varviz.slicing;

public class LocalVarDependency implements IDependency {
	
	public final String varName;

	public LocalVarDependency(String varName) {
		this.varName = varName;
	}
	
	@Override
	public String toString() {
		return varName;
	}

	@Override
	public int hashCode() {
		return varName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return varName.equals(((LocalVarDependency) obj).varName);
	}
	
	
}
