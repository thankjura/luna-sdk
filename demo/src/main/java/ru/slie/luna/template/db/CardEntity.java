package ru.slie.luna.template.db;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import ru.slie.luna.regolith.ActiveDocEntity;
import ru.slie.luna.template.card.CardState;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
public class CardEntity extends ActiveDocEntity {
    private String value;
    private String state;
    private LocalDateTime created;

    // empty constructor required for morphia
    public CardEntity() {}

    public CardEntity(String value) {
        this.value = value;
        this.state = CardState.Open.getState();
        this.created = LocalDateTime.now();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
