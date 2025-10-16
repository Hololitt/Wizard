package JC.Training.src.WizardGame;

import JC.Training.src.WizardGame.DTOs.CreateTrickBidsContextDTO;
import JC.Training.src.WizardGame.DTOs.GameContextDTO;
import JC.Training.src.WizardGame.DTOs.LastWinnerDTO;
import JC.Training.src.WizardGame.DTOs.TrickContextDTO;
import JC.Training.src.WizardGame.contexts.GameContext;
import JC.Training.src.WizardGame.models.Card;
import JC.Training.src.WizardGame.models.GameBot;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.contexts.TrickContext;
import JC.Training.src.WizardGame.models.GameResult;

import java.util.*;

public class GameHandler {
    private final GameContext gameContext;
private final Set<GameBot> gameBots;
private final Set<Card> allCards;

    public GameHandler(GameContext gameContext){
        this.gameContext = gameContext;
        gameBots = gameContext.getGameBots();
        allCards = gameContext.getAllCards();
    }

    private GameBot defineGameBotById(String id){
        Optional<GameBot> gameHandlerOptional = gameContext.getGameBots()
                .stream()
                .filter(gh -> gh.getGameBotId().equals(id)).findFirst();

        return gameHandlerOptional.get();
    }

    private Set<CardType> defineAllowedResponseCardTypes(Card firstDroppedCard) {
        CardType cardType = firstDroppedCard.getType();


        if(cardType.equals(CardType.WIZARD) || cardType.equals(CardType.JESTER)){
            return new HashSet<>(List.of(CardType.values()));
        }else{
            return new HashSet<>(List.of(firstDroppedCard.getType(), CardType.WIZARD, CardType.JESTER));
        }
    }

    public GameResult start(){

int totalRoundAmount = gameContext.getTotalRoundAmount();
int currentRoundNumber = 1;
gameContext.setCurrentRoundNumber(currentRoundNumber);

while(currentRoundNumber < totalRoundAmount){

    updateCards(currentRoundNumber);

RoundContext roundContext = initializeRoundContext(currentRoundNumber);

    gameContext.setNewRoundContext(roundContext);

if(currentRoundNumber == 1){
    roundContext.lastWinnerDTO().setLastWinnerExists(LastWinnerExists.NO);
}

    for(int i = 0; i < currentRoundNumber; i++){
        GameBot startingBot = defineGameBotById(determinePlayerIdMove());
        Card firstDroppedCard = startingBot.dropCard(roundContext, createGameContextDTO());

        Map<GameBot, Card> requests = selectCardToBeatRequests(startingBot, firstDroppedCard);

        TrickContext trickContext = new TrickContext(requests, firstDroppedCard, startingBot);

        GameBot winnerGameBot = defineTrickWinner(trickContext);

        String gameBotId = winnerGameBot.getGameBotId();

        roundContext.lastWinnerDTO().setLastWinnerId(gameBotId);
        roundContext.fullTrickWins().merge(winnerGameBot.getGameBotId(), 1, Integer::sum);

gameContext.updateCurrentRoundContext(roundContext);

    }
    updateBotScores();

    gameContext.incrementCurrentRoundNumber();
    currentRoundNumber++;
}
        GameBot winnerBot = null;
        List<String> winners = getWinners();


        int drawAmount = (winners.size() > 1) ? 1 : 0;
        if (drawAmount == 0) {
            winnerBot = defineGameBotById(winners.getFirst());
        }
//printScores();
        return new GameResult(winnerBot, createGameContextDTO(), drawAmount);
    }

    private List<String> getWinners() {
        int winningScore = Integer.MIN_VALUE;
        List<String> winners = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : gameContext.getBotScores().entrySet()) {
            int score = entry.getValue();

            if (score > winningScore) {
                winningScore = score;
                winners.clear();
                winners.add(entry.getKey());
            } else if (score == winningScore) {
                winners.add(entry.getKey());
            }
        }
        return winners;
    }

    private void updateBotScores(){
Map<String, Integer> fullStrickBids = gameContext.getRoundContexts().peek().fullTrickBids();
Map<String, Integer> fullStrickWins = gameContext.getRoundContexts().peek().fullTrickWins();
Map<String, Integer> updatedBotScores = gameContext.getBotScores();

for(Map.Entry<String, Integer> bids : fullStrickBids.entrySet()){
    String botId = bids.getKey();
    Integer bidsAmount = bids.getValue();
Integer winsAmount = fullStrickWins.get(botId);

    int earnedScore;

    if(winsAmount.equals(bidsAmount)){
        earnedScore = 20 + bidsAmount * 10;
    }else{
       int bidsWinsDif = Math.abs(bidsAmount - winsAmount);

       earnedScore = -10 * bidsWinsDif;
    }

    updatedBotScores.merge(botId, earnedScore, Integer::sum);

    gameContext.updateBotScores(updatedBotScores);
}


}

    private RoundContext initializeRoundContext(int roundNumber){
        Card trumpCard = generateRandomTrumpCard();
        Map<String, Integer> fullTrickBids = initializeFullTrickBids(trumpCard);
        LastWinnerDTO lastWinnerDTO;

        if(roundNumber == 1){
            lastWinnerDTO = new LastWinnerDTO();
        }else{
            lastWinnerDTO = gameContext.getRoundContexts().peek().lastWinnerDTO();
        }

        Map<String, Integer> defaultFullTrickWins = initializeDefaultFullTrickWins();

        return new RoundContext(roundNumber, trumpCard, fullTrickBids, defaultFullTrickWins, lastWinnerDTO);
    }

    private Map<String, Integer> initializeDefaultFullTrickWins(){
        Map<String, Integer> fullTrickWins = new HashMap<>();
        List<GameBot> gameBotList = new ArrayList<>(gameContext.getGameBots());

        for (GameBot gameBot : gameBotList) {
            fullTrickWins.put(gameBot.getGameBotId(), 0);
        }

        return fullTrickWins;
    }

    private GameContextDTO createGameContextDTO(){
        int currentRoundNumber = gameContext.getCurrentRoundNumber();

        return new GameContextDTO(gameContext.getTotalRoundAmount(),
                currentRoundNumber, gameContext.getBotScores(), gameContext.getBotAmount());
    }

    private Map<String, Integer> initializeFullTrickBids(Card trumpCard){
        Map<String, Integer> fullTrickBids = new HashMap<>();

        for(GameBot gameBot : gameContext.getGameBots()){

            GameContextDTO gameContextDTO = createGameContextDTO();

            CreateTrickBidsContextDTO dto = new CreateTrickBidsContextDTO(gameContextDTO, fullTrickBids,
                    trumpCard, gameBot.getCards());

            Integer wonTricks = gameBot.createTrickBids(dto);

            fullTrickBids.put(gameBot.getGameBotId(), wonTricks);
        }

        return fullTrickBids;
    }

    private GameBot defineTrickWinner(TrickContext trickContext){
Map<GameBot, Card> requests = trickContext.requests();

Card winningCard = trickContext.firstDroppedCard();
Card trumpCard = gameContext.getRoundContexts().peek().trumpCard();

GameBot winningGameBot = trickContext.startingBot();

for(Map.Entry<GameBot, Card> request : requests.entrySet()){
    Card card = request.getValue();

    Card wonCard = defineWinningCard(card, winningCard, trumpCard);

    if(!wonCard.equals(winningCard)){
        winningCard = wonCard;
        winningGameBot = request.getKey();
    }

}

return winningGameBot;
    }

    private Card defineWinningCard(Card droppedCard, Card currentWinningCard, Card trumpCard){
        CardType cardType = droppedCard.getType();
        CardType winningCardType = currentWinningCard.getType();

        Card winningCard = currentWinningCard;

        if(cardType.equals(winningCardType)){
                if(droppedCard.getNumber() > winningCard.getNumber()){
                    winningCard = droppedCard;
                }
        }else{
            if(winningCardType.equals(CardType.JESTER)){
                winningCard = droppedCard;
            }
            if(!winningCardType.equals(CardType.WIZARD)){
                if(cardType.equals(CardType.WIZARD)){
                    winningCard = droppedCard;
                }
                if(cardType.equals(trumpCard.getType())){
                    winningCard = droppedCard;
                }
            }
        }
        return winningCard;
    }


  private Map<GameBot, Card> selectCardToBeatRequests(GameBot startingBot, Card firstDroppedCard){
    List<GameBot> gameBotsList = getPlayersInClockwiseOrder(gameContext.getGameBots().stream().toList(),
            startingBot);

    gameBotsList.remove(startingBot);

    Map<GameBot, Card> finalRequests = new HashMap<>();

TrickContextDTO trickContext = new TrickContextDTO(new ArrayList<>(), firstDroppedCard);


    for(GameBot gameBot : gameBotsList){
        Set<Card> gameBotCards = gameBot.getCards();
        Set<CardType> allowedCardTypes = CardManager.defineAllowedResponseCardTypes(firstDroppedCard, gameBotCards);

  Card beatCard = gameBot.beatCard(trickContext, gameContext.getCurrentRoundContext(), allowedCardTypes, createGameContextDTO());

 if(!allowedCardTypes.contains(beatCard.getType())){
     System.out.println("происходит что то страшное");
     System.out.println("Фигню бросил бот со стратегией " + gameBot.getStrategyName());
     System.exit(0);
 }

trickContext.addRequest(beatCard);

finalRequests.put(gameBot, beatCard);
    }

return finalRequests;
    }

    private boolean cardsContainAllowedType(Set<Card> cards, Set<CardType> cardTypes){
        for(Card c : cards){
            if(cardTypes.contains(c.getType())){
                return true;
            }
        }
        return false;
    }

    public List<GameBot> getPlayersInClockwiseOrder(List<GameBot> allBots, GameBot startingBot) {
        int startIndex = allBots.indexOf(startingBot);

        List<GameBot> ordered = new ArrayList<>(allBots.subList(startIndex, allBots.size()));

        if (startIndex > 0) {
            ordered.addAll(allBots.subList(0, startIndex));
        }

        return ordered;
    }


    private String determinePlayerIdMove(){
LastWinnerDTO lastWinnerDTO = gameContext.getCurrentRoundContext().lastWinnerDTO();
LastWinnerExists lastWinnerExists = lastWinnerDTO.getLastWinnerExists();

if(lastWinnerExists.equals(LastWinnerExists.YES)){
return lastWinnerDTO.getLastWinnerId();
}else{
    List<GameBot> gameBotsList = new ArrayList<>(gameBots);
    Collections.shuffle(gameBotsList);

    return gameBotsList.getFirst().getGameBotId();
}
    }

    private void updateCards(int roundNumber){
        List<Card> allCardsList = new ArrayList<>(allCards);
        Collections.shuffle(allCardsList);

        for (GameBot gameBot : gameBots) {
            Set<Card> botCards = new HashSet<>();

            for (int i = 0; i < roundNumber; i++) {
                Card card = allCardsList.getFirst();
              botCards.add(card);
              allCardsList.remove(card);
            }

            gameBot.setCards(botCards);
        }
    }

    private Card generateRandomTrumpCard(){
        List<Card> cardList = new ArrayList<>(allCards);
        Collections.shuffle(cardList);

    return cardList.getFirst();
    }


    public GameContext getGameContext(){
        return gameContext;
    }

    private void printScores(){
        for(Map.Entry<String, Integer> scores : gameContext.getBotScores().entrySet()){
            System.out.println(defineGameBotById(scores.getKey()).getStrategyName() + "; Score: " + scores.getValue());
        }
    }
}
