import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.exceptions.UnsupportedOsType;
import dev.firstdark.rpc.handlers.DiscordEventHandler;
import dev.firstdark.rpc.models.DiscordJoinRequest;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RPCTest {

    public static void main(String[] args) throws UnsupportedOsType {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

        DiscordRpc rpc = new DiscordRpc();
        rpc.setDebugMode(true);

        DiscordEventHandler handler = new DiscordEventHandler() {
            @Override
            public void ready(User user) {
                System.out.println("Ready");
                DiscordRichPresence presence = DiscordRichPresence.builder()
                        .details("Hello World")
                        .largeImageKey("gear")
                        .activityType(ActivityType.WATCHING)
                        .button(DiscordRichPresence.RPCButton.of("Test", "https://google.com"))
                        .build();

                rpc.updatePresence(presence);
                System.out.println(user.getUsername());
            }

            @Override
            public void disconnected(ErrorCode errorCode, String message) {
                System.out.println("Disconnected " + errorCode + " - " + message);
            }

            @Override
            public void errored(ErrorCode errorCode, String message) {
                System.out.println("Errored " + errorCode + " - " + message);
            }

            @Override
            public void joinGame(String joinSecret) {

            }

            @Override
            public void spectateGame(String spectateSecret) {

            }

            @Override
            public void joinRequest(DiscordJoinRequest joinRequest) {

            }
        };

        rpc.init("1000773209924317265", handler, false);
        //scheduledExecutorService.scheduleAtFixedRate(rpc::runCallbacks, 0, 500, TimeUnit.MILLISECONDS);
    }

}
