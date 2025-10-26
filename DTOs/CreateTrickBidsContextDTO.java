package JC.Training.src.WizardGame.DTOs;

import JC.Training.src.WizardGame.models.Card;

import java.util.Map;
import java.util.Set;

public record CreateTrickBidsContextDTO(GameContextDTO gameContextDTO,
                                        Map<String, TrickBidDTO> trickBids,
                                        Card trumpCard,
                                        Set<Card> ownCards,
                                        String dealerId,
                                        String ownID) {
}
