package JC.Training.src.WizardGame.contexts;

import JC.Training.src.WizardGame.DTOs.LastWinnerDTO;
import JC.Training.src.WizardGame.DTOs.TrickBidDTO;
import JC.Training.src.WizardGame.models.Card;

import java.util.Map;

public record RoundContext(int number,
                           Card trumpCard,
                           Map<String, TrickBidDTO> fullTrickBids,
                           Map<String, Integer> fullTrickWins,
                           LastWinnerDTO lastWinnerDTO,
                           String dealerId) {

}
