package WizardGame.DTOs;

import WizardGame.enums.DroppedCardExists;
import WizardGame.models.Card;

public class DroppedCardsInTrickDTO {
    public DroppedCardsInTrickDTO(){
        droppedCardExists = DroppedCardExists.NO;
    }

    public DroppedCardsInTrickDTO(Card droppedCard, boolean isFirstDroppedCard){
        this.droppedCard = droppedCard;
        droppedCardExists = DroppedCardExists.YES;
        this.isFirstDroppedCard = isFirstDroppedCard;
    }

    private final DroppedCardExists droppedCardExists;
    private Card droppedCard;
    private boolean isFirstDroppedCard;

    public DroppedCardExists getDroppedCardExists() {
        return droppedCardExists;
    }

    public Card getDroppedCard() {
        return droppedCard;
    }

    public boolean isFirstDroppedCard(){
        return isFirstDroppedCard;
    }
}
