package WizardGame.strategies;

import WizardGame.handlers.CardManager;
import WizardGame.enums.CardType;
import WizardGame.DTOs.BeatCardContextDTO;
import WizardGame.DTOs.CreateTrickBidsContextDTO;
import WizardGame.DTOs.DropCardContextDTO;
import WizardGame.DTOs.GameContextDTO;
import WizardGame.contexts.RoundContext;
import WizardGame.models.Card;

import java.util.Set;

public class Version2Strategy implements GameStrategy {
    private final DefaultStrategy defaultStrategy = new DefaultStrategy();

    @Override
    public Card dropCard(DropCardContextDTO dropCardContextDTO) {
        String ownId = dropCardContextDTO.ownId();
        RoundContext roundContext = dropCardContextDTO.roundContext();
        GameContextDTO gameContextDTO = dropCardContextDTO.gameContextDTO();
        Integer ownTrickWinsAmount = roundContext.fullTrickWins().get(ownId);
        Integer ownTrickBidsAmount = roundContext.fullTrickBids().get(ownId).trickBid();

        Set<Card> ownCards = dropCardContextDTO.ownCards();
        Card dropCard;

        if(ownTrickBidsAmount.equals(ownTrickWinsAmount) || ownTrickBidsAmount < ownTrickWinsAmount){
            dropCard = CardManager.getWeakestCard(ownCards, roundContext.trumpCard().getType());
        }else{
            dropCard = defaultStrategy.dropCard(new DropCardContextDTO(ownId, roundContext, ownCards, gameContextDTO));
        }

        return dropCard;
    }

    @Override
    public Card beatCard(BeatCardContextDTO beatCardContextDTO) {
        return defaultStrategy.beatCard(beatCardContextDTO);
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO createTrickBidsContextDTO) {
        Set<Card> cards = createTrickBidsContextDTO.ownCards();
        Card trumpCard = createTrickBidsContextDTO.trumpCard();
        GameContextDTO gameContextDTO = createTrickBidsContextDTO.gameContextDTO();
        int currentRoundNumber = gameContextDTO.currentRoundNumber();
        int botAmount = gameContextDTO.botAmount();

        int trickBidsAmount = 0;

        for(Card card : cards){
            CardType type = card.getType();

            if(type.equals(CardType.WIZARD)){
                trickBidsAmount++;
            }else if(type.equals(trumpCard.getType())){
                if(currentRoundNumber == 1){
                    trickBidsAmount++;
                }else if(card.getNumber() > 6){
                    trickBidsAmount++;
                }
            }else if(card.getType() != CardType.JESTER){
                if(card.getNumber() > 10){
                    trickBidsAmount++;
                }
            }
        }
        return trickBidsAmount;
    }

    @Override
    public String getStrategyName() {
        return "Version2";
    }
}
