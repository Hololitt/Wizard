package WizardGame.DTOs;

import WizardGame.contexts.RoundContext;

import java.util.Map;
import java.util.Stack;

public record GameContextDTO(int totalRoundAmount, int currentRoundNumber,
                             Map<String, Integer> botScores, Stack<RoundContext> roundContexts, int botAmount) {
}
