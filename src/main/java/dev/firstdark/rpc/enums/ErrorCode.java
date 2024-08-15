package dev.firstdark.rpc.enums;

import lombok.Getter;

/**
 * @author HypherionSA
 * Various errors codes that can be returned by the IPC Pipe
 */
@Getter
public enum ErrorCode {
    SUCCESS(0),
    PIPE_CLOSED(1),
    READ_CORRUPT(2),
    UNKNOWN(-1);

    private final int id;

    ErrorCode(int id) {
        this.id = id;
    }
}
