package JC.Training.src.WizardGame;

public class IDGenerator {
    private static int playerIdCounter;
    private static int gameIdCounter;

    public static synchronized String getNextPlayerId(){
        return "ID: " + playerIdCounter++;
    }

    public static synchronized String getNextGameId(){
        return "ID: " + gameIdCounter++;
    }
}
