package WizardGame.DTOs;

import WizardGame.contexts.RoundContext;
import WizardGame.models.Card;

import java.util.Set;

public record DropCardContextDTO(String ownId, RoundContext roundContext, Set<Card> ownCards, GameContextDTO gameContextDTO) {
}
