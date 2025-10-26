package JC.Training.src.WizardGame.handlers;

import JC.Training.src.WizardGame.DTOs.TrickBidDTO;
import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.enums.TrickBidExists;
import JC.Training.src.WizardGame.models.Card;
import JC.Training.src.WizardGame.models.CardModel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GameManager {

    private final static Set<Card> allPossibleCards = initializeAllPossibleCards();

    public static boolean isFirstToBid(Map<String, TrickBidDTO> bids){
        boolean isFirst = true;

        for(Map.Entry<String, TrickBidDTO> map : bids.entrySet()){
            if(map.getValue().trickBidExists().equals(TrickBidExists.YES)){
                isFirst = false;
                break;
            }
        }

        return isFirst;
    }

    public static int getMadeBidsAmount(Map<String, TrickBidDTO> bids){
        int otherBidsCounter = 0;

        for(Map.Entry<String, TrickBidDTO> map : bids.entrySet()){
            if(map.getValue().trickBidExists().equals(TrickBidExists.YES)){
                otherBidsCounter++;
            }
        }

        return otherBidsCounter;
    }

    private static Set<Card> initializeAllPossibleCards(){
        Set<Card> cards = new HashSet<>();

        for(int i = 1; i <= 13; i++){
            Card blueCard = new Card(IDGenerator.getNextCardId(), CardType.BLUE, i);
            Card redCard = new Card(IDGenerator.getNextCardId(), CardType.RED, i);
            Card greenCard = new Card(IDGenerator.getNextCardId(), CardType.GREEN, i);
            Card yellowCard = new Card(IDGenerator.getNextCardId(), CardType.YELLOW, i);

            cards.add(blueCard);
            cards.add(redCard);
            cards.add(greenCard);
            cards.add(yellowCard);
        }

        for(int i = 1; i<=4; i++){
            cards.add(new Card(IDGenerator.getNextCardId(), CardType.JESTER, 0));
            cards.add(new Card(IDGenerator.getNextCardId(), CardType.WIZARD, 14));
        }
        return cards;
    }

public static Set<Card> findCards(Set<CardModel> cardModels){
        Set<Card> foundCards = new HashSet<>();

        for(CardModel cardModel : cardModels){
            CardType cardModelType = cardModel.getModelCardType();
            int cardModelNumber = cardModel.getModelCardNumber();

            for(Card c : allPossibleCards){
                if(c.getType().equals(cardModelType) && c.getNumber() == cardModelNumber){
                    foundCards.add(c);
                }
            }
        }
        return foundCards;
}
public static Card findCard(CardModel cardModel){
    CardType cardModelType = cardModel.getModelCardType();
    int cardModelNumber = cardModel.getModelCardNumber();

    return allPossibleCards.stream().filter(c -> c.getNumber() == cardModelNumber
            && c.getType().equals(cardModelType)).findFirst().get();
}

    public static Set<Card> getAllPossibleCards() {
        return allPossibleCards;
    }
}
