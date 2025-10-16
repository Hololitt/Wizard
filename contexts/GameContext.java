package JC.Training.src.WizardGame.contexts;

import JC.Training.src.WizardGame.CardType;
import JC.Training.src.WizardGame.IDGenerator;
import JC.Training.src.WizardGame.models.Card;
import JC.Training.src.WizardGame.models.GameBot;

import java.util.*;

public class GameContext {
    private final String gameId;

    private final Set<GameBot> gameBots;
  private final int totalRoundAmount;
    private final Set<Card> allCards;
    private int currentRoundNumber;

  private final Stack<RoundContext> roundContexts = new Stack<>();
 private Map<String, Integer> botScores = new HashMap<>();

    public GameContext(Set<GameBot> gameBots, int totalRoundsAmount){
        this.gameBots = gameBots;
        this.totalRoundAmount = totalRoundsAmount;
       gameId = IDGenerator.getNextGameId();
       allCards = initializeAllCards();
    }

private Set<Card> initializeAllCards(){
        Set<Card> cards = new HashSet<>();

        for(int i = 1; i <= 13; i++){
            Card blueCard = new Card(CardType.BLUE, i);
            Card redCard = new Card(CardType.RED, i);
            Card greenCard = new Card(CardType.GREEN, i);
            Card yellowCard = new Card(CardType.YELLOW, i);

                    cards.add(blueCard);
                    cards.add(redCard);
                    cards.add(greenCard);
                    cards.add(yellowCard);
        }

        for(int i = 1; i<=4; i++){
            cards.add(new Card(CardType.JESTER));
            cards.add(new Card(CardType.WIZARD, 14));
        }
        return cards;
}

public void updateCurrentRoundContext(RoundContext newRoundContext){
        roundContexts.pop();
        roundContexts.push(newRoundContext);
}

public void updateBotScores(Map<String, Integer> botScores){
        this.botScores = botScores;
}

    public Set<GameBot> getGameBots(){
        return gameBots;
    }

    public int getCurrentRoundNumber(){
        return currentRoundNumber;
    }

    public int getTotalRoundAmount(){
        return totalRoundAmount;
    }
    public String getPlayerIdMove(){
        return roundContexts.peek().lastWinnerDTO().getLastWinnerId();
    }
    public Set<Card> getAllCards(){
        return allCards;
    }

    public void setNewRoundContext(RoundContext roundContext){
        roundContexts.push(roundContext);
    }
    public RoundContext getCurrentRoundContext(){
        return roundContexts.peek();
    }

    public Map<String, Integer> getBotScores() {
        return botScores;
    }

    public Stack<RoundContext> getRoundContexts() {
        return roundContexts;
    }
    public int getBotAmount(){
        return gameBots.size();
    }
    public void incrementCurrentRoundNumber(){
        currentRoundNumber++;
    }
    public void setCurrentRoundNumber(int currentRoundNumber){
        this.currentRoundNumber = currentRoundNumber;
    }
}
