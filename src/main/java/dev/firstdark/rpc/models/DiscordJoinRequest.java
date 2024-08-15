package dev.firstdark.rpc.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author HypherionSA
 * Event sent when a Join/Spectate request is made
 */
@AllArgsConstructor
@Getter
public class DiscordJoinRequest {

    /**
     * The user that requested to join
     */
    private final User user;

}
