package de.tu_dortmund.ub.api.daia.jop;

public class JOPException extends Exception {

	private static final long serialVersionUID = 1535406374894145286L;

    public JOPException() {
    }

    public JOPException(String message) {
        super(message);
    }

    public JOPException(String message, Throwable cause) {
        super(message, cause);
    }
}
