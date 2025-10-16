package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.CardManager;
import JC.Training.src.WizardGame.CardType;
import JC.Training.src.WizardGame.DTOs.BeatCardContextDTO;
import JC.Training.src.WizardGame.DTOs.CreateTrickBidsContextDTO;
import JC.Training.src.WizardGame.DTOs.DropCardContextDTO;
import JC.Training.src.WizardGame.DTOs.TrickContextDTO;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.models.Card;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class ChatGPTV4 implements GameStrategy{
    @Override
    public String getStrategyName() {
        return "ChatGPT_V4";
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        Set<Card> cards = ctx.ownCards();
        Card trumpCard = ctx.trumpCard();
        int round = ctx.gameContextDTO().currentRoundNumber();

        int bid = 0;

        for (Card card : cards) {
            CardType type = card.getType();

            if (type == CardType.WIZARD) {
                bid++;
            } else if (type == trumpCard.getType()) {
                if (card.getNumber() >= 7 || round <= 2) {
                    bid++;
                }
            } else if (type != CardType.JESTER) {
                if (card.getNumber() >= 11) {
                    bid++;
                }
            }
        }

        return bid;
    }

    @Override
    public Card dropCard(DropCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().getOrDefault(ownId, 0);
        Set<Card> cards = ctx.ownCards();
        Card trump = round.trumpCard();

        // Если уже набрал нужное количество штихов — сбросить слабую
        if (wins >= bid) {
            return getWeakestCard(cards, trump);
        }

        // Попробовать взять штих — кинуть сильную карту
        return getStrongestCard(cards, trump);
    }

    @Override
    public Card beatCard(BeatCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();
        TrickContextDTO trick = ctx.trickContextDTO();

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().getOrDefault(ownId, 0);
        Card trump = round.trumpCard();
        Card toBeat = trick.firstDroppedCard();

        Set<Card> hand = ctx.ownCards();

        // Проверка допустимых типов карт
        Set<CardType> allowedTypes = CardManager.defineAllowedResponseCardTypes(toBeat, hand);
        Set<Card> allowedCards = CardManager.determineAllowedCards(allowedTypes, hand);

        if (allowedCards.isEmpty()) {
            // на случай непредвиденного бага
            allowedCards = hand;
        }

        if (wins >= bid) {
            // Уже набрал — сбрасываем слабую из допустимых
            return getWeakestCard(allowedCards, trump);
        }

        // Ищем допустимую карту, которая может побить
        List<Card> beating = allowedCards.stream()
                .filter(c -> canBeat(c, toBeat, trump))
                .sorted(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .toList();

        if (!beating.isEmpty()) {
            return beating.getFirst(); // Побить минимальной возможной
        }

        return getWeakestCard(allowedCards, trump); // Побить не можем — сбрасываем
    }

    // ==== Вспомогательные методы ====

    private boolean isWizard(Card c) {
        return c.getType() == CardType.WIZARD;
    }

    private boolean isJester(Card c) {
        return c.getType() == CardType.JESTER;
    }

    private boolean isTrump(Card c, Card trump) {
        return trump != null && c.getType() == trump.getType();
    }

    public int cardStrength(Card c, Card trump) {
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

    public boolean canBeat(Card attacker, Card defender, Card trump) {
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
