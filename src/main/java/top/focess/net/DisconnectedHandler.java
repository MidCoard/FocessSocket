package top.focess.net;


/**
 * Represents a disconnected event handler to define how to handle disconnecting event.
 *            This is a functional interface whose functional method is {@link DisconnectedHandler#handle()}
 */
public interface DisconnectedHandler {

    void handle(int clientId);
}
