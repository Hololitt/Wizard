package WizardGame.models;

import WizardGame.enums.CardType;

import java.util.Objects;

public class Card {
    private final String id;
    private final CardType cardType;
    private final int number;


    public Card(String id, CardType cardType, int number){
        this.cardType = cardType;
        this.number = number;
        this.id = id;
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

    public String getId(){
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Card card)) return false;
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
