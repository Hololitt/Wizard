package WizardGame.DTOs;

import WizardGame.enums.CardType;
import WizardGame.contexts.RoundContext;
import WizardGame.models.Card;

import java.util.Set;

public record BeatCardContextDTO(String ownId, TrickContextDTO trickContextDTO, RoundContext roundContext,
                                 Set<CardType> allowedResponses, Set<Card> ownCards, GameContextDTO gameContextDTO) {
}
