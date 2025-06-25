package dev.firstdark.rpc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.firstdark.rpc.connection.RPCConnection;
import dev.firstdark.rpc.enums.DiscordReply;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.exceptions.NoDiscordClientException;
import dev.firstdark.rpc.exceptions.PipeAccessDenied;
import dev.firstdark.rpc.exceptions.UnsupportedOsType;
import dev.firstdark.rpc.handlers.DiscordEventHandler;
import dev.firstdark.rpc.models.DiscordJoinRequest;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import dev.firstdark.rpc.utils.Backoff;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author HypherionSA
 * The Main RPC SDK entry point. You want to start here
 */
public class DiscordRpc {

    /**
     * The logger that is currently in use
     */
    @Setter
    @NotNull
    @Getter
    private Logger logger = LoggerFactory.getLogger(DiscordRpc.class);

    /**
     * If enabled, the SDK will print some additional info to the logs
     */
    @Getter
    @Setter
    private boolean isDebugMode = false;

    private final boolean disableIoThread;

    private long pid;
    private long nonce;
    private DiscordEventHandler eventHandler;
    private RPCConnection rpcConnection;
    private final Backoff reconnectTimeMs;

    private long nextConnect;

    private String joinGameSecret;
    private String spectateGameSecret;
    private ErrorCode lastErrorCode;
    private String lastErrorMessage;
    private ErrorCode lastDisconnectErrorCode;
    private String lastDisconnectErrorMessage;

    private final AtomicBoolean wasJustConnected;
    private final AtomicReference<User> connectedUser;
    private final AtomicBoolean wasJustDisconnected;
    private final AtomicBoolean gotErrorMessage;
    private final AtomicBoolean wasJoinGame;
    private final AtomicBoolean wasSpectateGame;

    private final Queue<byte[]> sendQueue;
    private final Queue<byte[]> presenceQueue;
    private final Queue<DiscordJoinRequest> joinAskQueue;

    private final AtomicBoolean keepRunning;
    private final Lock waitForIoMutex;
    private final Condition waitForIOActivity;
    private Thread ioThread;

    /**
     * Create a new RPC SDK instance, with the internal thread enabled
     */
    public DiscordRpc() {
        this(false);
    }

    /**
     * Create a new RPC SDK instance, potentially disabling the internal thread
     * If you disabled the thread, you will need to call {@link DiscordRpc#runCallbacks()} yourself, or they won't fire
     *
     * @param disableIoThread Disable or Enable in the internal thread
     */
    public DiscordRpc(boolean disableIoThread) {
        this.disableIoThread = disableIoThread;

        this.pid = -1;
        this.nonce = -1;
        this.eventHandler = null;
        this.rpcConnection = null;
        this.reconnectTimeMs = new Backoff(500L, 60000L);

        this.nextConnect = System.currentTimeMillis();

        this.wasJustConnected = new AtomicBoolean(false);
        this.connectedUser = new AtomicReference<>();
        this.wasJustDisconnected = new AtomicBoolean(false);
        this.gotErrorMessage = new AtomicBoolean(false);
        this.wasJoinGame = new AtomicBoolean(false);
        this.wasSpectateGame = new AtomicBoolean(false);

        this.sendQueue = new ConcurrentLinkedQueue<>();
        this.presenceQueue = new ConcurrentLinkedQueue<>();
        this.joinAskQueue = new ConcurrentLinkedQueue<>();

        this.keepRunning = new AtomicBoolean(true);
        this.waitForIoMutex = new ReentrantLock(true);
        this.waitForIOActivity = this.waitForIoMutex.newCondition();
        this.ioThread = null;
    }

    /**
     * Start an RPC connection
     *
     * @param applicationId The discord Application ID to use
     * @param handler Optional {@link DiscordEventHandler} to handle events
     * @param autoRegister Should the current game automatically be registered with Discord
     * @throws UnsupportedOsType Thrown when the Current OS is not supported
     */
    public void init(@NotNull String applicationId, @Nullable DiscordEventHandler handler, boolean autoRegister) throws UnsupportedOsType, PipeAccessDenied {
        this.init(applicationId, handler, autoRegister, null);
    }

    /**
     * Start an RPC connection
     *
     * @param applicationId The discord Application ID to use
     * @param handler Optional {@link DiscordEventHandler} to handle events
     * @param autoRegister Should the current game automatically be registered with Discord
     * @param optionalSteamId The Steam ID of the game that the RPC is tied to
     * @throws UnsupportedOsType Thrown when the Current OS is not supported
     */
    public void init(@NotNull String applicationId, @Nullable DiscordEventHandler handler, boolean autoRegister, @Nullable String optionalSteamId) throws UnsupportedOsType, PipeAccessDenied {
        if (this.rpcConnection != null)
            return;

        this.pid = this.getProcessId();
        this.eventHandler = handler;

        this.rpcConnection = RPCConnection.create(applicationId, this);

        if (autoRegister) {
            if (optionalSteamId != null && !optionalSteamId.isEmpty())
                this.registerSteamGame(applicationId, optionalSteamId);
            else
                this.register(applicationId, null);
        }

        this.rpcConnection.setConnectedCallback((user) -> {
            this.wasJustConnected.set(true);
            this.connectedUser.set(user);
            this.reconnectTimeMs.reset();

            if (this.eventHandler != null) {
                this.registerForEvent("ACTIVITY_JOIN");
                this.registerForEvent("ACTIVITY_SPECTATE");
                this.registerForEvent("ACTIVITY_JOIN_REQUEST");
            }
        });

        this.rpcConnection.setDisconnectedCallback(((lastErrorCode, lastErrorMessage) -> {
            this.lastDisconnectErrorCode = lastErrorCode;
            this.lastDisconnectErrorMessage = lastErrorMessage;
            this.wasJustDisconnected.set(true);
            this.updateReconnectTime();
        }));

        if (!this.disableIoThread) {
            this.keepRunning.set(true);
            this.ioThread = new Thread(() -> {
                try {
                    this.discordRpcIo();
                } catch (NoDiscordClientException ignored) {}
            });
            this.ioThread.start();
        }
    }

    /**
     * Shut down the RPC connection, not allowing new updates
     */
    public void shutdown() {
        if (this.rpcConnection == null)
            return;

        this.rpcConnection.setDisconnectedCallback(null);
        this.rpcConnection.setConnectedCallback(null);
        this.eventHandler = null;

        if (!this.disableIoThread) {
            this.keepRunning.set(false);
            this.signalIoActivity();

            try {
                this.ioThread.join();
            } catch (Exception ignored) {
                // TODO Logging
            }
        }

        RPCConnection.destroy(this.rpcConnection);
        this.rpcConnection = null;
    }

    /**
     * Update the RPC that is displayed currently
     *
     * @param discordRichPresence The new RPC to display, or NULL to clear
     */
    public void updatePresence(@Nullable DiscordRichPresence discordRichPresence) {
        if (discordRichPresence == null)
            discordRichPresence = DiscordRichPresence.builder().build();

        JsonObject data = discordRichPresence.toJson(this.pid, this.nonce++);
        this.presenceQueue.offer(data.toString().getBytes(StandardCharsets.UTF_8));
        this.signalIoActivity();
    }

    /**
     * Respond to a {@link DiscordJoinRequest}
     * @param user The {@link User} that initiated the request
     * @param reply The {@link DiscordReply} to send
     */
    public void respond(User user, DiscordReply reply) {
        if (this.rpcConnection == null || !this.rpcConnection.isOpen())
            return;

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("cmd", new JsonPrimitive(reply == DiscordReply.YES ? "SEND_ACTIVITY_JOIN_INVITE" : "CLOSE_ACTIVITY_JOIN_REQUEST"));

        JsonObject args = new JsonObject();
        args.addProperty("user_id", user.getUserId());
        jsonObject.add("args", args);
        jsonObject.add("nonce", new JsonPrimitive(String.valueOf(this.nonce++)));

        byte[] bytes = jsonObject.toString().getBytes();

        if (this.sendQueue.offer(bytes))
            this.signalIoActivity();
    }

    /**
     * Method to update {@link DiscordEventHandler} when the internal thread is disabled
     */
    public void runCallbacks() {
        if (this.rpcConnection == null)
            return;

        if (this.eventHandler != null) {
            boolean wasDisconnected = this.wasJustDisconnected.getAndSet(false);
            boolean isConnected = this.rpcConnection.isOpen();

            if (isConnected && wasDisconnected)
                this.eventHandler.disconnected(this.lastDisconnectErrorCode, this.lastDisconnectErrorMessage);

            if (this.wasJustConnected.getAndSet(false))
                this.eventHandler.ready(connectedUser.get());

            if (this.gotErrorMessage.getAndSet(false))
                this.eventHandler.errored(this.lastErrorCode, this.lastErrorMessage);

            if (this.wasJoinGame.getAndSet(false))
                this.eventHandler.joinGame(this.joinGameSecret);

            if (this.wasSpectateGame.getAndSet(false))
                this.eventHandler.spectateGame(this.spectateGameSecret);

            DiscordJoinRequest request;
            while ((request = this.joinAskQueue.poll()) != null)
                if (this.eventHandler != null)
                    this.eventHandler.joinRequest(request);

            if (!isConnected && wasDisconnected)
                this.eventHandler.disconnected(this.lastDisconnectErrorCode, this.lastDisconnectErrorMessage);
        }
    }

    /**
     * Register a steam Game with the RPC
     *
     * @param applicationId The discord application ID
     * @param optionalSteamId The Steam ID of the game
     */
    public void registerSteamGame(String applicationId, String optionalSteamId) {
        if (this.rpcConnection != null)
            this.rpcConnection.getBaseConnection().registerSteamGame(applicationId, optionalSteamId);
    }

    /**
     * Register a game with the RPC
     *
     * @param applicationId The discord application ID
     * @param command The command that starts the game
     */
    public void register(String applicationId, String command) {
        if (this.rpcConnection != null)
            this.rpcConnection.getBaseConnection().register(applicationId, command);
    }

    /**
     * Internal method
     * Used to subscribe to RPC events like Join, Spectate etc
     *
     * @param name The name of the EVENT to subscribe to
     */
    private void registerForEvent(String name) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("cmd", new JsonPrimitive("SUBSCRIBE"));
        jsonObject.add("evt", new JsonPrimitive(name));
        jsonObject.add("nonce", new JsonPrimitive(String.valueOf(this.nonce++)));

        byte[] bytes = jsonObject.toString().getBytes();

        if (this.sendQueue.offer(bytes))
            this.signalIoActivity();
    }

    /**
     * Internal method
     * Used to calculate the {@link Backoff} time between requests
     */
    private void updateReconnectTime() {
        this.nextConnect = System.currentTimeMillis() + this.reconnectTimeMs.nextDelay();
    }

    /**
     * The internal thread that takes care of updating the connection and callbacks
     */
    private void discordRpcIo() throws NoDiscordClientException, PipeAccessDenied {
        while (this.keepRunning.get()) {
            this.updateConnection();
            runCallbacks();
            this.waitForIoMutex.lock();

            try {
                this.waitForIOActivity.await(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {

            } finally {
                this.waitForIoMutex.unlock();
            }
        }
    }

    /**
     * Internal method
     * Used to check the current RPC queues for data that needs to be processed
     */
    private void signalIoActivity() {
        this.waitForIoMutex.lock();

        try {
            this.waitForIOActivity.signalAll();
        } catch (Exception ignored) {

        } finally {
            this.waitForIoMutex.unlock();
        }
    }

    /**
     * Update the state of the Current RPC connection
     */
    public void updateConnection() throws NoDiscordClientException, PipeAccessDenied {
        if (this.rpcConnection == null)
            return;

        if (!this.rpcConnection.isOpen()) {
            if (System.currentTimeMillis() >= this.nextConnect) {
                this.updateReconnectTime();
                this.rpcConnection.open();
            }
        } else {
            while (true) {
                JsonObject message = new JsonObject();

                if (!this.rpcConnection.read(message, false))
                    break;

                String evtName = message.has("evt") && !message.get("evt").isJsonNull() ? message.get("evt").getAsString() : null;
                String nonce = message.has("nonce") && !message.get("nonce").isJsonNull() ? message.get("nonce").getAsString() : null;

                if (nonce != null) {
                    if (evtName != null && evtName.equals("ERROR")) {
                        JsonObject data = message.get("data").getAsJsonObject();
                        int error = data.get("code").getAsInt();
                        this.lastErrorCode = data.has("code") ? error >= ErrorCode.values().length ? ErrorCode.UNKNOWN : ErrorCode.values()[error] : ErrorCode.SUCCESS;
                        this.lastErrorMessage = data.has("message") ? data.get("message").getAsString() : "";
                        this.gotErrorMessage.set(true);
                    }
                } else {
                    if (evtName == null)
                        continue;

                    switch (evtName) {
                        case "ACTIVITY_JOIN": {
                            JsonObject data = message.get("data").getAsJsonObject();
                            String secret = data.has("secret") ? data.get("secret").getAsString() : null;

                            if (secret != null) {
                                this.joinGameSecret = secret;
                                this.wasJoinGame.set(true);
                            }
                            break;
                        }
                        case "ACTIVITY_SPECTATE": {
                            JsonObject data = message.get("data").getAsJsonObject();
                            String secret = data.has("secret") ? data.get("secret").getAsString() : null;

                            if (secret != null) {
                                this.spectateGameSecret = secret;
                                this.wasSpectateGame.set(true);
                            }
                            break;
                        }
                        case "ACTIVITY_JOIN_REQUEST": {
                            JsonObject data = message.get("data").getAsJsonObject();
                            JsonObject user = data.get("user").getAsJsonObject();

                            if (!user.isJsonNull()) {
                                DiscordJoinRequest discordJoinRequest = new DiscordJoinRequest(new Gson().fromJson(user, User.class));
                                this.joinAskQueue.offer(discordJoinRequest);
                            }

                            break;
                        }
                    }
                }
            }

            if (!this.presenceQueue.isEmpty()) {
                byte[] bytes;

                while ((bytes = this.presenceQueue.peek()) != null) {
                    if (!this.rpcConnection.write(bytes))
                        break;
                    else
                        this.presenceQueue.poll();
                }
            }

            if (!this.sendQueue.isEmpty()) {
                byte[] bytes;

                while ((bytes = this.sendQueue.poll()) != null)
                    this.rpcConnection.write(bytes);
            }
        }
    }

    /**
     * Internal Method
     * Used to calculate the current Process ID
     *
     * @return The process ID or -1 if invalid
     */
    private long getProcessId() {
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int index = jvmName.indexOf('@');

        if (index < 1)
            return -1;

        try {
            return Long.parseLong(jvmName.substring(0, index));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Helper method to print debug information when {@link DiscordRpc#isDebugMode()} is set to true
     *
     * @param message The message to be sent
     * @param objects Optional data that will replace the placeholders in Message
     */
    public void printDebug(String message, Object... objects) {
        if (!isDebugMode)
            return;

        getLogger().info("[DEBUG] {}", String.format(message, objects));
    }

}
