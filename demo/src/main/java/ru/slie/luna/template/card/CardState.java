package ru.slie.luna.template.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CardState {
    Open("open"),
    Done("done");

    private final String state;

    CardState(String state) {
        this.state = state;
    }

    @JsonValue
    public String getState() {
        return state;
    }

    @JsonCreator
    public static CardState fromString(String state) {
        for (CardState cardState: CardState.values()) {
            if (cardState.state.equals(state)) {
                return cardState;
            }
        }

        return null;
    }
}
