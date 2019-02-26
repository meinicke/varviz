package cmu.varviz.slicing;

public class FieldDependency implements IDependency {

	public final String owner;
	public final String fieldName;

	public FieldDependency(String owner, String fieldName) {
		this.owner = owner;
		this.fieldName = fieldName;
	}
	
	@Override
	public String toString() {
		return owner +  '.' + fieldName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
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
		FieldDependency other = (FieldDependency) obj;
		if (fieldName == null) {
			if (other.fieldName != null)
				return false;
		} else if (!fieldName.equals(other.fieldName))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		return true;
	}
	
	
}
