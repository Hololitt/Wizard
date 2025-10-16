package JC.Training.src.WizardGame.contexts;

import JC.Training.src.WizardGame.models.Card;
import JC.Training.src.WizardGame.models.GameBot;

import java.util.Map;

public record TrickContext (Map<GameBot, Card> requests, Card firstDroppedCard, GameBot startingBot){
}
