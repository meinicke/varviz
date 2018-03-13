package cmu.varviz;

/**
 * {@link RuntimeException} for Varviz.
 * 
 * @author Jens Meinicke
 *
 */
public class VarvizException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public VarvizException(String description) {
		super(description);
	}

}
