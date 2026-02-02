package WizardGame.DTOs;

import WizardGame.models.Card;

import java.util.Set;

public record AbstractProbabilityDTO(
        Card card,
        Card trumpCard,
        Set<Card> ownCards,
        int botCardsNumberAtTrickStart,
        int botsNumber
) {
}
