package ru.slie.luna.template.card;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.jspecify.annotations.Nullable;
import ru.slie.luna.template.db.CardEntity;

import java.time.LocalDateTime;

public class Card {
    private final CardEntity entity;

    public Card(CardEntity entity) {
        this.entity = entity;
    }

    protected CardEntity getEntity() {
        return entity;
    }

    public Long getId() {
        if (entity.getId() != null) {
            return entity.getId();
        }

        return null;
    }

    public String getValue() {
        return entity.getValue();
    }

    public CardState getState() {
        return CardState.fromString(entity.getState());
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime getCreated() {
        return entity.getCreated();
    }

    public void setValue(String value) {
        this.entity.setValue(value);
    }

    public void setState(@Nullable CardState state) {
        if (state == null) {
            this.entity.setState(null);
        } else {
            this.entity.setState(state.getState());
        }
    }
}
