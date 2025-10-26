package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.handlers.CardManager;
import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.DTOs.BeatCardContextDTO;
import JC.Training.src.WizardGame.DTOs.CreateTrickBidsContextDTO;
import JC.Training.src.WizardGame.DTOs.DropCardContextDTO;
import JC.Training.src.WizardGame.models.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultStrategy implements GameStrategy{
    @Override
    public Card dropCard(DropCardContextDTO dropCardContextDTO) {
        List<Card> list = new ArrayList<>(dropCardContextDTO.ownCards());

        return list.getFirst();
    }

    @Override
    public Card beatCard(BeatCardContextDTO beatCardContextDTO) {
        Set<Card> cards = beatCardContextDTO.ownCards();
        Set<CardType> allowedCardTypes = beatCardContextDTO.allowedResponses();
        Set<Card> allowedResponses = CardManager.determineAllowedCards(allowedCardTypes, cards);

        return new ArrayList<>(allowedResponses).getFirst();
    }


    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO createTrickBidsContextDTO) {
        return 1;
    }
    public String getStrategyName(){
        return "DefaultStrategy";
    }
}
