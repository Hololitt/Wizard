package JC.Training.src.WizardGame.models;

import JC.Training.src.WizardGame.contexts.GameContext;

public record GameResult(GameBot winnerBot, GameContext gameContext, int drawAmount) {
}
