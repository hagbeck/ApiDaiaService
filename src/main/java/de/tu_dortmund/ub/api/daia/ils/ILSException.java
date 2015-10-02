package de.tu_dortmund.ub.api.daia.ils;

public class ILSException extends Exception {

	private static final long serialVersionUID = 1535406374894145286L;

    public ILSException() {
    }

    public ILSException(String message) {
        super(message);
    }

    public ILSException(String message, Throwable cause) {
        super(message, cause);
    }
}
