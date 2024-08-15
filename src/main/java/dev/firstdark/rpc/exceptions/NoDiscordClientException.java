package dev.firstdark.rpc.exceptions;

/**
 * @author HypherionSA
 * Exception thrown when the SDK can't find a valid Discord install
 */
public class NoDiscordClientException extends Exception {

    private static final long serialVersionUID = 1L;

    public NoDiscordClientException() {
        super("No Valid Discord Client was found for this Instance");
    }

}
