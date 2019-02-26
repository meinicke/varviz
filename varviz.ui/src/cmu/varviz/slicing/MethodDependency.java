package cmu.varviz.slicing;

public class MethodDependency implements IDependency {

	public final String owner;
	public final String methodName;

	public MethodDependency(String owner, String methodName) {
		this.owner = owner;
		this.methodName = methodName;
	}
	
	@Override
	public String toString() {
		return owner +  '.' + methodName + "()";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodDependency other = (MethodDependency) obj;
		if (methodName == null) {
			if (other.methodName != null)
				return false;
		} else if (!methodName.equals(other.methodName))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		return true;
	}
}
