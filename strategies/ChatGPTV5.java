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
import java.util.stream.Collectors;

/**
 * ProbabilisticDestroyer � ���������, ����������� ����������� ������ ����� ������ ������
 * � ���������� �����, ���������������/�������������� ����� ������ � ����������� �� �����.
 *
 * ������: ���������� ������������ ������ ������ ������ Wizard:
 *  - 4 ����� (����� 1..13)
 *  - 4 WIZARD
 *  - 4 JESTER
 *
 * ��� ������������� ������������ ��������� � ������ "DECK ASSUMPTIONS".
 */
public class ChatGPTV5 implements GameStrategy {

    // ====== ��������� ������ ������ (��������, ���� � ���� ������ ������������) ======
    private static final int SUITS_COUNT = 4;
    private static final int TOTAL_NUMBERS_PER_SUIT = 13; // ����� 1..13
    private static final int WIZARD_COUNT = 4;
    private static final int JESTER_COUNT = 4;
    private static final int TOTAL_DECK_SIZE = SUITS_COUNT * TOTAL_NUMBERS_PER_SUIT + WIZARD_COUNT + JESTER_COUNT; // 60

    // ====== ������������ ��������� ======
    private static final double BID_SAFETY_MARGIN = 0.0; // ��� ������ ������������� ������ ������ (0..1)
    // ����� ����� ������������ � ����������

    @Override
    public String getStrategyName() {
        return "ChatGPTV5";
    }

    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        Set<Card> hand = ctx.ownCards();
        Card trump = ctx.trumpCard();
        int players = ctx.gameContextDTO().botAmount();
        int round = ctx.gameContextDTO().currentRoundNumber();
        int cardsPerPlayer = round;

        double threshold = determineThreshold(round);
        Map<Card, Double> winProb = new HashMap<>();
        for (Card c : hand) {
            double p = estimateWinProbabilityAsLeader(c, trump, hand, players, cardsPerPlayer);
            winProb.put(c, p);
        }

        long goodCards = winProb.values().stream().filter(p -> p >= threshold).count();

        // WIZARD ����������� ������
        long wizards = hand.stream().filter(c -> c.getType() == CardType.WIZARD).count();

        int baseBid = (int) (goodCards + wizards);

        // �� �������������
        return Math.min(hand.size(), baseBid);
    }

    private double determineThreshold(int round) {
        // � ������ ������� ����� ���������
        if (round <= 3) return 0.45;
        if (round <= 6) return 0.5;
        if (round <= 10) return 0.6;
        return 0.7;
    }

    @Override
    public Card dropCard(DropCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();
        Set<Card> hand = new HashSet<>(ctx.ownCards());
        Card trump = round.trumpCard();
        int players = ctx.gameContextDTO().botAmount();
        int roundNumber = ctx.gameContextDTO().currentRoundNumber();
        int myWins = round.fullTrickWins().getOrDefault(ownId, 0);
        int myBids = round.fullTrickBids().getOrDefault(ownId, 0);
        int cardsPerPlayer = roundNumber;

        // ���� ��� ������ � ������ ����� � ����������� ������������ �����
        if (myWins >= myBids) {
            return pickCardWithMinWinProb(hand, trump, players, cardsPerPlayer);
        } else {
            // ��� ����� ������� � ������ ����� � ������������ ������������ �����
            return pickCardWithMaxWinProbPreserveTop(hand, trump, players, cardsPerPlayer);
        }
    }

    @Override
    public Card beatCard(BeatCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();
        TrickContextDTO trick = ctx.trickContextDTO();
        Set<CardType> allowedTypes = ctx.allowedResponses();
        Set<Card> handAll = ctx.ownCards();
        Card trump = round.trumpCard();
        Card firstDropped = trick.firstDroppedCard();
        CardType trumpType = trump != null ? trump.getType() : null;

        // Determine allowed cards to play
        Set<Card> allowed = CardManager.determineAllowedCards(allowedTypes, handAll);
        if (allowed == null || allowed.isEmpty()) {
            allowed = new HashSet<>(handAll);
        }

        int players = ctx.gameContextDTO().botAmount(); // ���� DTO �� ��������, fallback below

        int cardsPerPlayer = round.number();

        int myWins = round.fullTrickWins().getOrDefault(ownId, 0);
        int myBids = round.fullTrickBids().getOrDefault(ownId, 0);

        Card leading = CardManager.defineLeadingCard(new HashSet<>(trick.responses()), firstDropped, trumpType);

        // ���� ��� ������ � �������� �� ����: ����� non-beating card (����� ���� ������ �����)
        if (myWins >= myBids) {
            List<Card> nonBeating = allowed.stream()
                    .filter(c -> !canBeat(c, leading, trump))
                    .collect(Collectors.toList());
            if (!nonBeating.isEmpty()) {
                // ����� non-beating ����� ����� �������� �������? �� ������� � ������ ����� ������
                Set<Card> finalAllowed1 = allowed;
                return nonBeating.stream()
                        .min(Comparator.comparingInt(c -> cardStrength(c, trump)))
                        .orElseGet(() -> safeGetWeakest(finalAllowed1, trump));
            } else {
                // ���� ��� ���� � ����� ����� � ����������� ������������ �������� (����� ������� �����)
                return pickCardWithMinWinProb(allowed, trump, players, cardsPerPlayer);
            }
        }

        // ���� ����� ������� � ���� ����������� ��������� ������, ������� �������������� ������ leading (�� allowed)
        List<Card> beating = CardManager.determineCardsBeatCard(allowed, leading, trumpType);
        if (beating != null && !beating.isEmpty()) {
            // ���������� ������� ����� beating
            Set<Card> finalAllowed = allowed;
            return beating.stream()
                    .min(Comparator.comparingInt(c -> cardStrength(c, trump)))
                    .orElseGet(() -> safeGetStrongest(finalAllowed, trump));
        }

        // ����� �� ����� � ������ �������� ������� �����
        return pickCardWithMinWinProb(allowed, trump, players, cardsPerPlayer);
    }

    // ----------------- ��������������� ������ -----------------

    private Card pickCardWithMinWinProb(Set<Card> cards, Card trump, int players, int cardsPerPlayer) {
        Map<Card, Double> map = new HashMap<>();
        for (Card c : cards) {
            map.put(c, estimateWinProbabilityAsLeader(c, trump, cards, players, cardsPerPlayer));
        }
        return map.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseGet(() -> safeGetWeakest(cards, trump));
    }

    private Card pickCardWithMaxWinProbPreserveTop(Set<Card> cards, Card trump, int players, int cardsPerPlayer) {
        // �������� ����� � ������������ ������������, �� ��������� ��������� WIZARD'� (���� ����)
        List<Card> ordered = cards.stream()
                .sorted(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .collect(Collectors.toList());
        // ���� ���� wizard, �� ������ ��� ����� � �� ���� �� ��� ����� ������� ����, ����� ������� ������
        Optional<Card> wizard = ordered.stream().filter(c -> c.getType() == CardType.WIZARD).findFirst();
        Map<Card, Double> map = new HashMap<>();
        for (Card c : cards) map.put(c, estimateWinProbabilityAsLeader(c, trump, cards, players, cardsPerPlayer));

        // ������� ������������, �� ���� ������������ � WIZARD, �������� ����� ������, ����� ���������
        Card best = map.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        if (best != null && best.getType() == CardType.WIZARD) {
            // ���� ���� ������ �� �����������, ���� � (����� �� ������� WIZARD), ����� ���� WIZARD
            Card second = map.entrySet().stream()
                    .filter(e -> !e.getKey().equals(best))
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(best);
            return second;
        }
        return best != null ? best : safeGetStrongest(cards, trump);
    }

    /**
     * ������ ����������� �������� ���� ���� �������� ������ � ���� ������ (������� ����������� ������).
     * ���������, ��� ��������� �������� ��������� ����� �� ���������� ������.
     */
    private double estimateWinProbabilityAsLeader(Card c, Card trumpCard, Set<Card> ownCards, int players, int cardsPerPlayer) {
        // ������� ������
        if (c.getType() == CardType.WIZARD) return 1.0;
        if (c.getType() == CardType.JESTER) {
            // Jester ��������� ������ ���� ��� ��������� ����� Jester � ������ ������
            double probAllJesters = Math.pow((double) JESTER_COUNT / (TOTAL_DECK_SIZE - ownCards.size()), (players - 1) * cardsPerPlayer);
            return probAllJesters;
        }

        // ���������� ���������� ���� � ������ ������, ������� ����� ������ c, ��� �������, ��� ��� � ����������:
        int beaterCount = 0;

        // 1) ��� Wizards (��� ��������� ����� �����)
        beaterCount += WIZARD_COUNT;

        // 2) ���� c � �� ������: ��� ���� ������ c (����� ����)
        CardType trumpType = trumpCard != null ? trumpCard.getType() : null;
        if (trumpType != null && c.getType() != trumpType) {
            // ���������� ��� � ������ ������: SUITS_COUNT * ? �� �������������: count of trump numbers = TOTAL_NUMBERS_PER_SUIT
            // ��� ���� (����� Wizards/Jesters) � TOTAL_NUMBERS_PER_SUIT
            beaterCount += TOTAL_NUMBERS_PER_SUIT;
        } else if (trumpType != null && c.getType() == trumpType) {
            // c � ������: ������ ������ ���� � ������� �������
            int higher = Math.max(0, TOTAL_NUMBERS_PER_SUIT - c.getNumber());
            beaterCount += higher;
        } else {
            // ��� ������ � ������ (������������) � ����� ������ ������ same-suit higher
            int higher = Math.max(0, TOTAL_NUMBERS_PER_SUIT - c.getNumber());
            beaterCount += higher;
        }

        // 3) Cards of the same suit with higher number (���� c �� ������, ��� ������ ���� ��������)
        if (c.getType() != CardType.WIZARD && c.getType() != CardType.JESTER && (trumpType == null || c.getType() != trumpType)) {
            int higherSame = Math.max(0, TOTAL_NUMBERS_PER_SUIT - c.getNumber());
            beaterCount += higherSame;
        }

        // ����� �� beaterCount ��������������� ���� ��� ����, ������� � ��� � ���� (�������������)
        // (����� ��������������: �������� count of same cards in own hand)
        // ��� �������� �� ������ ������ ���������.

        int remainingDeck = TOTAL_DECK_SIZE - ownCards.size();
        if (remainingDeck <= 0) return 0.0;

        int opponentCardsCount = (players - 1) * cardsPerPlayer;
        // �����������, ��� �� � ���� �� ���������� ��� �� ����� �����, �� ��� ��� ���� c:
        double pNoBeaterInOpponents;
        double beaterFraction = Math.min(1.0, (double) beaterCount / Math.max(1, remainingDeck));
        pNoBeaterInOpponents = Math.pow(1.0 - beaterFraction, Math.max(0, opponentCardsCount));

        // ���������: ���� beaterCount==0, pNoBeaterInOpponents == 1
        return pNoBeaterInOpponents;
    }

    private int cardStrength(Card c, Card trump) {
        if (c.getType() == CardType.WIZARD) return 1000;
        if (c.getType() == CardType.JESTER) return -1;
        if (trump != null && c.getType() == trump.getType()) return 200 + c.getNumber();
        return 10 + c.getNumber();
    }

    private Card safeGetWeakest(Set<Card> cards, Card trump) {
        if (cards == null || cards.isEmpty()) throw new IllegalStateException("No cards to choose from");
        return cards.stream()
                .min(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .orElse(cards.iterator().next());
    }

    private Card safeGetStrongest(Set<Card> cards, Card trump) {
        if (cards == null || cards.isEmpty()) throw new IllegalStateException("No cards to choose from");
        return cards.stream()
                .max(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .orElse(cards.iterator().next());
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

