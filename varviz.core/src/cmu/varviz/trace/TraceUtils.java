package cmu.varviz.trace;

public final class TraceUtils {
	
	private TraceUtils() {}

	public static final String toShortID(int i) {
		if (i < 0) {
			return "";
		}
		final int rem = i % 52;
		final char letter;
		if (rem < 26) {
			letter = (char) ('a' + (rem));
		} else {
			letter = (char) ('A' + (rem - 26));
		}
		return toShortID(i / 52 - 1) + letter;
	}
}
