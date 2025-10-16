package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.CardManager;
import JC.Training.src.WizardGame.CardType;
import JC.Training.src.WizardGame.DTOs.BeatCardContextDTO;
import JC.Training.src.WizardGame.DTOs.CreateTrickBidsContextDTO;
import JC.Training.src.WizardGame.DTOs.DropCardContextDTO;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.models.Card;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ChatGPTV2Strategy implements GameStrategy{

    @Override
    public String getStrategyName() {
        return "ChatGPT_V2";
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        Set<Card> cards = ctx.ownCards();
        Card trumpCard = ctx.trumpCard();
        int roundNumber = ctx.gameContextDTO().currentRoundNumber();

        int bid = 0;

        for (Card card : cards) {
            CardType type = card.getType();

            if (type == CardType.WIZARD) {
                bid++;
            } else if (type == trumpCard.getType()) {
                if (card.getNumber() > 6 || roundNumber <= 2) {
                    bid++;
                }
            } else if (type != CardType.JESTER && card.getNumber() >= 11) {
                bid++;
            }
        }

        // ¬ первом раунде с 1 картой Ч не делаем ставку, если карта слаба€
        if (cards.size() == 1 && bid > 0 && cards.iterator().next().getType() == CardType.JESTER) {
            bid = 0;
        }

        return bid;
    }

    @Override
    public Card dropCard(DropCardContextDTO ctx) {

        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();

        int ownWins = round.fullTrickWins().getOrDefault(ownId, 0);
        int ownBid = round.fullTrickBids().getOrDefault(ownId, 0);

        Set<Card> ownCards = ctx.ownCards();
        Card trumpCard = round.trumpCard();


        if (ownWins >= ownBid) {
            return getWeakestCard(ownCards, trumpCard);
        }

        return getStrongestCard(ownCards, trumpCard);
    }

    @Override
    public Card beatCard(BeatCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();

        int ownWins = round.fullTrickWins().getOrDefault(ownId, 0);
        int ownBid = round.fullTrickBids().getOrDefault(ownId, 0);
        Card trumpCard = round.trumpCard();

        Card toBeat = ctx.trickContextDTO().firstDroppedCard();
        Set<Card> hand = ctx.ownCards();

        // ? ќграничение по допустимым картам
        Set<CardType> allowedTypes = CardManager.defineAllowedResponseCardTypes(toBeat, hand);
        Set<Card> allowedCards = CardManager.determineAllowedCards(allowedTypes, hand);

        if (allowedCards.isEmpty()) {
            // fallback: защищаемс€ от пустого набора
            return getWeakestCard(hand, trumpCard);
        }

        if (ownWins >= ownBid) {
            return getWeakestCard(allowedCards, trumpCard);
        }

        List<Card> beatingCards = allowedCards.stream()
                .filter(card -> canBeat(card, toBeat, trumpCard))
                .sorted(Comparator.comparingInt(card -> cardStrength(card, trumpCard)))
                .toList();

        if (!beatingCards.isEmpty()) {
            return beatingCards.getFirst();
        }

        return getWeakestCard(allowedCards, trumpCard);
    }

    // ===== ¬спомогательные методы =====

    private boolean isWizard(Card c) {
        return c.getType() == CardType.WIZARD;
    }

    private boolean isJester(Card c) {
        return c.getType() == CardType.JESTER;
    }

    private boolean isTrump(Card c, Card trump) {
        return trump != null && c.getType() == trump.getType();
    }

    private int cardStrength(Card c, Card trump) {
        if (isWizard(c)) return 1000;
        if (isJester(c)) return -1;
        if (isTrump(c, trump)) return 100 + c.getNumber();
        return c.getNumber();
    }

    private Card getWeakestCard(Set<Card> cards, Card trump) {
        return cards.stream()
                .min(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .orElseThrow();
    }

    private Card getStrongestCard(Set<Card> cards, Card trump) {
        return cards.stream()
                .max(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .orElseThrow();
    }

    private boolean canBeat(Card attacker, Card defender, Card trump) {
        if (isWizard(attacker)) return true;
        if (isJester(attacker)) return false;

        if (isWizard(defender)) return false;
        if (isJester(defender)) return true;

        boolean attackerIsTrump = isTrump(attacker, trump);
        boolean defenderIsTrump = isTrump(defender, trump);

        if (attacker.getType() == defender.getType()) {
            return attacker.getNumber() > defender.getNumber();
        }

        return attackerIsTrump && !defenderIsTrump;
    }
}
