package WizardGame.strategies;

import WizardGame.handlers.CardManager;
import WizardGame.enums.CardType;
import WizardGame.DTOs.BeatCardContextDTO;
import WizardGame.DTOs.CreateTrickBidsContextDTO;
import WizardGame.DTOs.DropCardContextDTO;
import WizardGame.models.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultStrategy implements GameStrategy {
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
