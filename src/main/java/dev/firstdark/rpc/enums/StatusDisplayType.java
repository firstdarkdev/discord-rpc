package dev.firstdark.rpc.enums;

import lombok.Getter;

@Getter
public enum StatusDisplayType {
    NAME(0),
    STATE(1),
    DETAILS(2);

    private final int id;

    StatusDisplayType(int id) {
        this.id = id;
    }
}
