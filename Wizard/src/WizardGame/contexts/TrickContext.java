package WizardGame.contexts;

import WizardGame.models.Card;
import WizardGame.models.GameBot;

import java.util.Map;

public record TrickContext (Map<GameBot, Card> responses, Card firstDroppedCard, GameBot startingBot){
}
