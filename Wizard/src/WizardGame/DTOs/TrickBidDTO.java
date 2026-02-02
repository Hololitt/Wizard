package WizardGame.DTOs;

import WizardGame.enums.TrickBidExists;

public record TrickBidDTO(TrickBidExists trickBidExists, Integer trickBid) {
}
