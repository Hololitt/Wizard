package WizardGame.models;

import WizardGame.enums.CardType;
import WizardGame.handlers.IDGenerator;

public class CardModel {
private final String id;
private final CardType modelCardType;
private final int modelCardNumber;

public CardModel(CardType modelCardType, int modelCardNumber){
    this.modelCardType = modelCardType;
    this.modelCardNumber = modelCardNumber;
    id = IDGenerator.getNextCardId();
}

    public String getId() {
        return id;
    }

    public CardType getModelCardType() {
        return modelCardType;
    }

    public int getModelCardNumber() {
        return modelCardNumber;
    }
}
