package WizardGame.strategies;

import WizardGame.DTOs.*;
import WizardGame.contexts.RoundContext;

import WizardGame.handlers.CardManager;
import WizardGame.handlers.ProbabilityCalculator;
import WizardGame.models.Card;

import java.util.*;

public class AnalystV1_2 implements GameStrategy {
    private final Set<Card> takingTrickCards = new HashSet<>();

private double winCoefficient = 40;

    private final AnalystV1 analystV1 = new AnalystV1(takingTrickCards);

    private int notEnoughTrickWinsStrick = 0;
    private int tooMuchTrickWinsStrick = 0;

    @Override
    public Card dropCard(DropCardContextDTO dropCardContextDTO) {
        int roundNumber = dropCardContextDTO.roundContext().number();
        int coefficient = CardManager.determineBidsWinsCoefficient(dropCardContextDTO.roundContext().fullTrickBids(), roundNumber);

        if(roundNumber <= 5 && coefficient <= 0){
            return CardManager.getStrongestCard(dropCardContextDTO.ownCards(), dropCardContextDTO.roundContext().trumpCard().getType());
        }
        return analystV1.dropCard(dropCardContextDTO);
    }

    @Override
    public Card beatCard(BeatCardContextDTO beatCardContextDTO) {
        return analystV1.beatCard(beatCardContextDTO);
    }
private static boolean b = true;
    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO createTrickBidsContextDTO) {
        takingTrickCards.clear();
        analystV1.updateMemory(takingTrickCards);

        Set<Card> ownCards = createTrickBidsContextDTO.ownCards();
        Card trumpCard = createTrickBidsContextDTO.trumpCard();
        GameContextDTO gameContextDTO = createTrickBidsContextDTO.gameContextDTO();
        int currentRoundNumber = gameContextDTO.currentRoundNumber();
        int botsNumber = gameContextDTO.botAmount();
        String ownId = createTrickBidsContextDTO.ownID();
        Map<String, TrickBidDTO> bids = createTrickBidsContextDTO.trickBids();
if(b){
    winCoefficient *= (double) botsNumber / 10 + 1;
b = false;
}
        Stack<RoundContext> roundContexts = createTrickBidsContextDTO.gameContextDTO().roundContexts();

        if(!roundContexts.isEmpty() && currentRoundNumber > 2){
            int ownCoefficient = getOwnBidWinCoefficient(getLastRoundContext(roundContexts), ownId);
            if(ownCoefficient > 0){
                notEnoughTrickWinsStrick++;
            }else if(ownCoefficient < 0){
                tooMuchTrickWinsStrick++;
            }
        }

        if(notEnoughTrickWinsStrick == 3){
winCoefficient *= 1.1;
notEnoughTrickWinsStrick = 0;
        } else if(tooMuchTrickWinsStrick == 3){
         winCoefficient *= 0.84;
            tooMuchTrickWinsStrick = 0;
         }


         //   findTakingTrickCardsIfNotFirst(ownCards, currentRoundNumber, trumpCard, botsNumber);
for(Card card : ownCards){
    double pWin = ProbabilityCalculator.calculateAbstractProbability(
            new AbstractProbabilityDTO(card, trumpCard, ownCards, currentRoundNumber, botsNumber)
    );
    System.out.println("PWin: " + pWin + " with card " + card);
    System.out.println("WinCoeffiecient: " + winCoefficient);
    if(pWin >= winCoefficient){
takingTrickCards.add(card);
    }
}

        System.out.println("Взятки: " + takingTrickCards.size());
        System.out.println(ownCards);
        System.out.println("Карты, на которые рассчитывает бот: " + takingTrickCards);
        return takingTrickCards.size();
    }


    private RoundContext getLastRoundContext(Stack<RoundContext> roundContexts){
        int roundNumber = roundContexts.getLast().number();

        RoundContext lastRoundContext;

        if(roundNumber == 1){
            lastRoundContext = roundContexts.getLast();
        }else{
            lastRoundContext = roundContexts.get(roundContexts.size()-2);
        }
        return lastRoundContext;
    }

    private int getOwnBidWinCoefficient(RoundContext roundContext, String ownId){
        Map<String, Integer> wins = roundContext.fullTrickWins();
        Map<String, TrickBidDTO> bids = roundContext.fullTrickBids();

        return bids.get(ownId).trickBid() - wins.get(ownId);
    }

    @Override
    public String getStrategyName() {
        return "AnalystV1_2";
    }
}
