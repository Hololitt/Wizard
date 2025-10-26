package JC.Training.src.WizardGame.DTOs;

import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.models.Card;

import java.util.Set;

public record BeatCardContextDTO(String ownId, TrickContextDTO trickContextDTO, RoundContext roundContext,
                                 Set<CardType> allowedResponses, Set<Card> ownCards, GameContextDTO gameContextDTO) {
}
