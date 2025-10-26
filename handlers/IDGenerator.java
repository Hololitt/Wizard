package JC.Training.src.WizardGame.handlers;

public class IDGenerator {
    private static int playerIdCounter;
    private static int gameIdCounter;
private static int cardIdCounter;

    public static synchronized String getNextPlayerId(){
        return "ID: " + playerIdCounter++;
    }

    public static synchronized String getNextGameId(){
        return "ID: " + gameIdCounter++;
    }

    public static synchronized String getNextCardId(){return "ID: " + cardIdCounter++;}
}
