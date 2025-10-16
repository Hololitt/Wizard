package JC.Training.src.WizardGame;

import JC.Training.src.WizardGame.contexts.GameContext;
import JC.Training.src.WizardGame.models.GameBot;
import JC.Training.src.WizardGame.models.GameResult;
import JC.Training.src.WizardGame.strategies.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Main {
    public static void main(String[] args) {


        GameStrategy s1 = new AnalystV1();
GameStrategy s2 = new AnalystV1();



 BotTournament.runTournament(new ArrayList<>(List.of(new AnalystV1(), new MetaCrusherV1(),
      new ChatGPTV4(), new Version2Strategy(), new ChatGPTV2Strategy(), new ChatGPTV3Strategy(), new Hybrid())));



    }



    static void handleGame(GameStrategy gs1, GameStrategy gs2){
        GameBot g1 = new GameBot(gs1);
        GameBot g2 = new GameBot(gs2);


        System.out.println("-----------------------------");
        int bot1 = 0;
        int bot2 = 0;
        int draws = 0;
        int totalRounds = 20;

        for(int i = 0; i < 1000; i++){
            GameContext gameContext = new GameContext(new HashSet<>(List.of(g2, g1)), totalRounds);
            GameHandler gameHandler = new GameHandler(gameContext);
            GameResult gameResult = gameHandler.start();


            if(gameResult.winnerBot() == null){
                draws++;
            }else{
                if(gameResult.winnerBot().getStrategyName().equals(g1.getStrategyName())){
                    bot1++;
                }else{
                    bot2++;
                }
            }
        }
        System.out.println("amount of rounds in every game: " + totalRounds);
        System.out.println(g1.getStrategyName() + " amount of wins: " + bot1);
        System.out.println(g2.getStrategyName() + " amount of wins: " + bot2);
        System.out.println("draws: " + draws);
    }
}
