package WizardGame.strategies;

import WizardGame.DTOs.BeatCardContextDTO;
import WizardGame.DTOs.CreateTrickBidsContextDTO;
import WizardGame.DTOs.DropCardContextDTO;
import WizardGame.models.Card;

public interface GameStrategy {
    Card dropCard(DropCardContextDTO dropCardContextDTO);
    Card beatCard(BeatCardContextDTO beatCardContextDTO);

    Integer createTrickBids(CreateTrickBidsContextDTO createTrickBidsContextDTO);
    String getStrategyName();
}
