package JC.Training.src.WizardGame.models;

import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.DTOs.*;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.strategies.GameStrategy;

import java.util.*;

public class GameBot {
    private final String gameBotId;

private Set<Card> cards = new HashSet<>();
private final GameStrategy gameStrategy;

    public GameBot(GameStrategy gameStrategy){
        gameBotId = UUID.randomUUID().toString();
        this.gameStrategy = gameStrategy;
    }


    public Card beatCard(TrickContextDTO trickContextDTO, RoundContext roundContext,
                         Set<CardType> allowedResponses, GameContextDTO gameContextDTO){

        BeatCardContextDTO dto = new BeatCardContextDTO(gameBotId, trickContextDTO, roundContext, allowedResponses,
                cards, gameContextDTO);

     Card card = gameStrategy.beatCard(dto);

        cards.remove(card);
     return card;
    }

    public Card dropCard(RoundContext roundContext, GameContextDTO gameContextDTO){
    Card card = gameStrategy.dropCard(new DropCardContextDTO(gameBotId, roundContext, cards, gameContextDTO));
cards.remove(card);
        return card;
    }

    public Integer createTrickBids(CreateTrickBidsContextDTO createTrickBidsContextDTO){

        return gameStrategy.createTrickBids(createTrickBidsContextDTO);
    }

    public void setCards(Set<Card> cards){
        this.cards = cards;
    }

public Set<Card> getCards(){
        return cards;
}
    public String getGameBotId(){
        return gameBotId;
    }
    public String getStrategyName(){
        return gameStrategy.getStrategyName();
    }
}
