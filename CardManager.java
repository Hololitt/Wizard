package JC.Training.src.WizardGame;

import JC.Training.src.WizardGame.DTOs.WeakestCardExceptDTO;
import JC.Training.src.WizardGame.models.Card;

import java.util.*;
import java.util.stream.Collectors;

public class CardManager {

    public static int countCardsOfType(Set<Card> cards, CardType cardType){
        int cardsOfTypeAmount = 0;

      for(Card card : cards){
          if(card.getType().equals(cardType)){
              cardsOfTypeAmount++;
          }
      }

      return cardsOfTypeAmount;
    }

   public static Set<Card> getCardsOfType(Set<Card> cards, CardType cardType){
       return cards.stream().filter(c -> c.getType().equals(cardType)).collect(Collectors.toSet());
    }

    public static List<Card> sortCardsFromMinToMax(Set<Card> cards){
        List<Card> cardList = new ArrayList<>(cards);
         cardList.sort(Comparator.comparingInt(Card::getNumber));
         return cardList;
    }

    public static WeakestCardExceptDTO getWeakestCardExcept(Set<Card> cardsWithoutExceptions, CardType trumpCardType,
                                                            List<CardType> exceptions){

        cardsWithoutExceptions.removeIf(c -> exceptions.contains(c.getType()));

        if(cardsWithoutExceptions.isEmpty()){
            return new WeakestCardExceptDTO(false, null);
        }else{
            Card weakestCard = getWeakestCard(cardsWithoutExceptions, trumpCardType);

            return new WeakestCardExceptDTO(true, weakestCard);
        }

    }

    public static Card getWeakestCard(Set<Card> cards, CardType trumpCardType){
        List<Card> cardList = sortCardsFromMinToMax(cards);

List<Card> trumpCards = new ArrayList<>();
List<Card> wizardCards = new ArrayList<>();

        for(Card c : cardList){
if(c.getType().equals(trumpCardType)){
    trumpCards.add(c);
}

if(c.getType().equals(CardType.WIZARD)){
    wizardCards.add(c);
}
        }

        int cardListSize = cardList.size();
        int wizardListSize = wizardCards.size();
        int trumpListSize = trumpCards.size();

        Card weakestCard;

        if(cardListSize > trumpListSize + wizardListSize){
            trumpCards.forEach(cardList::remove);
            wizardCards.forEach(cardList::remove);
weakestCard = cardList.getFirst();

        }else {
                if(cardListSize == wizardListSize){
                    weakestCard = cardList.getFirst();

                }else if(cardListSize == trumpListSize + wizardListSize){
                    wizardCards.forEach(cardList::remove);
                    weakestCard = cardList.getFirst();
                }else{
                    weakestCard = cardList.getFirst();
                }
            }



        return weakestCard;
    }

    public static Card getStrongestCard(Set<Card> cards, CardType trumpCardType){
    Set<Card> trumpCards = getCardsOfType(cards, trumpCardType);
Set<Card> wizardCards = getCardsOfType(cards, CardType.WIZARD);

if(!wizardCards.isEmpty()){
    return new ArrayList<>(wizardCards).getFirst();
}
    if(trumpCards.isEmpty()){
        return sortCardsFromMinToMax(cards).reversed().getFirst();
    }else{
        return sortCardsFromMinToMax(trumpCards).reversed().getFirst();
    }
    }

    public static int countAllTrickBids(Map<String, Integer> trickBids){
        int allTrickBidsAmount = 0;

        for(Map.Entry<String, Integer> map : trickBids.entrySet()){
            allTrickBidsAmount += map.getValue();
        }

        return allTrickBidsAmount;
    }

    public static Card defineLeadingCard(Set<Card> cards, Card firstDroppedCard, CardType trumpCardType){
        Card leadingCard = firstDroppedCard;
        CardType leadingCardType = leadingCard.getType();

        for(Card card : cards){
            CardType cardType = card.getType();

            if(!leadingCardType.equals(CardType.WIZARD)){
                if(cardType.equals(leadingCardType) && card.getNumber() > leadingCard.getNumber()){
                    leadingCard = card;
                }

                if(!(leadingCardType.equals(trumpCardType)) && cardType.equals(trumpCardType)){
                    leadingCard = card;
                }

                if(card.getType().equals(CardType.WIZARD)){
                    leadingCard = card;
                }
            }

        }
        return leadingCard;
    }


    public static List<Card> determineCardsBeatCard(Set<Card> cards, Card cardToBeat, CardType trumpCardType){
        List<Card> cardsBeatCard = new ArrayList<>();

        for(Card beatCard : cards){
            CardType beatCardType = beatCard.getType();
            int beatCardNumber = beatCard.getNumber();

            CardType cardToBeatType = cardToBeat.getType();
            int cardToBeatNumber = cardToBeat.getNumber();

            if(!cardToBeat.getType().equals(CardType.WIZARD)){
                if(beatCardType.equals(cardToBeatType) && beatCardNumber > cardToBeatNumber){
                    cardsBeatCard.add(beatCard);
                }
                if(beatCardType.equals(trumpCardType) && !cardToBeatType.equals(trumpCardType)){
                    cardsBeatCard.add(beatCard);
                }
                if(cardToBeatType.equals(CardType.JESTER) && !beatCardType.equals(CardType.JESTER)){
                    cardsBeatCard.add(beatCard);
                }
                if(beatCardType.equals(CardType.WIZARD)){
                    cardsBeatCard.add(beatCard);
                }
            }
        }
        return cardsBeatCard;
    }

    public static Set<CardType> defineAllowedResponseCardTypes(Card firstDroppedCard, Set<Card> cards) {
        CardType firstDroppedCardType = firstDroppedCard.getType();
boolean sameCardTypeExists = false;

        for(Card card : cards){
            if(card.getType().equals(firstDroppedCard.getType())){
sameCardTypeExists = true;
break;
            }
        }

        if(!sameCardTypeExists){
            List<CardType> cardTypes = new ArrayList<>(Arrays.stream(CardType.values()).toList());

            cardTypes.remove(firstDroppedCardType);

            return new HashSet<>(cardTypes);
        }

        if(firstDroppedCardType.equals(CardType.WIZARD) || firstDroppedCardType.equals(CardType.JESTER)){
            return new HashSet<>(List.of(CardType.values()));
        }else{
            return new HashSet<>(List.of(firstDroppedCard.getType(), CardType.WIZARD, CardType.JESTER));
        }
    }

    public static Set<Card> determineAllowedCards(Set<CardType> allowedCardTypes, Set<Card> cards){
        Set<Card> allowedCards = new HashSet<>();

        for(Card card : cards){
            if(allowedCardTypes.contains(card.getType())){
                allowedCards.add(card);
            }
        }
        return allowedCards;
    }
public static int determineBidsWinsCoefficient(Map<String, Integer> bids, int roundNumber){
        for(Map.Entry<String, Integer> map : bids.entrySet()){
            roundNumber -= map.getValue();
        }
        return roundNumber;
}
}
