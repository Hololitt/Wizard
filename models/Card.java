package JC.Training.src.WizardGame.models;

import JC.Training.src.WizardGame.CardType;

public class Card {
    private final CardType cardType;
    private int number;


    public Card(CardType cardType, int number){
        this.cardType = cardType;
        this.number = number;
    }

public Card(CardType cardType){
        this.cardType = cardType;
}

    public CardType getType() {
        return cardType;
    }

    public int getNumber() {
        return number;
    }

    public String toString(){
        return cardType + " " + number;
    }
}
