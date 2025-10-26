package JC.Training.src.WizardGame.handlers;

import JC.Training.src.WizardGame.contexts.GameContext;
import JC.Training.src.WizardGame.models.GameBot;
import JC.Training.src.WizardGame.models.GameResult;
import JC.Training.src.WizardGame.strategies.GameStrategy;

import java.util.*;

public class BotTournament {

    public static void runTournament(List<GameStrategy> strategies) {
        Map<String, BotStats> allStats = new HashMap<>();

        int[] roundOptions = {10, 20, 30};

        for (int rounds : roundOptions) {
            System.out.println("=== TOURNAMENT: " + rounds + " rounds per game ===");

            for (int i = 0; i < strategies.size(); i++) {
                for (int j = i + 1; j < strategies.size(); j++) {
                    GameStrategy s1 = strategies.get(i);
                    GameStrategy s2 = strategies.get(j);

                    GameBot bot1 = new GameBot(s1);
                    GameBot bot2 = new GameBot(s2);

                    for (int game = 0; game < 1000; game++) {
                        GameContext context = new GameContext(new HashSet<>(List.of(bot1, bot2)), rounds);
                        GameHandler handler = new GameHandler(context);
                        GameResult result = handler.start();

                        updateStats(allStats, result, bot1, bot2);
                    }
                }
            }
        }

        printStats(allStats);
    }

    private static void updateStats(Map<String, BotStats> statsMap, GameResult result, GameBot bot1, GameBot bot2) {
        String name1 = bot1.getStrategyName();
        String name2 = bot2.getStrategyName();

        int score1 = result.gameContext().getBotScores().get(bot1.getGameBotId());
        int score2 = result.gameContext().getBotScores().get(bot2.getGameBotId());

        statsMap.putIfAbsent(name1, new BotStats(name1));
        statsMap.putIfAbsent(name2, new BotStats(name2));

        BotStats stat1 = statsMap.get(name1);
        BotStats stat2 = statsMap.get(name2);

        stat1.addGame(score1);
        stat2.addGame(score2);

        if (score1 > score2) {
            stat1.addWin();
            stat2.addLoss();
        } else if (score2 > score1) {
            stat2.addWin();
            stat1.addLoss();
        } else {
            stat1.addDraw();
            stat2.addDraw();
        }
    }

    private static void printStats(Map<String, BotStats> statsMap) {
        System.out.println("\n=== FINAL TOURNAMENT RESULTS ===");
        statsMap.values().stream()
                .sorted(Comparator.comparingDouble(BotStats::winRate).reversed())
                .forEach(System.out::println);
    }

    public static class BotStats {
        private final String name;
        private int totalScore = 0;
        private int games = 0;
        private int wins = 0;
        private int losses = 0;
        private int draws = 0;

        public BotStats(String name) {
            this.name = name;
        }

        public void addGame(int score) {
            totalScore += score;
            games++;
        }

        public void addWin() {
            wins++;
        }

        public void addLoss() {
            losses++;
        }

        public void addDraw() {
            draws++;
        }

        public double avgScore() {
            return games == 0 ? 0 : (double) totalScore / games;
        }

        public double winRate() {
            return games == 0 ? 0 : (double) wins / games * 100.0;
        }

        @Override
        public String toString() {
            return String.format("Bot: %-15s | Games: %6d | Wins: %5d | Draws: %5d | WinRate: %6.2f%% | Avg Score: %8.2f | Total Score: %8d",
                    name, games, wins, draws, winRate(), avgScore(), totalScore);
        }
    }
}


