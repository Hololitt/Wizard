package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.DTOs.BeatCardContextDTO;
import JC.Training.src.WizardGame.DTOs.CreateTrickBidsContextDTO;
import JC.Training.src.WizardGame.DTOs.DropCardContextDTO;
import JC.Training.src.WizardGame.models.Card;

public interface GameStrategy {
    Card dropCard(DropCardContextDTO dropCardContextDTO);
    Card beatCard(BeatCardContextDTO beatCardContextDTO);

    Integer createTrickBids(CreateTrickBidsContextDTO createTrickBidsContextDTO);
    String getStrategyName();
}
