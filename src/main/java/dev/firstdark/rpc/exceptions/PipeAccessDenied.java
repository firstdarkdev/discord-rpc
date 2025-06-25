package dev.firstdark.rpc.exceptions;

public class PipeAccessDenied extends RuntimeException {
    public PipeAccessDenied(String message) {
        super(message);
    }
}
