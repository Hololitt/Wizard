package JC.Training.src.WizardGame.models;

import JC.Training.src.WizardGame.IDGenerator;

public class Player {
    private final String id;

    private int winsAmount;
    private int lossesAmount;
private int drawsAmount;

    public Player(){
id = IDGenerator.getNextPlayerId();
    }

public void incrementWinsAmount(){
        winsAmount++;
}

    public void incrementLossesAmount(){
        lossesAmount++;
    }

public void incrementDrawsAmount(){
        drawsAmount++;
}

    public String getId() {
        return id;
    }

    public int getWinsAmount() {
        return winsAmount;
    }

    public int getLossesAmount() {
        return lossesAmount;
    }

    public int getDrawsAmount() {
        return drawsAmount;
    }
}
