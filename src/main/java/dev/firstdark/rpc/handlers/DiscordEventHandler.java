package dev.firstdark.rpc.handlers;

import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.models.DiscordJoinRequest;
import dev.firstdark.rpc.models.User;
import org.jetbrains.annotations.Nullable;

/**
 * @author HypherionSA
 * Public facing events that fire during the lifecycle of the Discord RPC
 * Your app/game should implement this if needed
 */
public interface DiscordEventHandler {

    /**
     * Fires when the RPC is connected and ready
     *
     * @param user The logged in discord user that is currently active
     */
    void ready(User user);

    /**
     * Called when the RPC connected is terminated
     *
     * @param errorCode The {@link ErrorCode} that was returned
     * @param message The error message that was returned, if any
     */
    void disconnected(ErrorCode errorCode, @Nullable String message);

    /**
     * Called when an error occurs in the RPC connection
     *
     * @param errorCode The {@link ErrorCode} that was returned
     * @param message The error message that was returned, if any
     */
    void errored(ErrorCode errorCode, @Nullable String message);

    /**
     * Called after a Join Request has been accepted
     *
     * @param joinSecret The password of the session
     */
    void joinGame(String joinSecret);

    /**
     * Called when a Spectate request has been approved
     *
     * @param spectateSecret The password of the session
     */
    void spectateGame(String spectateSecret);

    /**
     * Called when someone requests to join/spectate your game
     *
     * @param joinRequest The {@link DiscordJoinRequest} that was sent
     */
    void joinRequest(DiscordJoinRequest joinRequest);

}
