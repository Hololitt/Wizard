package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.DTOs.*;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.handlers.CardManager;
import JC.Training.src.WizardGame.handlers.GameManager;
import JC.Training.src.WizardGame.models.Card;

import java.util.*;

public class AnalystV1_1 implements GameStrategy{
private final Set<Card> takingTrickCards = new HashSet<>();

    private final Map<Integer, Integer> botAmountNormalCardNumber = new HashMap<>(Map.of(
            2, 9,
            3, 10,
            4, 11,
            5, 12,
            6, 12
    ));

private final Map<Integer, Integer> botAmountTrumpCardNumber = new HashMap<>(Map.of(
        2, 6,
        3, 7,
        4, 8,
        5, 8,
        6, 8
));

private final AnalystV1 analystV1 = new AnalystV1(takingTrickCards);

private int notEnoughTrickWinsStrick = 0;
private int tooMuchTrickWinsStrick = 0;

    @Override
    public Card dropCard(DropCardContextDTO dropCardContextDTO) {
        int roundNumber = dropCardContextDTO.roundContext().number();
        int coefficient = CardManager.determineBidsWinsCoefficient(dropCardContextDTO.roundContext().fullTrickBids(), roundNumber);

        if(roundNumber <= 4 && coefficient <= 0){
            return CardManager.getStrongestCard(dropCardContextDTO.ownCards(), dropCardContextDTO.roundContext().trumpCard().getType());
        }
        return analystV1.dropCard(dropCardContextDTO);
    }

    @Override
    public Card beatCard(BeatCardContextDTO beatCardContextDTO) {
        return analystV1.beatCard(beatCardContextDTO);
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO createTrickBidsContextDTO) {
        takingTrickCards.clear();
        analystV1.updateMemory(takingTrickCards);


        Set<Card> cards = createTrickBidsContextDTO.ownCards();
        Card trumpCard = createTrickBidsContextDTO.trumpCard();
        GameContextDTO gameContextDTO = createTrickBidsContextDTO.gameContextDTO();
        int currentRoundNumber = gameContextDTO.currentRoundNumber();
        int botAmount = gameContextDTO.botAmount();
        String ownId = createTrickBidsContextDTO.ownID();
        Map<String, TrickBidDTO> bids = createTrickBidsContextDTO.trickBids();

        boolean isFirst = GameManager.isFirstToBid(bids);

        int coefficient = CardManager.determineBidsWinsCoefficient(createTrickBidsContextDTO.trickBids(), currentRoundNumber);
        int countCardTypes = CardManager.countCardTypes(cards);
        double averageCardsNumber = CardManager.getAverageCardsNumber(cards);


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
            botAmountNormalCardNumber.merge(botAmount, 1, Integer::sum);
            notEnoughTrickWinsStrick = 0;

        } else if(tooMuchTrickWinsStrick == 3){
            botAmountNormalCardNumber.merge(botAmount, -1, Integer::sum);
            tooMuchTrickWinsStrick = 0;
        }


        if(isFirst && currentRoundNumber <=3 && countCardTypes == 1 && averageCardsNumber >= 10){
takingTrickCards.addAll(cards);
        }else{
            findTakingTrickCards(cards, currentRoundNumber, isFirst, trumpCard, botAmount);
        }

        return takingTrickCards.size();
    }

private void findTakingTrickCards(Set<Card> cards, int currentRoundNumber, boolean isFirst, Card trumpCard, int botAmount){
    for (Card card : cards) {
        CardType type = card.getType();
        boolean forTakingTrick = type.equals(CardType.WIZARD);

        if (type.equals(trumpCard.getType())) {
            if (currentRoundNumber <= Math.round((float) 4 / ((float) botAmount /2))) {
                forTakingTrick = true;
            } else if (card.getNumber() > botAmountTrumpCardNumber.get(botAmount)) {
                forTakingTrick = true;
            }
        }else if (card.getType() != CardType.JESTER) {
            if (card.getNumber() >= botAmountNormalCardNumber.get(botAmount)) {
                forTakingTrick = true;
            }
        }


        if (forTakingTrick) {
            takingTrickCards.add(card);
        }
    }
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
        return "AnalystV1_1";
    }
}