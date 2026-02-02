package WizardGame.contexts;

import WizardGame.handlers.GameManager;
import WizardGame.handlers.IDGenerator;
import WizardGame.models.Card;
import WizardGame.models.GameBot;

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
       allCards = GameManager.getAllPossibleCards();
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
