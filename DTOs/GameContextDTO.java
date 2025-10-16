package JC.Training.src.WizardGame.DTOs;

import java.util.Map;

public record GameContextDTO(int totalRoundAmount, int currentRoundNumber, Map<String, Integer> botScores, int botAmount) {
}
