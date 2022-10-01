package top.focess.net;

import java.io.IOException;

/**
 * Thrown to indicate this port is not available
 */
public class IllegalPortException extends IOException {
    /**
     * Constructs a IllegalPortException
     *
     * @param port the unavailable port
     */
    public IllegalPortException(final int port) {
        super("The " + port + " is not available.");
    }
}
