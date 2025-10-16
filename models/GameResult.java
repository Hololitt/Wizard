package JC.Training.src.WizardGame.models;

import JC.Training.src.WizardGame.DTOs.GameContextDTO;

public record GameResult(GameBot winnerBot, GameContextDTO gameContextDTO, int drawAmount) {
}
