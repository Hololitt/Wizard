package JC.Training.src.WizardGame;

import JC.Training.src.WizardGame.DTOs.DroppedCardsInTrickDTO;
import JC.Training.src.WizardGame.DTOs.ProbabilityCalculatorDTO;
import JC.Training.src.WizardGame.contexts.GameContext;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.enums.Probability;
import JC.Training.src.WizardGame.handlers.GameHandler;
import JC.Training.src.WizardGame.handlers.GameManager;
import JC.Training.src.WizardGame.handlers.ProbabilityCalculator;
import JC.Training.src.WizardGame.models.Card;
import JC.Training.src.WizardGame.models.CardModel;
import JC.Training.src.WizardGame.models.GameBot;
import JC.Training.src.WizardGame.models.GameResult;
import JC.Training.src.WizardGame.strategies.*;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        CardModel dropCard = new CardModel(CardType.RED, 1);

        Set<CardModel> cardModels = Set.of(
               dropCard,
                new CardModel(CardType.RED, 1)
        );

        Set<Card> ownCards = GameManager.findCards(cardModels);

        Set<Card> usedCards = new HashSet<>();

        Map<String, DroppedCardsInTrickDTO> dropped = new HashMap<>();
        dropped.put("qweasd", new DroppedCardsInTrickDTO());
        dropped.put("qweddasd", new DroppedCardsInTrickDTO());
        dropped.put("qssswe", new DroppedCardsInTrickDTO());
        dropped.put("qadasdasdwe", new DroppedCardsInTrickDTO());

        ProbabilityCalculatorDTO dto1 = new ProbabilityCalculatorDTO(
                GameManager.findCard(dropCard),
                GameManager.findCard(new CardModel(CardType.YELLOW, 1)),
                usedCards,
                ownCards,
                dropped,
                2,
                true
        );

        ProbabilityCalculatorDTO dto2 = new ProbabilityCalculatorDTO(
                GameManager.findCard(dropCard),
                GameManager.findCard(new CardModel(CardType.GREEN, 1)),
                usedCards,
                ownCards,
                dropped,
                1,
                false
        );

double probability = ProbabilityCalculator.calculateProbabilityToBeat(dto1, Probability.TOTAL);

        System.out.println("Dropped Card: " + dto1.card());
        System.out.println("Trump card: " + dto1.trumpCard());
        System.out.println(probability);
    }

    static void handleGames(GameStrategy... strategies){
        List<GameBot> gameBots = new ArrayList<>();

        for(GameStrategy gs : strategies){
            gameBots.add(new GameBot(gs));
        }

        Map<GameBot, Integer> wins = initializeDefaultFullTrickWins(new HashSet<>(gameBots));

        int totalRounds = (int) Math.floor((double) 60 / gameBots.size());

        int games = 1000;
int drawAmount = 0;

int notEnough = 0;
int tooMuch = 0;
int perfekt = 0;

for(int i = 0; i < games; i++){
    GameContext gameContext = new GameContext(new HashSet<>(gameBots), totalRounds);
    GameHandler gameHandler = new GameHandler(gameContext);
    GameResult gameResult = gameHandler.start();

String botId = findGameBotIdByStrategy(new AdaptiveMasterV1(), gameBots);

for(RoundContext rc : gameResult.gameContext().getRoundContexts()){
    
int bids = rc.fullTrickBids().get(botId).trickBid();
   int totalWins = rc.fullTrickWins().get(botId);

   if(bids < totalWins){
       tooMuch++;
   }else if(bids > totalWins){
       notEnough++;
   }else{
       perfekt++;
   }
}
    if(gameResult.winnerBot() != null){
        wins.merge(gameResult.winnerBot(), 1, Integer::sum);
    }else{
        drawAmount += gameResult.drawAmount();
    }
}
        printBidWinsResults(notEnough, tooMuch, perfekt);
        System.out.println("games: " + games + "; total rounds in every game: " + totalRounds);
        System.out.println("------------------------------------------------");

     printBotResults(wins, games);

        System.out.println("Draws: " + drawAmount);
    }

    private static void printBotResults(Map<GameBot, Integer> wins, int games){
        LinkedHashMap<GameBot, Integer> sortedMap = wins.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        for(Map.Entry<GameBot, Integer> map : sortedMap.entrySet()){
            int winAmount = map.getValue();
            double winrate = ((double) winAmount /games) * 100;

            String rateFormat = String.format("%.2f", winrate);
            System.out.println(map.getKey().getStrategyName() + ": " + winAmount+ " wins;  winrate: " + rateFormat+"%");
        }
    }

    private static String findGameBotIdByStrategy(GameStrategy gameStrategy, List<GameBot> gameBots){
        for(GameBot gameBot : gameBots){
            if(gameBot.getStrategyName().equals(gameStrategy.getStrategyName())){
                return gameBot.getGameBotId();
            }
        }
        return "nothing found";
    }

    private static void printBidWinsResults(int notEnough, int tooMuch, int perfekt){
        int allResult = tooMuch + notEnough + perfekt;

        double tooMuchRate = ((double) tooMuch / allResult)*100;
        double notEnoughRate = ((double) notEnough / allResult)*100;
        double perfektRate = ((double) perfekt / allResult)*100;

String tooMuchF = String.format("%.2f", tooMuchRate);
String notEnoughF = String.format("%.2f", notEnoughRate);
        String perfektF = String.format("%.2f", perfektRate);

        System.out.println("профицит: " + tooMuchF+"%");
        System.out.println("дефицит: " + notEnoughF+"%");
        System.out.println("идеально: " + perfektF+"%");
    }

    static Map<GameBot, Integer> initializeDefaultFullTrickWins(Set<GameBot> gameBots){
        Map<GameBot, Integer> fullTrickWins = new HashMap<>();

        for (GameBot gameBot : gameBots) {
            fullTrickWins.put(gameBot, 0);
        }

        return fullTrickWins;
    }
}
