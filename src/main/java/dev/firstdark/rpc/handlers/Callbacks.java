package dev.firstdark.rpc.handlers;

import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.models.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * @author HypherionSA
 * Callbacks fired by the SDK during certain events
 */
@ApiStatus.Internal
public class Callbacks {

    public interface Connected {
        /**
         * Fires when the Discord RPC is connected and ready to be used
         *
         * @param user The User that is currently connected
         */
        void accept(User user);
    }

    public interface Disconnected {
        /**
         * Fires when an error occurs in the SDK
         *
         * @param lastErrorCode The last {@link ErrorCode} that was returned
         * @param lastErrorMessage The last error message that was returned
         */
        void accept(ErrorCode lastErrorCode, @Nullable String lastErrorMessage);
    }

}
