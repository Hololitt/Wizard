package JC.Training.src.WizardGame.DTOs;

import JC.Training.src.WizardGame.LastWinnerExists;

public class LastWinnerDTO {
    private LastWinnerExists lastWinnerExists;
    private String lastWinnerId;

    public LastWinnerDTO(){
        lastWinnerExists = LastWinnerExists.YES;
    }

public LastWinnerDTO(LastWinnerExists lastWinnerExists, String lastWinnerId){
        this.lastWinnerExists = lastWinnerExists;
        this.lastWinnerId = lastWinnerId;
}

    public void setLastWinnerExists(LastWinnerExists lastWinnerExists){
        this.lastWinnerExists = lastWinnerExists;
    }
    public void setLastWinnerId(String lastWinnerId){
        this.lastWinnerId = lastWinnerId;
    }

    public LastWinnerExists getLastWinnerExists() {
        return lastWinnerExists;
    }

    public String getLastWinnerId() {
        return lastWinnerId;
    }
}
