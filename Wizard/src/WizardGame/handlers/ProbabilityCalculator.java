package WizardGame.handlers;

import WizardGame.DTOs.AbstractProbabilityDTO;
import WizardGame.DTOs.DroppedCardsInTrickDTO;
import WizardGame.DTOs.ProbabilityCalculatorDTO;
import WizardGame.enums.CardType;
import WizardGame.enums.DroppedCardExists;
import WizardGame.enums.ProbabilityFormat;
import WizardGame.models.Card;

import java.util.*;

public class ProbabilityCalculator {
private final static Set<Card> allPossibleCards = GameManager.getAllPossibleCards();

    public static double calculateProbabilityToBeat(ProbabilityCalculatorDTO dto, ProbabilityFormat probabilityFormat){
boolean isFirstToDrop = dto.isFirstToDrop();
Set<Card> unusedCards = new HashSet<>(new HashSet<>(allPossibleCards));

unusedCards.removeAll(dto.usedCards());
unusedCards.removeAll(dto.ownCards());
unusedCards.remove(dto.trumpCard());

double probability;

        if(isFirstToDrop){
            probability = calculateProbabilityToWinIfFirstToDrop(dto, unusedCards);

        }else{
           probability = calculateProbabilityToWinIfNotFirstToDrop(dto, unusedCards);
        }

if(probabilityFormat.equals(ProbabilityFormat.TOTAL)){
    return probability * 100;
}else{
    return probability;
}

    }

    private static double calculateProbabilityToWinIfNotFirstToDrop(ProbabilityCalculatorDTO dto, Set<Card> unusedCards){
        Map<String, DroppedCardsInTrickDTO> droppedCardsInTrickDTOMap = dto.droppedCardsInTrickDTO();
        Card card = dto.card();
Card trumpCard = dto.trumpCard();
CardType trumpCardtype = trumpCard.getType();
int botCardsNumberAtTrickStart = dto.botCardsNumberAtTrickStart();

        List<Card> droppedCards = new ArrayList<>();

        for(Map.Entry<String, DroppedCardsInTrickDTO> map : droppedCardsInTrickDTOMap.entrySet()){
            DroppedCardsInTrickDTO droppedCardDTO = map.getValue();

            if(droppedCardDTO.getDroppedCardExists().equals(DroppedCardExists.YES)){
                Card droppedCard = droppedCardDTO.getDroppedCard();
                droppedCards.add(droppedCard);
            }
        }

        unusedCards.removeAll(new HashSet<>(droppedCards));

        boolean cardCanBeatDroppedCards = CardManager.cardCanBeatAll(card, new HashSet<>(droppedCards), trumpCardtype);

        if(!cardCanBeatDroppedCards){
            return 0.0;
        }


        List<Card> cardsCanBeat = findCardsCanBeat(unusedCards, card, trumpCardtype);
int botsNumber = dto.droppedCardsInTrickDTO().size();
int droppedCardsSize = droppedCards.size();
int botsNumberNotPlayedInTrick = botsNumber - droppedCardsSize;
int botCards = botsNumberNotPlayedInTrick * botCardsNumberAtTrickStart;
if(botsNumberNotPlayedInTrick == 0){
    return 1;
}
        if(unusedCards.isEmpty()){
            return 0;
        }

if(card.getType().equals(trumpCardtype) || botCardsNumberAtTrickStart == 1){

    return Math.pow(1 - (double) cardsCanBeat.size() / unusedCards.size(), botsNumberNotPlayedInTrick * botCardsNumberAtTrickStart);

}else{

return getPWin(unusedCards, cardsCanBeat, card, trumpCardtype, botCards);
}
    }


private static double calculateProbabilityToWinIfFirstToDrop(ProbabilityCalculatorDTO dto, Set<Card> unusedCards){
        Card firstDroppedCard = dto.card();

        CardType firstDroppedCardType = firstDroppedCard.getType();
        CardType trumpCardType = dto.trumpCard().getType();
boolean isLastTrickInRound = dto.botCardsNumberAtTrickStart() == 1;

        int botsNumber = dto.botsNumber();
int botCardsNumberAtTrickStart = dto.botCardsNumberAtTrickStart();

    if(firstDroppedCardType.equals(CardType.WIZARD))  return 1;

    if(firstDroppedCardType.equals(CardType.JESTER))  return 0;

    List<Card> cardsCanBeat = findCardsCanBeat(unusedCards, dto.card(), trumpCardType);
    int cardsCanBeatCount = cardsCanBeat.size();
    int botCards = botCardsNumberAtTrickStart * botsNumber;

    if (cardsCanBeatCount == 0) return 1.0;


if(firstDroppedCardType.equals(trumpCardType) || isLastTrickInRound){
    double pBeatCard = (double) cardsCanBeatCount / unusedCards.size();

    return Math.pow(1-pBeatCard, botCards);

}else{

    return getPWin(unusedCards, cardsCanBeat, firstDroppedCard, trumpCardType, botCards);
}
}

    private static double getPNoCardTypes(Set<Card> unusedCards, CardType cardType, int cards) {
        int cardsOfType = CardManager.countCardsOfType(unusedCards, cardType);

        return Math.pow(1 - (double) cardsOfType / cards, cards);
    }


    private static List<Card> countBeatCardsIfCardTrump(Set<Card> cards, Card card){
        if(card.getType().equals(CardType.JESTER)){
            return cards.stream().filter(c -> !c.getType().equals(CardType.JESTER)).toList();
        }
    return cards.stream().filter(c -> c.getType().equals(CardType.WIZARD)
            || c.getNumber() > card.getNumber() && c.getType().equals(card.getType())).toList();
    }

    private static List<Card> findCardsCanBeat(Set<Card> cards, Card cardToBeat, CardType trumpCardType){
        CardType cardToBeatType = cardToBeat.getType();

        if(cardToBeatType.equals(trumpCardType)){
            return countBeatCardsIfCardTrump(cards, cardToBeat);
        }

        return countBeatCardsIfCardNotTrump(cards, cardToBeat, trumpCardType);
    }

    private static List<Card> countBeatCardsIfCardNotTrump(Set<Card> cards, Card card, CardType trumpCardType){
return cards.stream().filter(c -> c.getType().equals(trumpCardType) ||
        c.getType().equals(card.getType()) && c.getNumber() > card.getNumber() || c.getType().equals(CardType.WIZARD)).toList();
    }

private static double getPWin(Set<Card> unusedCards, List<Card> cardsCanBeat,
                              Card card, CardType trumpCardType, int cards){

    double pNoCardTypes = getPNoCardTypes(unusedCards, card.getType(), cards);

    int trumpCardsNumber = cardsCanBeat.stream().filter(c -> c.getType().equals(trumpCardType)).toList().size();
    int wizardsNumber = cardsCanBeat.stream().filter(c -> c.getType().equals(CardType.WIZARD)).toList().size();
    int cardToDropTypeBeatCards = cardsCanBeat.size() - trumpCardsNumber - wizardsNumber;

    double base = 1 - (double) trumpCardsNumber / unusedCards.size();

    double pWin1 = 1 - pNoCardTypes *  (1 - Math.pow(base, cards));

    double pWin2 = Math.pow(1- (double) cardToDropTypeBeatCards / unusedCards.size(), cards);

    double pNoWizard = Math.pow(1- (double) wizardsNumber / unusedCards.size(), cards);
/*
   System.out.println("оппонент не имеет визарда: " + pNoWizard);
    System.out.println("у оппонента вероятно есть моя карта: " + pWin1);
    System.out.println("у оппонента нет сильней карта моего типа: " +pWin2);

 */

    double independenceFactor = 0.9;
    //return pNoWizard * (pWin2 * independenceFactor + pWin1 * (1 - independenceFactor));
    return pNoWizard * pWin2 * pWin1;
}

public static double calculateAbstractProbability(AbstractProbabilityDTO dto){
Set<Card> unusedCards = new HashSet<>(allPossibleCards);
unusedCards.removeAll(dto.ownCards());
unusedCards.remove(dto.trumpCard());

    List<Card> cardsCanBeat = CardManager.determineCardsBeatCard(unusedCards, dto.card(), dto.trumpCard().getType());

int botCards = dto.botCardsNumberAtTrickStart() * (dto.botsNumber() - 1);

if(cardsCanBeat.isEmpty()){
    return 100;
}
if(dto.card().getType().equals(CardType.JESTER)) {
    return 0;
}


return Math.pow(1 - ((double) cardsCanBeat.size() / unusedCards.size()), botCards);
}
}
