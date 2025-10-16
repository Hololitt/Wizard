package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.CardManager;
import JC.Training.src.WizardGame.CardType;
import JC.Training.src.WizardGame.DTOs.*;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.models.Card;

import java.util.*;
import java.util.stream.Collectors;

public class MetaCrusherV1 implements GameStrategy {

    // ������� ������/��������� �� ������� (������������ � ������ ����� ������)
    private final Map<String, List<Integer>> bidsHistory = new HashMap<>();
    private final Map<String, List<Integer>> winsHistory = new HashMap<>();

    // ��������� ������ (������ ��������� ��������)
    private final double WIZARD_WEIGHT = 1.0;
    private final double HIGH_TRUMP_WEIGHT = 0.9;
    private final double MEDIUM_TRUMP_WEIGHT = 0.6;
    private final double HIGH_NONTRUMP_WEIGHT = 0.6;
    private final double MEDIUM_NONTRUMP_WEIGHT = 0.35;
    private final double RISK_AVERSION_BASE = 0.25; // ������ � ����� ����������


    @Override
    public String getStrategyName() {
        return "MetaCrusherV1";
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        // ����� ������� � ������ ������ (���� �����)
        int round = ctx.gameContextDTO().currentRoundNumber();
        if (round == 1) {
            bidsHistory.clear();
            winsHistory.clear();
        }

        Set<Card> hand = ctx.ownCards();
        Card trump = ctx.trumpCard();
        GameContextDTO gameCtx = ctx.gameContextDTO();
        int totalRoundAmount = gameCtx.totalRoundAmount(); // �� ����������
        int botAmount = gameCtx.botAmount();

        // 1) ��������� �������� ���������� ���� ������������
        double expectedTricks = 0.0;

        for (Card c : hand) {
            CardType t = c.getType();
            if (t == CardType.WIZARD) {
                expectedTricks += WIZARD_WEIGHT;
            } else if (t == trump.getType()) {
                int n = c.getNumber();
                if (n >= 11) expectedTricks += HIGH_TRUMP_WEIGHT;
                else if (n >= 7) expectedTricks += MEDIUM_TRUMP_WEIGHT;
                else expectedTricks += 0.25;
            } else if (t == CardType.JESTER) {
                expectedTricks += 0.0;
            } else {
                int n = c.getNumber();
                if (n >= 12) expectedTricks += HIGH_NONTRUMP_WEIGHT;
                else if (n >= 9) expectedTricks += MEDIUM_NONTRUMP_WEIGHT;
                else expectedTricks += 0.1;
            }
        }

        // 2) �����, ��� ����� ��������� � ������� ������� ������ ������� ������ (������� ������� ��������������)
        double opponentsAggression = 0.0;
        int opponentsCounted = 0;
        for (Map.Entry<String, Integer> entry : ctx.gameContextDTO().botScores().entrySet()) {
            String id = entry.getKey();
            // ���������� ���� ���� �����
        }
        // ���������� current round context bids if available (faster ������)
        // NOTE: RoundContext �� ������� � CreateTrickBidsContextDTO, �� � ���� � AnalystV1 �������������� roundContext � Drop/Beat
        // ����� ������������� �� ������� ������� (���� ����)
        for (Map.Entry<String, List<Integer>> e : bidsHistory.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            opponentsAggression += avg;
            opponentsCounted++;
        }
        if (opponentsCounted > 0) opponentsAggression /= opponentsCounted; // ������� ������ ���������

        // 3) ������������ �������� ������ �� "�������� ������" �� �����:
        int knownAllBids = 0;
        for (List<Integer> v : bidsHistory.values()) {
            if (!v.isEmpty()) {
                knownAllBids += v.getLast(); // ��������� ������ ������
            }
        }
        // ���������� �����������: ���� ��������� ����� ������ ������ � �������� ������ ������, ������� ������
        double crowdPressure = knownAllBids; // ������� �������

        // 4) ����-���������: ��������� ������, ����� ����������� �������� ������
        double riskAversion = RISK_AVERSION_BASE;
        if (crowdPressure > hand.size() * 0.8) {
            riskAversion += 0.35; // ������� ������ ��������
        } else if (crowdPressure < hand.size() * 0.4) {
            riskAversion -= 0.1; // ����� ����������
        }

        // 5) ������������� ������: ��������� expectedTricks � ������ �����
        double rawBid = Math.max(0.0, expectedTricks * (1.0 - riskAversion));
        int bid = (int) Math.round(rawBid);

        // �������� fallback: �� ������ ������, ����������� ���������� ����
        bid = Math.max(0, Math.min(bid, hand.size()));

        // ��������� ���� ������ � ������� (������������� ����������� ����������, �� ���������� ��� � ������ ���� ��������)
        // ��� ��������������� �����������, ��� ���������� ������������ ���������� � game loop, �� �� ����� �����
        // ��������� ���� bidsHistory ��� ���� "ME" ����� ����� ��� ������������.
        bidsHistory.computeIfAbsent("ME", k -> new ArrayList<>()).add(bid);

        return bid;
    }

    @Override
    public Card dropCard(DropCardContextDTO ctx) {
        RoundContext round = ctx.roundContext();
        Card trump = round.trumpCard();
        String ownId = ctx.ownId();
        Set<Card> hand = ctx.ownCards();

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().getOrDefault(ownId, 0);

        // ���� ��� ����� ������� ������, ������� ������� � �����: ���������� ������ �� ���, ������� �� ����� ����
        if (wins >= bid) {
            // ���������� ����� �����, ������� � ���������� ������������ �� ������ ����
            return chooseCardToLose(hand, trump, round);
        }

        // ���� ����� ����� ����� � ������� ����������� �������� �����:
        // 1) �� ������� WIZARD ���� ���� ������������, 2) �� ������� ���-������ �� ������ ����, ���� ����� �������� ������� ������
        return chooseCardToWin(hand, trump, round);
    }

    @Override
    public Card beatCard(BeatCardContextDTO ctx) {
        TrickContextDTO trick = ctx.trickContextDTO();
        Card firstDropped = trick.firstDroppedCard();
        CardType trumpType = ctx.roundContext().trumpCard().getType();
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().getOrDefault(ownId, 0);

        Set<CardType> allowedTypes = ctx.allowedResponses();
        Set<Card> hand = ctx.ownCards();
        Set<Card> allowedCards = CardManager.determineAllowedCards(allowedTypes, hand);

        if (allowedCards.isEmpty()) {
            allowedCards = hand;
        }

        // ���� ��� �������� ������ � ���������� ������ �� ����������
        if (wins >= bid) {
            return getWeakest(allowedCards, ctx.roundContext().trumpCard());
        }

        // ����� �������� ������ ����������� ��������� ������, �������� �������� �������
        List<Card> canBeat = CardManager.determineCardsBeatCard(allowedCards, firstDropped, trumpType);

        if (!canBeat.isEmpty()) {
            // �������� ��, ������� ������, �� � ����������� ������������
            canBeat.sort(Comparator.comparingInt(c -> cardStrength(c, ctx.roundContext().trumpCard())));
            // ���� ����� ������ �������� ����� � WIZARD ��� ������� ������, ��������� ����� ��������� ����������-�������
            Card pick = canBeat.get(0);
            if (isWizard(pick) && canBeat.size() > 1) {
                // ������������ �� ������� WIZARD, ���� �����
                return canBeat.get(1);
            }
            return pick;
        }

        // �� ����� ������ � ���������� ����� ����������� �����
        return chooseCardToLose(allowedCards, ctx.roundContext().trumpCard(), round);
    }

    // ===== ��������������� ������ =====

    private Card chooseCardToLose(Set<Card> cards, Card trump, RoundContext round) {
        // 1) ������������ JESTER
        List<Card> jesters = cards.stream().filter(c -> c.getType() == CardType.JESTER).toList();
        if (!jesters.isEmpty()) return jesters.getFirst();

        // 2) ������������ ������ ����� ��-������ � ��-���� (����� �� �������� ���������� ������)
        List<Card> nonTrumpNonWizard = cards.stream()
                .filter(c -> c.getType() != trump.getType() && c.getType() != CardType.WIZARD)
                .sorted(Comparator.comparingInt(Card::getNumber))
                .toList();
        if (!nonTrumpNonWizard.isEmpty()) return nonTrumpNonWizard.getFirst();

        // 3) ����� � ����� ������ �� ����� �������
        return getWeakest(cards, trump);
    }

    private Card chooseCardToWin(Set<Card> cards, Card trump, RoundContext round) {
        // �� ������ WIZARD �� ������ ������, ���� �����
        List<Card> wizard = cards.stream().filter(c -> c.getType() == CardType.WIZARD).toList();
        List<Card> trumpCards = CardManager.getCardsOfType(cards, trump.getType()).stream().sorted(Comparator.comparingInt(Card::getNumber)).collect(Collectors.toList());

        // ���� �������� �������� �����: ������� ��-������, ��� ������� ������, �� �� ����� �������
        List<Card> candidate = cards.stream()
                .filter(c -> c.getType() != CardType.WIZARD && c.getType() != CardType.JESTER)
                .sorted(Comparator.comparingInt(Card::getNumber).reversed())
                .toList();

        // ���� ���� ������� ������ (�� ����� �������) � ��������� �� ����� ������ WIZARD
        if (!trumpCards.isEmpty()) {
            // ������ ������� ������ (�� ����� ��������� � �� ����� �������)
            if (trumpCards.size() >= 3) {
                return trumpCards.get(trumpCards.size() / 2);
            } else {
                // ���� ���� ���� �� ���� ������, �� ��� ������� � ������ ����� ��������� ������, ������� �� ��� �������
                return trumpCards.getFirst();
            }
        }

        // ���� ��� ������� � ������ ����� ������ �� ����������, ������� ������ ����� ��������
        if (!candidate.isEmpty()) {
            return candidate.getFirst();
        }

        // ���� ������ ��������� � ������ WIZARD (������� ����)
        if (!wizard.isEmpty()) return wizard.getFirst();

        // �������
        return getStrongest(cards, trump);
    }

    private Card getWeakest(Set<Card> cards, Card trump) {
        return cards.stream()
                .min(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .orElseThrow();
    }

    private Card getStrongest(Set<Card> cards, Card trump) {
        return cards.stream()
                .max(Comparator.comparingInt(c -> cardStrength(c, trump)))
                .orElseThrow();
    }

    private int cardStrength(Card c, Card trump) {
        if (isWizard(c)) return 1000;
        if (isJester(c)) return -1;
        if (isTrump(c, trump)) return 200 + c.getNumber();
        return 100 + c.getNumber();
    }

    private boolean isWizard(Card c) {
        return c.getType() == CardType.WIZARD;
    }

    private boolean isJester(Card c) {
        return c.getType() == CardType.JESTER;
    }

    private boolean isTrump(Card c, Card trump) {
        return trump != null && c.getType() == trump.getType();
    }
}
