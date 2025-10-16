package JC.Training.src.WizardGame.DTOs;

import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.models.Card;

import java.util.Set;

public record DropCardContextDTO(String ownId, RoundContext roundContext, Set<Card> ownCards, GameContextDTO gameContextDTO) {
}
