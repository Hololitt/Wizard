package WizardGame.strategies;

import WizardGame.enums.CardType;
import WizardGame.DTOs.BeatCardContextDTO;
import WizardGame.DTOs.CreateTrickBidsContextDTO;
import WizardGame.DTOs.DropCardContextDTO;
import WizardGame.DTOs.GameContextDTO;
import WizardGame.models.Card;

import java.util.Set;

public class Version1Strategy implements GameStrategy {
    private final DefaultStrategy defaultStrategy = new DefaultStrategy();
    @Override
    public Card dropCard(DropCardContextDTO dropCardContextDTO) {
      return defaultStrategy.dropCard(dropCardContextDTO);
    }

    @Override
    public Card beatCard(BeatCardContextDTO beatCardContextDTO) {
        String ownId = beatCardContextDTO.ownId();
        Integer ownTrickWins = beatCardContextDTO.roundContext().fullTrickWins().get(ownId);
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
    public String getStrategyName(){
        return "Version1";
    }
}
