package WizardGame.contexts;

import WizardGame.DTOs.LastWinnerDTO;
import WizardGame.DTOs.TrickBidDTO;
import WizardGame.models.Card;

import java.util.Map;
import java.util.Set;

public record RoundContext(int number,
                           Card trumpCard,
                           Map<String, TrickBidDTO> fullTrickBids,
                           Map<String, Integer> fullTrickWins,
                           Set<Card> usedCards,
                           LastWinnerDTO lastWinnerDTO,
                           String dealerId) {
public void addUsedCard(Card usedCard){
    usedCards.add(usedCard);
}
}
