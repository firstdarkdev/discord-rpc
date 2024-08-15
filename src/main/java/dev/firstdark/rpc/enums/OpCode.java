package dev.firstdark.rpc.enums;

import lombok.Getter;

/**
 * @author HypherionSA
 * Valid types of IPC packets that can be sent
 */
@Getter
public enum OpCode {
    HANDSHAKE(0),
    FRAME(1),
    CLOSE(2),
    PING(3),
    PONG(4);

    private final int id;

    OpCode(int id) {
        this.id = id;
    }
}
