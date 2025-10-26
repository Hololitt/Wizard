package JC.Training.src.WizardGame.handlers;

import JC.Training.src.WizardGame.DTOs.DroppedCardsInTrickDTO;
import JC.Training.src.WizardGame.DTOs.ProbabilityCalculatorDTO;
import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.enums.Probability;
import JC.Training.src.WizardGame.models.Card;

import java.util.*;

public class ProbabilityCalculator {
private final static Set<Card> allPossibleCards = GameManager.getAllPossibleCards();

    public static double calculateProbabilityToBeat(ProbabilityCalculatorDTO dto, Probability probability){
boolean isFirstToDrop = dto.isFirstToDrop();
Set<Card> unusedCards = new HashSet<>(new HashSet<>(allPossibleCards));

unusedCards.removeAll(dto.usedCards());
unusedCards.removeAll(dto.ownCards());
unusedCards.remove(dto.trumpCard());

int probabilityInterpreter = 1;

if(probability.equals(Probability.TOTAL)){
    probabilityInterpreter = 100;
}

if(isFirstToDrop){
    return calculateProbabilityToWinIfFirstToDrop(dto, unusedCards) * probabilityInterpreter;

}else{
    return calculateProbabilityToWinIfNotFirstToDrop(dto, unusedCards) * probabilityInterpreter;
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

            if(droppedCardDTO.isFirstDroppedCard()){
                Card droppedCard = droppedCardDTO.getDroppedCard();
                droppedCards.add(droppedCard);
            }
        }

        unusedCards.removeAll(new HashSet<>(droppedCards));

        boolean cardCanBeatDroppedCards = CardManager.cardCanBeatAll(card, new HashSet<>(droppedCards), trumpCardtype);

        if(!cardCanBeatDroppedCards){
            return 0;
        }


        List<Card> cardsCanBeat = findCardsCanBeat(unusedCards, card, trumpCardtype);
int botsNumber = dto.droppedCardsInTrickDTO().size() + 1;
int droppedCardsSize = droppedCards.size() + 1;
int botsNumberNotPlayedInTrick = botsNumber - droppedCardsSize;

if(botsNumberNotPlayedInTrick == 0){
    return 1;
}

if(card.getType().equals(trumpCardtype) || botCardsNumberAtTrickStart == 1){
    return Math.pow(1- (double) cardsCanBeat.size() /unusedCards.size(), botsNumberNotPlayedInTrick * botCardsNumberAtTrickStart);

}else{
return getPWin(unusedCards, cardsCanBeat, card, trumpCardtype, botCardsNumberAtTrickStart, botsNumberNotPlayedInTrick);
}
    }

private static double calculateProbabilityToWinIfFirstToDrop(ProbabilityCalculatorDTO dto, Set<Card> unusedCards){
        Card firstDroppedCard = dto.card();

        CardType firstDroppedCardType = firstDroppedCard.getType();
        CardType trumpCardType = dto.trumpCard().getType();

        int botsNumber = dto.droppedCardsInTrickDTO().size();
int botCardsNumberAtTrickStart = dto.botCardsNumberAtTrickStart();

    if(firstDroppedCardType.equals(CardType.WIZARD))  return 1;

    if(firstDroppedCardType.equals(CardType.JESTER) && botsNumber > 4)  return 0;

    List<Card> cardsCanBeat = findCardsCanBeat(unusedCards, dto.card(), trumpCardType);
    int cardsCanBeatCount = cardsCanBeat.size();
    if (cardsCanBeatCount == 0) return 1.0;

if(firstDroppedCardType.equals(trumpCardType)){
    double pBeatCard = (double) cardsCanBeatCount / unusedCards.size();
int totalCardsCount = botCardsNumberAtTrickStart * botsNumber;

    return Math.pow(1-pBeatCard, totalCardsCount);

}else{

    return getPWin(unusedCards, cardsCanBeat, firstDroppedCard, trumpCardType, botCardsNumberAtTrickStart, botsNumber);
}
}


    private static double getPNoCardTypes(Set<Card> unusedCards, CardType cardType, int players, int cardsPerPlayer) {
       int unusedCardsSize = unusedCards.size();
       int cardsOfTypeCount = CardManager.countCardsOfType(unusedCards, cardType);
double p =  Math.pow(1 - (double) cardsOfTypeCount / unusedCardsSize, cardsPerPlayer * players);
        System.out.println((double)cardsOfTypeCount / unusedCardsSize);
        System.out.println("шанс того, что ни у кого не будет типа моей карты: " + p);
        return p;
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
                              Card card, CardType trumpCardType, int botCardsNumberAtTrickStart, int botsNumberNotPlayedInTrick){

    double pNoCardTypes = getPNoCardTypes(unusedCards, card.getType(), botsNumberNotPlayedInTrick, botCardsNumberAtTrickStart);

    int trumpCardsNumber = cardsCanBeat.stream().filter(c -> c.getType().equals(trumpCardType)).toList().size();
    int wizardsNumber = cardsCanBeat.stream().filter(c -> c.getType().equals(CardType.WIZARD)).toList().size();

    int cardToDropTypeBeatCards = cardsCanBeat.size() - trumpCardsNumber - wizardsNumber;

    double pWin1 = 1 - pNoCardTypes *  (1 - Math.pow( 1 - (double) trumpCardsNumber / unusedCards.size(),
            botsNumberNotPlayedInTrick * botCardsNumberAtTrickStart));

    double pWin2 = Math.pow(1- (double) cardToDropTypeBeatCards / unusedCards.size(),
            botsNumberNotPlayedInTrick * botCardsNumberAtTrickStart);

    double pNoWizard = Math.pow(1- (double) wizardsNumber / unusedCards.size(),
            botsNumberNotPlayedInTrick * botCardsNumberAtTrickStart);

    System.out.println("оппонент не имеет визарда: " + pNoWizard);
    System.out.println("у оппонента вероятно есть моя карта: " + pWin1);
    System.out.println("у оппонента нет сильней карта моего типа: " +pWin2);
    return pNoWizard * pWin2 * pWin1;
}
}
