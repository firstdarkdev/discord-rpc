package dev.firstdark.rpc.connection;

import dev.firstdark.rpc.DiscordRpc;

import java.io.FileWriter;

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
     * @param command       The command used to launch the application
     */
    @Override
    public void register(String applicationId, String command) {
        try {
            if (command != null)
                this.registerCommand(applicationId, command);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to register command", ex);
        }
    }

    /**
     * Register a steam game with Discord
     *
     * @param applicationId The Discord Application ID
     * @param steamId       The ID of the steam game to register
     */
    @Override
    public void registerSteamGame(String applicationId, String steamId) {
        this.register(applicationId, "steam://rungameid/" + steamId);
    }

    private void registerCommand(String applicationId, String command) {
        String home = System.getenv("HOME");
        if (home == null)
            throw new RuntimeException("Unable to find user HOME directory");

        String path = home + "/Library/Application Support/discord";

        if (!this.mkdir(path))
            throw new RuntimeException("Failed to create directory '" + path + "'");

        path += "/games";

        if (!this.mkdir(path))
            throw new RuntimeException("Failed to create directory '" + path + "'");

        path += "/" + applicationId + ".json";

        try (FileWriter fileWriter = new FileWriter(path)) {
            fileWriter.write("{\"command\": \"" + command + "\"}");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write fame info into '" + path + "'");
        }
    }

}
