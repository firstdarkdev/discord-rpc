package dev.firstdark.rpc.enums;

/**
 * @author HypherionSA
 * The current state of the RPC SDK
 */
public enum RPCState {
    DISCONNECTED,
    SENT_HANDSHAKE,
    AWAITING_RESPONSE,
    CONNECTED
}
