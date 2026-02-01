package ru.slie.luna.template.card;

import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import org.springframework.stereotype.Component;
import ru.slie.luna.exception.ValidateException;
import ru.slie.luna.locale.I18nResolver;
import ru.slie.luna.regolith.ActiveDocManager;
import ru.slie.luna.search.DeleteInfo;
import ru.slie.luna.search.DeleteInfoImpl;
import ru.slie.luna.template.db.CardEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CardManager {
    private final ActiveDocManager activeDocManager;
    private final I18nResolver i18n;

    public CardManager(ActiveDocManager activeDocManager,
                       I18nResolver i18n) {
        this.activeDocManager = activeDocManager;
        this.i18n = i18n;
    }

    private void validate(CardEntity entity) throws ValidateException {
        ValidateException validateException = new ValidateException();

        if (entity.getValue() == null || entity.getValue().trim().isEmpty()) {
            validateException.addError("value", i18n.getText("demo.field.value.required"));
        }

        if (entity.getState() == null || entity.getState().trim().isEmpty()) {
            validateException.addError("value", i18n.getText("demo.field.value.required"));
        }

        validateException.raise();
    }

    public Optional<Card> getCardById(String cardId) {
        Optional<CardEntity> entity = activeDocManager.getById(CardEntity.class, cardId);
        return entity.map(Card::new);

    }

    public List<Card> getCards() {
        Query<CardEntity> query = activeDocManager.query(CardEntity.class);
        List<Card> out = new ArrayList<>();

        try (MorphiaCursor<CardEntity> cursor =  query.iterator(new FindOptions().sort(Sort.ascending("created")))) {
            while (cursor.hasNext()) {
                out.add(new Card(cursor.next()));
            }
        }

        return out;
    }

    public Card createCard(String value) {
        CardEntity entity = new CardEntity(value);
        activeDocManager.save(entity);

        return new Card(entity);
    }

    public void saveCard(Card card) throws ValidateException {
        CardEntity entity = card.getEntity();
        validate(entity);
        activeDocManager.save(entity);
    }

    public DeleteInfo delete(String cardId) {
        Optional<CardEntity> entity = activeDocManager.getById(CardEntity.class, cardId);
        if (entity.isPresent()) {
            return activeDocManager.delete(entity.get());
        }

        return new DeleteInfoImpl();
    }
}
