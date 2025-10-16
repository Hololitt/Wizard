package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.CardManager;
import JC.Training.src.WizardGame.CardType;
import JC.Training.src.WizardGame.DTOs.*;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.models.Card;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnalystV1 implements GameStrategy{
    private final Set<Card> takingTrickCards = new HashSet<>();

    @Override
    public Card dropCard(DropCardContextDTO dropCardContextDTO) {
        String ownId = dropCardContextDTO.ownId();
        RoundContext roundContext = dropCardContextDTO.roundContext();

        Integer ownTrickWinsAmount = roundContext.fullTrickWins().get(ownId);
        Integer ownTrickBidsAmount = roundContext.fullTrickBids().get(ownId);

        Set<Card> ownCards = dropCardContextDTO.ownCards();
        Card dropCard;

        if(ownTrickBidsAmount.equals(ownTrickWinsAmount) || ownTrickBidsAmount < ownTrickWinsAmount){
            dropCard = CardManager.getWeakestCard(ownCards, dropCardContextDTO.roundContext().trumpCard().getType());
        }else{
            dropCard = chooseDropCard(ownCards, roundContext);
        }

        return dropCard;
    }

    private Card chooseDropCard(Set<Card> ownCards, RoundContext roundContext){
        Card dropCard = null;
        int roundContextNumber = roundContext.number();

        Set<Card> notTakingTrickCards = new HashSet<>();

        for(Card c : ownCards){
            if(!takingTrickCards.contains(c)){
                notTakingTrickCards.add(c);
            }
        }
        int allTrickBidsAmount = CardManager.countAllTrickBids(roundContext.fullTrickBids());
        boolean isNotTakingTrickCardsEmpty = notTakingTrickCards.isEmpty();

        if(allTrickBidsAmount == roundContextNumber){
            if(isNotTakingTrickCardsEmpty){
                dropCard = CardManager.getWeakestCard(takingTrickCards, roundContext.trumpCard().getType());
            }else{
                dropCard = notTakingTrickCards.stream().findFirst().get();
            }
        }
        if(allTrickBidsAmount < roundContextNumber){
         dropCard = CardManager.getStrongestCard(ownCards, roundContext.trumpCard().getType());
        }

        if(allTrickBidsAmount > roundContextNumber){
            dropCard = CardManager.getWeakestCard(ownCards, roundContext.trumpCard().getType());
        }

        return dropCard;
    }

    @Override
    public Card beatCard(BeatCardContextDTO beatCardContextDTO) {

        TrickContextDTO trickContext = beatCardContextDTO.trickContextDTO();

        Set<Card> responses = new HashSet<>(trickContext.responses());
        Set<CardType> allowedCardTypes = beatCardContextDTO.allowedResponses();
        Set<Card> ownAllowedCards = CardManager.determineAllowedCards(allowedCardTypes, beatCardContextDTO.ownCards());

        CardType trumpCardType = beatCardContextDTO.roundContext().trumpCard().getType();

        Card firstDroppedCard = trickContext.firstDroppedCard();
        Card leadingCard = CardManager.defineLeadingCard(responses, firstDroppedCard,trumpCardType);

        RoundContext roundContext = beatCardContextDTO.roundContext();
        String ownId = beatCardContextDTO.ownId();

        Integer ownTrickWinsAmount = roundContext.fullTrickWins().get(ownId);
        Integer ownTrickBidsAmount = roundContext.fullTrickBids().get(ownId);

        Card beatCard = null;

        if(ownTrickBidsAmount.equals(ownTrickWinsAmount) || ownTrickBidsAmount < ownTrickWinsAmount){

  beatCard =  chooseDropCardIfBidsEqualsWinOrSmaller(ownAllowedCards, leadingCard, trumpCardType);
        }

        if(ownTrickBidsAmount > ownTrickWinsAmount){
ChooseDropCardIfBidsBiggerThanWinsDTO dto = new ChooseDropCardIfBidsBiggerThanWinsDTO(allowedCardTypes,
        ownAllowedCards, leadingCard, roundContext);

          beatCard = chooseDropCardIfBidsBiggerThanWins(dto);
        }

        return beatCard;
    }


    private Card chooseDropCardIfBidsEqualsWinOrSmaller(Set<Card> ownAllowedCards, Card leadingCard, CardType trumpCardType){
        Card beatCard;

        List<Card> cardsNotAbleToBeat = CardManager.determineCardsBeatCard(ownAllowedCards, leadingCard, trumpCardType);

        cardsNotAbleToBeat.forEach(ownAllowedCards::remove);

        if(cardsNotAbleToBeat.isEmpty()){
            beatCard = CardManager.getStrongestCard(ownAllowedCards, trumpCardType);
        }else{
            beatCard = CardManager.getStrongestCard(new HashSet<>(cardsNotAbleToBeat), trumpCardType);
        }

        return beatCard;
    }


    private Card chooseDropCardIfBidsBiggerThanWins(ChooseDropCardIfBidsBiggerThanWinsDTO dto){
        RoundContext roundContext = dto.roundContext();

        Set<CardType> allowedCardTypes = dto.allowedCardTypes();
        Set<Card> ownAllowedCards = dto.ownAllowedCards();

        Card leadingCard = dto.leadingCard();

        CardType trumpCardType = roundContext.trumpCard().getType();


        List<Card> cardsBeatCard = CardManager.determineCardsBeatCard(ownAllowedCards, leadingCard, trumpCardType);
        Set<Card> allowedTakingTrickCards = CardManager.determineAllowedCards(allowedCardTypes, takingTrickCards);


        int coefficient = CardManager.determineBidsWinsCoefficient(roundContext.fullTrickBids(), roundContext.number());

boolean isCoefficientSmallerThanZero = coefficient < 0;
boolean isCoefficientBiggerThanZero = coefficient > 0;

        Card beatCard;

        if(cardsBeatCard.isEmpty()){
            if(isCoefficientSmallerThanZero){
                beatCard = CardManager.getWeakestCard(ownAllowedCards, trumpCardType);
            } else if(isCoefficientBiggerThanZero){ //кто-то обожрется
               beatCard = CardManager.getStrongestCard(ownAllowedCards, trumpCardType);
            }else{

                List<CardType> takingTrickCardTypes = new ArrayList<>();

                for(Card c : takingTrickCards){
                    takingTrickCardTypes.add(c.getType());
                }


            WeakestCardExceptDTO weakestCardExceptDTO = CardManager.getWeakestCardExcept(new HashSet<>(ownAllowedCards), trumpCardType,
                    new ArrayList<>(takingTrickCardTypes));

                if(weakestCardExceptDTO.isPresent()){
                    beatCard = weakestCardExceptDTO.weakestCard();
                }else{
                    beatCard = new ArrayList<>(ownAllowedCards).getFirst();
                }
            }


        }else{

            if(isCoefficientSmallerThanZero){
                beatCard = CardManager.getWeakestCard(new HashSet<>(cardsBeatCard), trumpCardType);
             } else if(isCoefficientBiggerThanZero){ //кто-то обожрется
                List<Card> cardsBeatLeadingCard = CardManager.determineCardsBeatCard(ownAllowedCards, leadingCard, trumpCardType);


                   List<Card> cardsCantBeatLeadingCard = new ArrayList<>(ownAllowedCards);
                 cardsCantBeatLeadingCard.removeAll(cardsBeatLeadingCard);

                boolean cardsCantBeatLeadingCardPresent = !cardsCantBeatLeadingCard.isEmpty();

                if(cardsBeatLeadingCard.size() == ownAllowedCards.size()){
                    beatCard = CardManager.getStrongestCard(ownAllowedCards, trumpCardType);

                }else {

                    if(cardsCantBeatLeadingCardPresent){
                        beatCard = CardManager.getStrongestCard(new HashSet<>(cardsCantBeatLeadingCard), trumpCardType);
                    }else{
                        beatCard = CardManager.getStrongestCard(ownAllowedCards, trumpCardType);
                    }

                }

              }else{
                List<Card> normalCardsCanBeat = new ArrayList<>(cardsBeatCard);
                Set<Card> allowedTakingTrickCardsList = new HashSet<>(allowedTakingTrickCards);

                allowedTakingTrickCardsList.forEach(normalCardsCanBeat::remove);


                boolean takingTrickCardsCanBeat = !CardManager.determineCardsBeatCard(allowedTakingTrickCardsList,
                        leadingCard, trumpCardType).isEmpty();

                if(takingTrickCardsCanBeat){
                    beatCard = allowedTakingTrickCardsList.stream().toList().getFirst();
                }else{
                    beatCard = cardsBeatCard.getFirst();
                }
            }
        }

        return beatCard;
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO createTrickBidsContextDTO) {
        cleanUpTakingTrickCards();

    Set<Card> cards = createTrickBidsContextDTO.ownCards();
        Card trumpCard = createTrickBidsContextDTO.trumpCard();
        GameContextDTO gameContextDTO = createTrickBidsContextDTO.gameContextDTO();
        int currentRoundNumber = gameContextDTO.currentRoundNumber();
        int botAmount = gameContextDTO.botAmount();

        for(Card card : cards){
            boolean forTakingTrick = isForTakingTrick(card, trumpCard, currentRoundNumber);

            if(forTakingTrick) {
                takingTrickCards.add(card);
            }
        }

        return takingTrickCards.size();
    }

    private static boolean isForTakingTrick(Card card, Card trumpCard, int currentRoundNumber) {
        CardType type = card.getType();
        boolean forTakingTrick = type.equals(CardType.WIZARD);

        if(type.equals(trumpCard.getType())){
            if(currentRoundNumber == 1){
               forTakingTrick = true;
            }else if(card.getNumber() > 6){
              forTakingTrick = true;
            }
        }
        if(card.getType() != CardType.JESTER){
            if(card.getNumber() >= 10){
             forTakingTrick = true;
            }
        }
        return forTakingTrick;
    }

    @Override
    public String getStrategyName() {
        return "AnalystV1";
    }

    private void cleanUpTakingTrickCards(){
        takingTrickCards.clear();
    }
}
