package WizardGame.DTOs;

import WizardGame.models.Card;

import java.util.List;

public record TrickContextDTO(List<Card> responses, Card firstDroppedCard) {

    public void addRequest(Card card) {
        responses.add(card);
    }
}
