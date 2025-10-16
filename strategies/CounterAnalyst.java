package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.CardManager;
import JC.Training.src.WizardGame.CardType;
import JC.Training.src.WizardGame.DTOs.BeatCardContextDTO;
import JC.Training.src.WizardGame.DTOs.CreateTrickBidsContextDTO;
import JC.Training.src.WizardGame.DTOs.DropCardContextDTO;
import JC.Training.src.WizardGame.DTOs.TrickContextDTO;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.models.Card;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CounterAnalyst implements GameStrategy {

    // ������������� ��������� � ������ �������� ��� ������ ����������
    private static final double EXTRA_BID_PROBABILITY = 0.18;   // � ����� ������������ ��������� +1 � ������
    private static final double RANDOM_YIELD_PROBABILITY = 0.30; // ��� already fulfilled: ����������� �������� (���� �����)
    private static final double OVERRIDE_BEAT_HIGHER_PROB = 0.18; // ��� ������� �����: ����������� �������� ����, ����� "�������������"
    private static final Random RNG = ThreadLocalRandom.current();


    @Override
    public String getStrategyName() {
        return "CounterAnalyst";
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        Set<Card> cards = ctx.ownCards();
        Card trump = ctx.trumpCard();
        int round = ctx.gameContextDTO().currentRoundNumber();

        int bid = 0;
        for (Card c : cards) {
            CardType t = c.getType();
            if (t == CardType.WIZARD) {
                bid++;
            } else if (trump != null && t == trump.getType()) {
                // ���� ����� ����������� ���������� ��� �������
                if (c.getNumber() >= 8 || round <= 2) bid++;
            } else if (t != CardType.JESTER) {
                // ��� ������� ��-������� ���������� ������
                if (c.getNumber() >= 12) bid++;
            }
        }

        // ��������� ��������: ������ ��������� +1, ����� "��������" Analyst (�� ������� �������� taking cards)
        if (bid > 0 && RNG.nextDouble() < EXTRA_BID_PROBABILITY) {
            bid = Math.min(bid + 1, cards.size());
        }

        return bid;
    }

    @Override
    public Card dropCard(DropCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();
        Set<Card> hand = ctx.ownCards();
        Card trump = round.trumpCard();

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().getOrDefault(ownId, 0);

        // ���� ��� ������ ������ � ����� ���������� �����, ������� Analyst ������ ����� ����� ���������.
        // ��� ������ Analyst-� ���������� �������� ��� ������ takingTrickCards.
        if (wins >= bid) {
            return getWorstForAnalyst(hand, trump);
        }

        // ���� ��� �� ������ � ������ ����������, �� ������ ��������� � ����� ������� ����� (������)
        if (RNG.nextDouble() < 0.12) {
            // ������ ������ � ������� �� ����� �������, ����� ��������� ����
            return getConservativeAggressive(hand, trump);
        }
        return getStrongestCardSafe(hand, trump);
    }

    @Override
    public Card beatCard(BeatCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();
        TrickContextDTO trick = ctx.trickContextDTO();

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().getOrDefault(ownId, 0);

        Card firstDropped = trick.firstDroppedCard();
        CardType trumpType = round.trumpCard() != null ? round.trumpCard().getType() : null;

        // ���������� ������ �� ����� (���������)
        Set<CardType> allowedTypes = ctx.allowedResponses();
        Set<Card> allowedCards = CardManager.determineAllowedCards(allowedTypes, ctx.ownCards());
        if (allowedCards == null || allowedCards.isEmpty()) {
            allowedCards = new HashSet<>(ctx.ownCards());
        }

        Card leadingCard = CardManager.defineLeadingCard(new HashSet<>(trick.responses()), firstDropped, trumpType);

        // ���� ��� ������ ������ � � �������� ���������� ������, �� ������ "��������" ���������
        if (wins >= bid) {
            if (RNG.nextDouble() < RANDOM_YIELD_PROBABILITY) {
                // ��������� �� ������ � ��� ���������� ����������� ����� (��������� Analyst ������ �����)
                return CardManager.getWeakestCard(allowedCards, trumpType);
            } else {
                // � ��������� � ���������� �����, ������� Analyst ������� ������ (�������)
                return getWorstForAnalyst(allowedCards, round.trumpCard());
            }
        }

        // ��� �� ������� � ������� ������ ����������. �� � ��������� ������������ "����������" ����,
        // ����� �������� ���������� ������� ������� �����.
        List<Card> beating = CardManager.determineCardsBeatCard(allowedCards, leadingCard, trumpType);
        if (beating != null && !beating.isEmpty()) {
            // ��������� �� ���� (������ -> �������)
            List<Card> sorted = beating.stream()
                    .sorted(Comparator.comparingInt(c -> cardStrength(c, round.trumpCard())))
                    .collect(Collectors.toList());

            if (RNG.nextDouble() < OVERRIDE_BEAT_HIGHER_PROB && sorted.size() > 1) {
                // ����� ���� ����� ������� �����, ����� "�������������" ����������
                return sorted.get(sorted.size() - 1);
            } else {
                // ���������� ������ �����
                return sorted.get(0);
            }
        }

        // �� ����� ������ � ���������� ������ (������� �������)
        return CardManager.getWeakestCard(allowedCards, trumpType);
    }

    // ----------------- ��������������� ������ -----------------

    private Card getWorstForAnalyst(Set<Card> cards, Card trump) {
        // "���� ��� Analyst" � ��� � ����� ��������� ����� ������� ����� (������� �� ����� �� ���������).
        // ��������� �� ���� � ���������� ����� �������.
        return getStrongestCardSafe(cards, trump);
    }

    private Card getConservativeAggressive(Set<Card> cards, Card trump) {
        // ���������� �������, �� �� �����-����� (��������, ������ ������), ����� ��������� ������� �������
        List<Card> ordered = cards.stream()
                .sorted(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .collect(Collectors.toList());
        if (ordered.isEmpty()) throw new IllegalStateException("No cards to play");
        if (ordered.size() == 1) return ordered.get(0);
        return ordered.get(Math.max(0, ordered.size() - 2));
    }

    private Card getStrongestCardSafe(Set<Card> cards, Card trump) {
        return cards.stream()
                .max(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .orElseGet(() -> cards.iterator().next());
    }

    private int cardStrength(Card c, Card trumpCard) {
        if (c.getType() == CardType.WIZARD) return 1000;
        if (c.getType() == CardType.JESTER) return -1;
        if (trumpCard != null && c.getType() == trumpCard.getType()) {
            return 200 + c.getNumber(); // ���� ���� �������
        }
        return 10 + c.getNumber();
    }

    private boolean canBeat(Card attacker, Card defender, Card trumpCard) {
        if (attacker.getType() == CardType.WIZARD) return true;
        if (attacker.getType() == CardType.JESTER) return false;
        if (defender.getType() == CardType.WIZARD) return false;
        if (defender.getType() == CardType.JESTER) return true;

        boolean attackerIsTrump = (trumpCard != null && attacker.getType() == trumpCard.getType());
        boolean defenderIsTrump = (trumpCard != null && defender.getType() == trumpCard.getType());

        if (attacker.getType() == defender.getType()) {
            return attacker.getNumber() > defender.getNumber();
        }
        return attackerIsTrump && !defenderIsTrump;
    }
}

