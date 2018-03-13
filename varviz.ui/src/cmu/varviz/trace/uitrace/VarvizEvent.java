package cmu.varviz.trace.uitrace;

public class VarvizEvent {

	private final EventType type;

	public enum EventType {
		COLOR_CHANGED, LOCATION_CHANGED, LABEL_CHANGED
	}
	
	public VarvizEvent(EventType type) {
		this.type = type;
	}
	
	public EventType getType() {
		return type;
	}
	
	
}
