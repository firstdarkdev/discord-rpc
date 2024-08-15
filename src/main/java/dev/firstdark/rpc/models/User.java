package dev.firstdark.rpc.models;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author HypherionSA
 * Represents a Discord User. Not all information is returned here, because it's not needed
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class User {

    private String userId;
    private String username;

    @Deprecated
    private String discriminator;

    @SerializedName("global_name")
    private String globalName;
    private String avatar;

}
