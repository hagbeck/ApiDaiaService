package de.tu_dortmund.ub.api.daia.linkresolver;

public class LinkResolverException extends Exception {

    private static final long serialVersionUID = 1535406374894145286L;

    public LinkResolverException() {
    }

    public LinkResolverException(String message) {
        super(message);
    }

    public LinkResolverException(String message, Throwable cause) {
        super(message, cause);
    }
}
