package ru.slie.luna.template.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.slie.luna.db.query.DeleteResult;
import ru.slie.luna.exception.ValidateException;
import ru.slie.luna.template.card.Card;
import ru.slie.luna.template.card.CardManager;
import ru.slie.luna.template.card.CardState;
import ru.slie.luna.template.rest.request.CardRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/todo")
public class TodoRest {
    private final CardManager cardManager;

    public TodoRest(CardManager cardManager) {
        this.cardManager = cardManager;
    }

    @GetMapping({"", "/"})
    public List<Card> getCards() {
        return cardManager.getCards();
    }

    @PostMapping({"", "/"})
    public Card createCard(@RequestBody CardRequest request) {
        return cardManager.createCard(request.getValue());
    }

    @PutMapping("/{cardId}")
    public Card updateCard(@PathVariable Long cardId, @RequestBody CardRequest request) throws ValidateException {
        Card card = cardManager.getCardById(cardId).orElse(null);
        if (card == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        card.setState(CardState.fromString(request.getState()));
        card.setValue(request.getValue());
        cardManager.saveCard(card);

        return card;
    }

    @PatchMapping("/{cardId}")
    public Card patchCard(@PathVariable("cardId") Long cardId, @RequestBody Map<String, String> request) throws ValidateException {
        Card card = cardManager.getCardById(cardId).orElse(null);
        if (card == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        for (Map.Entry<String, String> entry: request.entrySet()) {
            switch (entry.getKey()) {
                case "value" -> card.setValue(entry.getValue());
                case "state" -> card.setState(CardState.fromString(entry.getValue()));
            }
        }

        cardManager.saveCard(card);
        return card;
    }

    @DeleteMapping("/{cardId}")
    public DeleteResult deleteCard(@PathVariable Long cardId) {
        return cardManager.delete(cardId);
    }
}
