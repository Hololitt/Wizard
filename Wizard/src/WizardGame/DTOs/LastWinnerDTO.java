package WizardGame.DTOs;

import WizardGame.enums.LastWinnerExists;

public class LastWinnerDTO {
    private LastWinnerExists lastWinnerExists;
    private String lastWinnerId;

    public LastWinnerDTO(){
        lastWinnerExists = LastWinnerExists.NO;
    }

public LastWinnerDTO(LastWinnerExists lastWinnerExists, String lastWinnerId){
        this.lastWinnerExists = lastWinnerExists;
        this.lastWinnerId = lastWinnerId;
}

    public void setLastWinnerExists(LastWinnerExists lastWinnerExists){
        this.lastWinnerExists = lastWinnerExists;
    }


    public void setLastWinnerDTO(LastWinnerDTO lastWinnerDTO){
        lastWinnerExists = lastWinnerDTO.lastWinnerExists;
        lastWinnerId = lastWinnerDTO.lastWinnerId;
    }

    public LastWinnerExists getLastWinnerExists() {
        return lastWinnerExists;
    }

    public String getLastWinnerId() {
        return lastWinnerId;
    }
}
