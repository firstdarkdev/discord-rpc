package dev.firstdark.rpc.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.enums.PartyPrivacy;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author HypherionSA
 * The Discord Rich Presence Structure
 * Use {@link DiscordRichPresence#builder()} to get started
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
@Getter
public class DiscordRichPresence {

    private String state;
    private String details;
    private long startTimestamp;
    private long endTimestamp;

    private String largeImageKey;
    private String largeImageText;
    private String smallImageKey;
    private String smallImageText;

    private String partyId;
    private int partySize;
    private int partyMax;

    private String matchSecret;
    private String joinSecret;
    private String spectateSecret;
    private boolean instance;

    @Builder.Default
    private PartyPrivacy privacy = PartyPrivacy.PRIVATE;

    @Builder.Default
    private ActivityType activityType = ActivityType.PLAYING;

    @Singular
    private List<RPCButton> buttons = new ArrayList<>();

    /**
     * Add a button to the RPC
     *
     * @param label The label of the button
     * @param url The URL of the button
     */
    public void addButton(String label, String url) {
        if (buttons.size() + 1 > 2)
            return;

        buttons.add(new RPCButton(label, url));
    }

    /**
     * Clear all the current buttons from the RPC
     */
    public void clearButtons() {
        this.buttons.clear();
    }

    /**
     * Converts the current RPC structure to a JSON object for sending
     *
     * @param pid The process ID of the session running the RPC
     * @param nonce Unique identifier for the RPC
     * @return The constructed JSON string, ready for sending
     */
    public JsonObject toJson(long pid, long nonce) {
        JsonObject data = new JsonObject();
        // Add the required command info
        data.addProperty("nonce", nonce);
        data.addProperty("cmd", "SET_ACTIVITY");

        // Add the Process ID
        JsonObject args = new JsonObject();
        args.addProperty("pid", pid);

        JsonObject activity = new JsonObject();

        // Check if the STATE value is set
        if (isNotNullOrEmpty(state))
            activity.addProperty("state", state);

        // Check if the DETAILS value is set
        if (isNotNullOrEmpty(details))
            activity.addProperty("details", details);

        // Check if the timestamps are set and need to be sent
        if (this.startTimestamp != 0 || this.endTimestamp != 0) {
            JsonObject timestamps = new JsonObject();

            if (this.startTimestamp != 0)
                timestamps.addProperty("start", this.startTimestamp);

            if (this.endTimestamp != 0)
                timestamps.addProperty("end", this.endTimestamp);

            activity.add("timestamps", timestamps);
        }

        // Check if the image keys/texts are set
        if (isNotNullOrEmpty(largeImageKey) || isNotNullOrEmpty(largeImageText) || isNotNullOrEmpty(smallImageKey) || isNotNullOrEmpty(smallImageText)) {
            JsonObject assets = new JsonObject();

            if (isNotNullOrEmpty(largeImageKey))
                assets.addProperty("large_image", this.largeImageKey);

            if (isNotNullOrEmpty(largeImageText))
                assets.addProperty("large_text", this.largeImageText);

            if (isNotNullOrEmpty(smallImageKey))
                assets.addProperty("small_image", this.smallImageKey);

            if (isNotNullOrEmpty(smallImageText))
                assets.addProperty("small_text", this.smallImageText);

            activity.add("assets", assets);
        }

        // Check if the party information is set
        if (isNotNullOrEmpty(partyId) || this.partySize > 0 || this.partyMax > 0) {
            JsonObject party = new JsonObject();

            if (isNotNullOrEmpty(partyId))
                party.addProperty("id", partyId);

            if (partySize != 0) {
                JsonArray size = new JsonArray();
                size.add(partySize);

                if (partyMax > 0)
                    size.add(partyMax);

                party.add("size", size);
            }

            party.addProperty("privacy", privacy.ordinal());
            activity.add("party", party);
        }

        // Check if the join/spectate secrets are set
        if (isNotNullOrEmpty(matchSecret) || isNotNullOrEmpty(spectateSecret) || isNotNullOrEmpty(joinSecret)) {
            JsonObject secrets = new JsonObject();

            if (isNotNullOrEmpty(matchSecret))
                secrets.addProperty("match", matchSecret);

            if (isNotNullOrEmpty(joinSecret))
                secrets.addProperty("join", joinSecret);

            if (isNotNullOrEmpty(spectateSecret))
                secrets.addProperty("spectate", spectateSecret);

            activity.add("secrets", secrets);
        }

        // Parse and add the buttons
        if (!buttons.isEmpty()) {
            List<RPCButton> finalButtons = buttons.stream().filter(RPCButton::isValid).collect(Collectors.toList());
            if (finalButtons.size() > 2)
                finalButtons = finalButtons.subList(0, 2);

            JsonArray btns = new JsonArray();
            finalButtons.forEach(b -> btns.add(b.toJson()));

            activity.add("buttons", btns);
        }

        // Put it all together
        activity.addProperty("type", activityType.ordinal());
        activity.addProperty("instance", instance);
        args.add("activity", activity);
        data.add("args", args);

        return data;
    }

    /**
     * Helper method to check if a string is not null and empty
     *
     * @param input The string to test
     * @return True if not null and empty
     */
    private boolean isNotNullOrEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }

    /**
     * Represents an RPC button on the RPC
     */
    @AllArgsConstructor(staticName = "of")
    @Getter
    public static class RPCButton {
        private final String label;
        private final String url;

        /**
         * Checks that both the LABEL and URL are set for the button
         *
         * @return True if both values are set
         */
        public boolean isValid() {
            return label != null && !label.isEmpty() && url != null && !url.isEmpty();
        }

        /**
         * Convert the button to JSON
         *
         * @return The JsonObject to be appended to the packet
         */
        public JsonObject toJson() {
            JsonObject button = new JsonObject();

            // Button labels have a length limit, so we apply it here
            button.addProperty("label", label.substring(0, Math.min(label.length(), 32)));
            button.addProperty("url", url);
            return button;
        }
    }
}
