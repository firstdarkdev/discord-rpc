package dev.firstdark.rpc.connection;

import dev.firstdark.rpc.DiscordRpc;

/**
 * @author HypherionSA
 * Windows IPC Client implementation
 */
class MacOsConnection extends UnixConnection {

    /**
     * Create a new instance of a MacOS IPC pipe
     *
     * @param rpc The initialized {@link DiscordRpc} client
     */
    MacOsConnection(DiscordRpc rpc) {
        super(rpc);
    }

    /**
     * Register an application as a Discord application
     *
     * @param applicationId The Discord Application ID
     * @param command The command used to launch the application
     */
    @Override
    public void register(String applicationId, String command) {
        // TODO Implement Register
    }

    /**
     * Register a steam game with Discord
     *
     * @param applicationId The Discord Application ID
     * @param steamId The ID of the steam game to register
     */
    @Override
    public void registerSteamGame(String applicationId, String steamId) {
        // TODO Implement Steam Register
    }

}
