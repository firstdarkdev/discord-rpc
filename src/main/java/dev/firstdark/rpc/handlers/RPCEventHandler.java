package dev.firstdark.rpc.handlers;

import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.models.DiscordJoinRequest;
import dev.firstdark.rpc.models.User;
import org.jetbrains.annotations.Nullable;

/**
 * @author HypherionSA
 *
 * Abstract class for Discord Event handler. Override what you need
 */
public class RPCEventHandler implements DiscordEventHandler {

    /**
     * Fires when the RPC is connected and ready
     *
     * @param user The logged in discord user that is currently active
     */
    @Override
    public void ready(User user) {

    }

    /**
     * Called when the RPC connected is terminated
     *
     * @param errorCode The {@link ErrorCode} that was returned
     * @param message The error message that was returned, if any
     */
    @Override
    public void disconnected(ErrorCode errorCode, @Nullable String message) {

    }

    /**
     * Called when an error occurs in the RPC connection
     *
     * @param errorCode The {@link ErrorCode} that was returned
     * @param message The error message that was returned, if any
     */
    @Override
    public void errored(ErrorCode errorCode, @Nullable String message) {

    }

    /**
     * Called after a Join Request has been accepted
     *
     * @param joinSecret The password of the session
     */
    @Override
    public void joinGame(String joinSecret) {

    }

    /**
     * Called when a Spectate request has been approved
     *
     * @param spectateSecret The password of the session
     */
    @Override
    public void spectateGame(String spectateSecret) {

    }

    /**
     * Called when someone requests to join/spectate your game
     *
     * @param joinRequest The {@link DiscordJoinRequest} that was sent
     */
    @Override
    public void joinRequest(DiscordJoinRequest joinRequest) {

    }
}
