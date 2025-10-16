package JC.Training.src.WizardGame.strategies;


import JC.Training.src.WizardGame.CardType;
import JC.Training.src.WizardGame.DTOs.BeatCardContextDTO;
import JC.Training.src.WizardGame.DTOs.CreateTrickBidsContextDTO;
import JC.Training.src.WizardGame.DTOs.DropCardContextDTO;
import JC.Training.src.WizardGame.models.Card;

import java.util.*;
import java.util.stream.Collectors;

public class ChatGPTStrategy implements GameStrategy{
    private final DefaultStrategy defaultStrategy = new DefaultStrategy();

    @Override
    public String getStrategyName() {
        return "ChatGPT_V1";
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        Card trumpCard = ctx.trumpCard();
        Set<Card> cards = ctx.ownCards();

        double bidScore = 0;

        for (Card card : cards) {
            if (isWizard(card)) {
                bidScore += 1.0;
            } else if (isTrump(card, trumpCard) && card.getNumber() >= 11) {
                bidScore += 0.9;
            } else if (!isJester(card) && card.getNumber() >= 11) {
                bidScore += 0.6;
            } else if (!isJester(card) && card.getNumber() >= 8) {
                bidScore += 0.3;
            }
        }

        return Math.max(0, (int) Math.floor(bidScore));
    }

    @Override
    public Card dropCard(DropCardContextDTO ctx) {
        int ownBid = ctx.roundContext().fullTrickBids().getOrDefault(ctx.ownId(), 0);
        int ownWins = ctx.roundContext().fullTrickWins().getOrDefault(ctx.ownId(), 0);
        Card trumpCard = ctx.roundContext().trumpCard();
        Set<Card> ownCards = ctx.ownCards();

        // Без шанса на null: карта всегда есть
        if (ownBid == 0 || ownWins >= ownBid) {
            return getWeakestCard(ownCards, trumpCard);
        } else {
            return getStrongestCard(ownCards, trumpCard);
        }
    }

    @Override
    public Card beatCard(BeatCardContextDTO ctx) {
        Card cardToBeat = ctx.trickContextDTO().firstDroppedCard();
        int ownBid = ctx.roundContext().fullTrickBids().getOrDefault(ctx.ownId(), 0);
        int ownWins = ctx.roundContext().fullTrickWins().getOrDefault(ctx.ownId(), 0);
        Card trumpCard = ctx.roundContext().trumpCard();
        Set<Card> hand = ctx.ownCards();

        if (ownWins >= ownBid) {
            return getWeakestCard(hand, trumpCard); // Сбрасываем, не бьём
        }

        // Ищем все карты, которые могут побить
        List<Card> beatingCards = hand.stream()
                .filter(c -> canBeat(c, cardToBeat, trumpCard))
                .collect(Collectors.toList());

        if (!beatingCards.isEmpty()) {
            // Побить минимальной из подходящих
            return getWeakestCard(beatingCards, trumpCard);
        } else {
            // Побить нельзя — сбрасываем слабейшую
            return getWeakestCard(hand, trumpCard);
        }
    }

    // === Вспомогательные методы ===

    private boolean isWizard(Card card) {
        return card.getType() == CardType.WIZARD;
    }

    private boolean isJester(Card card) {
        return card.getType() == CardType.JESTER;
    }

    private boolean isTrump(Card card, Card trumpCard) {
        return trumpCard != null && card.getType() == trumpCard.getType();
    }

    private int cardStrength(Card card, Card trumpCard) {
        if (isWizard(card)) return 1000;
        if (isJester(card)) return -1;
        if (isTrump(card, trumpCard)) return 100 + card.getNumber();
        return card.getNumber();
    }

    private Card getWeakestCard(Collection<Card> cards, Card trumpCard) {
        return cards.stream()
                .min(Comparator.comparingInt(card -> cardStrength(card, trumpCard)))
                .orElseGet(() -> cards.iterator().next()); // fallback: хотя бы что-то
    }

    private Card getStrongestCard(Collection<Card> cards, Card trumpCard) {
        return cards.stream()
                .max(Comparator.comparingInt(card -> cardStrength(card, trumpCard)))
                .orElseGet(() -> cards.iterator().next()); // fallback: хотя бы что-то
    }

    private boolean canBeat(Card attacker, Card defender, Card trumpCard) {
        if (isWizard(attacker)) return true;
        if (isJester(attacker)) return false;

        if (isWizard(defender)) return false;
        if (isJester(defender)) return true;

        boolean attackerIsTrump = isTrump(attacker, trumpCard);
        boolean defenderIsTrump = isTrump(defender, trumpCard);

        if (attacker.getType() == defender.getType()) {
            return attacker.getNumber() > defender.getNumber();
        }

        return attackerIsTrump && !defenderIsTrump;
    }
}
