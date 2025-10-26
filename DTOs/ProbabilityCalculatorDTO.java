package JC.Training.src.WizardGame.DTOs;

import JC.Training.src.WizardGame.models.Card;

import java.util.Map;
import java.util.Set;

public record ProbabilityCalculatorDTO(Card card,
                                       Card trumpCard,
                                       Set<Card> usedCards,
                                       Set<Card> ownCards,
                                       Map<String, DroppedCardsInTrickDTO> droppedCardsInTrickDTO,
                                       int botCardsNumberAtTrickStart,
                                       boolean isFirstToDrop
                                       ) {
}
