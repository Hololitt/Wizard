package JC.Training.src.WizardGame.DTOs;

import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.models.Card;

import java.util.Set;

public record ChooseDropCardIfBidsBiggerThanWinsDTO(Set<CardType> allowedCardTypes, Set<Card> ownAllowedCards,
                                                    Card leadingCard, RoundContext roundContext) {
}
