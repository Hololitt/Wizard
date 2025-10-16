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
 * PrecisionCounter � ���������, ��������������� �� ���������� ���������� ������ � ����������� overtricks.
 * ����: ���������� ������� ������� AnalystV1 (����������� "taking" ����), ��:
 *  - ��������� ������ �� 1 ��� ������� ����� overtrick,
 *  - ���������� ������ ���� �����,
 *  - ��� ��� ����������� ������ �������� �����, ������� ������ Analyst'� ��������� taking-cards.
 */
public class CounterAnalystV2 implements GameStrategy {

    private final Set<Card> predictedTakingCards = new HashSet<>();


    @Override
    public String getStrategyName() {
        return "CounterAnalystV2";
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        // ���������� candidate taking cards �� ��������, ������� � AnalystV1,
        // �� ��������� �������� ������ �� 1 (���� >0) ����� �������� overtricks.
        Set<Card> cards = ctx.ownCards();
        Card trump = ctx.trumpCard();
        int round = ctx.gameContextDTO().currentRoundNumber();

        Set<Card> taking = new HashSet<>();
        for (Card c : cards) {
            if (isForTakingTrick(c, trump, round)) {
                taking.add(c);
            }
        }

        // �������� ������� ��� use � ������ ����
        predictedTakingCards.clear();
        predictedTakingCards.addAll(taking);

        int predicted = taking.size();
        // �������������� �������� �����: ���� predicted > 0, ��������� �� 1 (�� �� ������ 0)
        int bid = Math.max(0, predicted - 1);

        // ��� ��������� ���������: ���� ���� ���� ������� (����� WIZARD), ������� �������� ����������
        long wizards = cards.stream().filter(c -> c.getType() == CardType.WIZARD).count();
        if (wizards > 0) {
            bid = Math.max(bid, (int) wizards);
        }

        return bid;
    }

    @Override
    public Card dropCard(DropCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();
        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bids = round.fullTrickBids().getOrDefault(ownId, 0);

        Set<Card> hand = new HashSet<>(ctx.ownCards());
        CardType trumpType = round.trumpCard() != null ? round.trumpCard().getType() : null;

        // ���� ��� �������� ������ � ���������� "�������" ��� Analyst ����� (�������/����),
        // ����� �� �� ���� ����������� ���� taking-cards.
        if (wins >= bids) {
            // 1) ���� ���� ����/������� ����� allowed (� ��� ��� allowed �� drop), ���������� ����� ������� ����
            Optional<Card> strongestTrump = hand.stream()
                    .filter(c -> c.getType() == trumpType && c.getType() != CardType.WIZARD)
                    .max(Comparator.comparingInt(c -> c.getNumber()));
            if (strongestTrump.isPresent()) return strongestTrump.get();

            // 2) ����� ������ ����� ������� (������� �����)
            return safeGetStrongest(hand, round.trumpCard());
        }

        // ���� ��� �� ������ � �������� ������ ���������, �������� ������� �������:
        // �������� ������� �����, �� �� ����� ��� (����� ��������� WIZARD/��� ���� ��� �������� ��������).
        return pickAggressiveButConservative(hand, round.trumpCard());
    }

    @Override
    public Card beatCard(BeatCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();
        TrickContextDTO trick = ctx.trickContextDTO();

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bids = round.fullTrickBids().getOrDefault(ownId, 0);

        Card firstDropped = trick.firstDroppedCard();
        CardType trumpType = round.trumpCard() != null ? round.trumpCard().getType() : null;

        // ���������� ���������� ����� (�� �������� ������� ���������)
        Set<CardType> allowedTypes = ctx.allowedResponses();
        Set<Card> allowed = CardManager.determineAllowedCards(allowedTypes, ctx.ownCards());
        if (allowed == null || allowed.isEmpty()) {
            allowed = new HashSet<>(ctx.ownCards());
        }

        // ���������� ������� ����� ���������
        Card leading = CardManager.defineLeadingCard(new HashSet<>(trick.responses()), firstDropped, trumpType);

        // ���� ��� ������ ������ � �� ���� (���� ���� ��-������ �����), ����� ���������� "�������" �����
        if (wins >= bids) {
            // ��������� ����� �����, ������� �� ���� ������� (����� �� ���� ������ ����)
            List<Card> nonBeating = allowed.stream()
                    .filter(c -> !canBeat(c, leading, round.trumpCard()))
                    .toList();
            if (!nonBeating.isEmpty()) {
                // ����� �������� ������� (����� ������ �� non-beating)
                Set<Card> finalAllowed = allowed;
                return nonBeating.stream()
                        .min(Comparator.comparingInt(c -> cardStrength(c, round.trumpCard())))
                        .orElseGet(() -> safeGetWeakest(finalAllowed, round.trumpCard()));
            } else {
                // ���� ��� non-beating (��� ����) � ����� weakest ����� ���������� ����������
                return safeGetWeakest(allowed, round.trumpCard());
            }
        }

        // ��� �� ������ � ����� ���������� �����, �� ����������. ����������� ��������� beating �����:
        List<Card> beating = CardManager.determineCardsBeatCard(allowed, leading, trumpType);
        if (beating != null && !beating.isEmpty()) {
            // ������� ����������� �� ���� �����, ������� ������ (����� ������� ����������)
            Card minimal = beating.stream()
                    .min(Comparator.comparingInt(c -> cardStrength(c, round.trumpCard())))
                    .orElse(null);
            if (minimal != null) {
                return minimal;
            }
        }

        // ���� �� ����� ������ � ���������� ����� ������
        return safeGetWeakest(allowed, round.trumpCard());
    }

    // ----------------- ��������������� � ���������� ������ -----------------

    private static boolean isForTakingTrick(Card card, Card trumpCard, int currentRoundNumber) {
        CardType type = card.getType();
        if (type == CardType.WIZARD) return true;
        if (trumpCard != null && type == trumpCard.getType()) {
            if (currentRoundNumber == 1) return true;
            if (card.getNumber() > 6) return true;
        }
        if (type != CardType.JESTER && card.getNumber() >= 10) return true;
        return false;
    }

    private Card safeGetWeakest(Set<Card> cards, Card trump) {
        if (cards == null || cards.isEmpty()) throw new IllegalStateException("No cards to choose weakest from");
        return cards.stream()
                .min(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .orElse(cards.iterator().next());
    }

    private Card safeGetStrongest(Set<Card> cards, Card trump) {
        if (cards == null || cards.isEmpty()) throw new IllegalStateException("No cards to choose strongest from");
        return cards.stream()
                .max(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .orElse(cards.iterator().next());
    }

    private Card pickAggressiveButConservative(Set<Card> hand, Card trump) {
        // ���������� �����: �������, �� �� ����� ������� � ��������� WIZARD/��� ����
        List<Card> ordered = hand.stream()
                .sorted(Comparator.comparingInt((Card c) -> cardStrength(c, trump)))
                .collect(Collectors.toList());
        int n = ordered.size();
        if (n == 0) throw new IllegalStateException("Empty hand");
        if (n == 1) return ordered.get(0);

        // ���� ���� WIZARD � �� ������� ���, ����� ������ ������
        if (ordered.get(n - 1).getType() == CardType.WIZARD) {
            return ordered.get(Math.max(0, n - 2));
        }
        // ������� ������ ����������, ����� ��������� ���-�������
        return ordered.get(Math.max(0, n - 2));
    }

    private int cardStrength(Card c, Card trump) {
        if (c.getType() == CardType.WIZARD) return 1000;
        if (c.getType() == CardType.JESTER) return -1;
        if (trump != null && c.getType() == trump.getType()) {
            return 200 + c.getNumber();
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

