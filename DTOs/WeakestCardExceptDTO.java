package JC.Training.src.WizardGame.DTOs;

import JC.Training.src.WizardGame.models.Card;

public record WeakestCardExceptDTO(Boolean isPresent, Card weakestCard) {

    public Card weakestCard() {
        if (!isPresent) {
            throw new IllegalStateException("Tried to get weakestCard when it is not present.");
        }
        return weakestCard;
    }
}
