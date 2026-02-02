package WizardGame.DTOs;

import WizardGame.enums.CardType;
import WizardGame.contexts.RoundContext;
import WizardGame.models.Card;

import java.util.Set;

public record ChooseDropCardIfBidsBiggerThanWinsDTO(Set<CardType> allowedCardTypes, Set<Card> ownAllowedCards,
                                                    Card leadingCard, RoundContext roundContext) {
}
