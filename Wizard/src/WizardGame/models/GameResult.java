package WizardGame.models;

import WizardGame.contexts.GameContext;

public record GameResult(GameBot winnerBot, GameContext gameContext, int drawAmount) {
}
