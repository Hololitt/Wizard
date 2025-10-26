package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.DTOs.*;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.handlers.CardManager;
import JC.Training.src.WizardGame.models.Card;

import java.util.*;
import java.util.stream.Collectors;

public class CounterAnalystV3 implements GameStrategy {

    // ������� ��������� N ������� �� ������� (������ � ������) � ������������ ��� �������� ���������
    private final int HISTORY_SIZE = 8;
    private final Map<String, Deque<Integer>> recentBids = new HashMap<>();
    private final Map<String, Deque<Integer>> recentWins = new HashMap<>();

    // ��������� ��������� (����� ����� �����������)
    private final double WIZARD_SCORE = 1000.0;
    private final double JESTER_SCORE = -3;
    private final double TRUMP_BASE = 400;
    private final double NONTRUMP_BASE = 110;

    private final Random rnd = new Random();

    @Override
    public String getStrategyName() {
        return "CounterAnalystV3";
    }

    // -------------------- CREATE BIDS --------------------
    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        // ���� ����� ����� 1 � ���������� ������� ����� ������
        int roundNumber = ctx.gameContextDTO().currentRoundNumber();
        if (roundNumber == 1) {
            recentBids.clear();
            recentWins.clear();
        }

        Set<Card> hand = ctx.ownCards();
        Card trump = ctx.trumpCard();
        int handSize = hand.size();

        // 1) ������ ���� ����: ��������� "���������" ����������� ������ ������
        double scoreSum = 0.0;
        for (Card c : hand) {
            scoreSum += heuristicCardScore(c, trump);
        }

        // ���������: ��� ������ ������������� ����� � ��� ���� raw expectation
        // ���������: ������� ���� ����� ~ 100..300 => expectedTricks ~ scoreSum / (handSize * 200)
        double avgCardScore = (handSize > 0) ? scoreSum / handSize : 0.0;
        double expectedTricks = (scoreSum) / 200.0; // ������ �����; ����� ������

        // 2) �������� "��������" ����������: ������� �� ��������� ������ (���� ����)
        double opponentsAvgLastBid = 0.0;
        int opponentsCount = 0;
        for (Map.Entry<String, Deque<Integer>> e : recentBids.entrySet()) {
            String id = e.getKey();
            if (id.equals(ctx.ownID())) continue;
            Deque<Integer> dq = e.getValue();
            if (!dq.isEmpty()) {
                opponentsAvgLastBid += dq.getLast();
                opponentsCount++;
            }
        }
        if (opponentsCount > 0) opponentsAvgLastBid /= opponentsCount;

        // 3) �������� Analyst-like ����������: ���� � ����-�� ����� bid == wins ��� ����� ��������� �
        // ������ ������� � ������������� ���� ���������
        boolean analystPresent = detectAnalystPattern(ctx.ownID());

        // 4) �������� ����� � ��������� ���� ���� ��������� ����� ������ ������ � �������� roundNumber
        // ���������� ��������� known bids (���������) ��� proxy
        double riskFactor = getRiskFactor(handSize);

        // 5) Exploit: ���� ����� Analyst-like ��������� � ������� ������� ������ � �� �������,
        // ������� �������� ��� ���� ���������: �� ������� ������ ������ (round 6..(total-3)) � ���������� ���� �����������,
        // �� ����� ������� � ����� ��������� (Analyst ����� �� ���������)
        double exploitBias = 0.0;
        if (analystPresent) {
            int totalRounds = ctx.gameContextDTO().totalRoundAmount();
            if (roundNumber >= Math.max(1, totalRounds / 4) && roundNumber <= Math.max(1, totalRounds - totalRounds / 6)) {
                // �������� ������ � ��������
                exploitBias = -5;
            } else {
                // ������/����� � ������ �����
                exploitBias = -0.1;
            }
        }

        // Compose final raw bid (non-negative)
        double rawBid = Math.max(0.0, expectedTricks * riskFactor + exploitBias);

        // ���������� ����� � ���������� ���������� ���� � ��������� ������ +1, ����� �������� �����������������
        int bid = (int) Math.floor(rawBid);
        if (rnd.nextDouble() < 0.08) bid++; // ��������� �������������� �������

        // ������: �� ������ ������, ��� ����
        bid = Math.max(0, Math.min(bid, handSize));

        // ��������� ���� ������ � �������
        recentBids.computeIfAbsent(ctx.ownID(), _ -> new ArrayDeque<>()).addLast(bid);
        trimDeque(recentBids.get(ctx.ownID()));

        return bid;
    }

    private double getRiskFactor(int handSize) {
        int knownAllBids = 0;
        int knownCount = 0;
        for (Map.Entry<String, Deque<Integer>> e : recentBids.entrySet()) {
            Deque<Integer> dq = e.getValue();
            if (!dq.isEmpty()) {
                knownAllBids += dq.getLast();
                knownCount++;
            }
        }

        double crowdPressure = (knownCount > 0) ? ((double) knownAllBids / (double) knownCount) : 0.0;

        double riskFactor = 1.0;
        // ����� ���������, ���� crowdPressure ������ relative to handSize
        if (crowdPressure > handSize * 0.8) {
            riskFactor = 0.65;
        } else if (crowdPressure < handSize * 0.4) {
            riskFactor = 1.05;
        } else {
            riskFactor = 0.9;
        }
        return riskFactor;
    }

    // -------------------- DROP CARD (lead) --------------------
    @Override
    public Card dropCard(DropCardContextDTO ctx) {
        RoundContext round = ctx.roundContext();
        String ownId = ctx.ownId();
        Set<Card> hand = ctx.ownCards();
        Card trump = round.trumpCard();

        // ��������� ������� (�������� ��������� bids/wins �� ���������)
        updateHistoriesFromRoundContext(round);

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().get(ownId).trickBid();
        int roundNumber = round.number();
        int totalRounds = ctx.gameContextDTO().totalRoundAmount();

        boolean analystPresent = detectAnalystPattern(ctx.ownId());

        // ���� ��� ������� ������� ������, ������� ������� � ������� �������� ������� �����
        if (wins >= bid) {
            return chooseCardToLose(hand, trump, roundNumber, totalRounds, analystPresent);
        }

        // ���� �����: �������� ����������-������� �����, �� � ������ ���������� �������� ��������
        return chooseCardToWin(hand, trump, roundNumber, totalRounds, analystPresent);
    }

    // -------------------- BEAT CARD (respond) --------------------
    @Override
    public Card beatCard(BeatCardContextDTO ctx) {
        RoundContext round = ctx.roundContext();
        TrickContextDTO trick = ctx.trickContextDTO();
        String ownId = ctx.ownId();
        Card first = trick.firstDroppedCard();
        Card trump = round.trumpCard();

        // ��������� �������
        updateHistoriesFromRoundContext(round);

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().get(ownId).trickBid();

        Set<CardType> allowedTypes = ctx.allowedResponses();
        Set<Card> hand = ctx.ownCards();
        Set<Card> allowed = determineAllowedCards(allowedTypes, hand);
        if (allowed.isEmpty()) allowed = new HashSet<>(hand);

        boolean analystPresent = detectAnalystPattern(ctx.ownId());

        // ���� ��� ��������� ������ � ���������� ����������� ���������� �����
        if (wins >= bid) {
            return chooseCardToLose(allowed, trump, round.number(), ctx.gameContextDTO().totalRoundAmount(), analystPresent);
        }

        // ������� ������ ���������� ��������� ������
        List<Card> canBeat = determineCardsBeatCard(allowed, first, trump.getType());
        if (!canBeat.isEmpty()) {
            // ��������� �� "���������" (������ �������)
            CardManager.sortCardsFromMinToMax(new HashSet<>(canBeat));

            // prefer not to spend WIZARD if we can avoid
            Card pick = canBeat.get(0);
            if (isWizard(pick) && canBeat.size() > 1) {
                // ���� ���� ������ ������� � ���������� ���
                pick = canBeat.get(1);
            }

            // ���� Analyst ������� � �� � �������� ������ � ����� ����������� ������� ������, ����� ������� �����
            if (analystPresent && isTrump(pick, trump) && rnd.nextDouble() < 0.12) {
                // ������� ������� ������� ������ ������ ������ ������� ���������
                Optional<Card> midTrump = canBeat.stream().filter(c -> isTrump(c, trump) && c.getNumber() >= 7 && c.getNumber() <= 11).findFirst();
                if (midTrump.isPresent()) pick = midTrump.get();
            }

            return pick;
        }

        // �� ����� ������ � ����� ������ "���������" (jester -> low non-trump)
        return chooseCardToLose(allowed, trump, round.number(), ctx.gameContextDTO().totalRoundAmount(), analystPresent);
    }

    // -------------------- Helpers --------------------

    // ������� ������� �� HISTORY_SIZE
    private void trimDeque(Deque<Integer> dq) {
        while (dq.size() > HISTORY_SIZE) dq.removeFirst();
    }

    // ���������� recentBids/recentWins �� RoundContext (���������� ��� drop/beat, �.�. ��� ������������ fullTrickBids/wins)
    private void updateHistoriesFromRoundContext(RoundContext round) {
        Map<String, TrickBidDTO> bids = round.fullTrickBids();
        Map<String, Integer> wins = round.fullTrickWins();

        for (Map.Entry<String, TrickBidDTO> e : bids.entrySet()) {
            recentBids.computeIfAbsent(e.getKey(), k -> new ArrayDeque<>()).addLast(e.getValue().trickBid());
            trimDeque(recentBids.get(e.getKey()));
        }
        for (Map.Entry<String, Integer> e : wins.entrySet()) {
            recentWins.computeIfAbsent(e.getKey(), k -> new ArrayDeque<>()).addLast(e.getValue());
            trimDeque(recentWins.get(e.getKey()));
        }
    }

    // ����� ������� �������� Analyst-��������: ����� bid == wins � ����� ��������� ������
    private boolean detectAnalystPattern(String ownId) {
        for (String id : recentBids.keySet()) {
            if (id.equals(ownId)) continue;
            Deque<Integer> bids = recentBids.get(id);
            Deque<Integer> wins = recentWins.getOrDefault(id, new ArrayDeque<>());

            if (bids.size() >= 4 && wins.size() >= 4) {
                int matches = 0;
                int i = 0;
                Iterator<Integer> ib = bids.iterator();
                Iterator<Integer> iw = wins.iterator();
                while (ib.hasNext() && iw.hasNext()) {
                    int bv = ib.next();
                    int wv = iw.next();
                    if (bv == wv) matches++;
                    i++;
                    if (i >= 6) break;
                }
                // ���� � >60% ��������� ������� � ������ bid == wins -> candidate
                if ((double) matches / Math.max(1, i) > 0.60) return true;

                // ����� �������� ��������� ������: ���� stddev ��������� (����� ���������� ������)
                double avg = bids.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                double var = bids.stream().mapToDouble(v -> (v - avg) * (v - avg)).average().orElse(0.0);
                if (var < 1.2) return true;
            }
        }
        return false;
    }

    // ������ "����" ����� � ���������� �������, ����������� CardManager.cardStrength, �� ��� double
    private double heuristicCardScore(Card c, Card trump) {
        if (c.getType() == CardType.WIZARD) return WIZARD_SCORE;
        if (c.getType() == CardType.JESTER) return JESTER_SCORE;
        if (trump != null && c.getType() == trump.getType()) {
            // ������ �������
            return TRUMP_BASE + c.getNumber();
        }
        return NONTRUMP_BASE + c.getNumber();
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

    // ����� ����� ��� �������� (������������ ����������� ���������� ������)
    private Card chooseCardToLose(Set<Card> cards, Card trump, int roundNumber, int totalRounds, boolean analystPresent) {
        // 1) Jester � ������ ������ ��������
        List<Card> jesters = cards.stream().filter(this::isJester).toList();
        if (!jesters.isEmpty()) return jesters.getFirst();

        // 2) ������ ��-������ (������������ � �� ������� �����, ������� ��� ��������� ���� � ����)
        List<Card> nonTrump = cards.stream().filter(c -> !isTrump(c, trump) && !isWizard(c) && !isJester(c))
                .sorted(Comparator.comparingInt(Card::getNumber))
                .toList();
        if (!nonTrump.isEmpty()) return nonTrump.getFirst();

        // 3) ���� ������ ������/���� �������� � ����� ����� ������ ������ (��� ���� �� ������� ������� ����� ������� �������)
        List<Card> trumpCards = cards.stream().filter(c -> isTrump(c, trump) && !isWizard(c)).sorted(Comparator.comparingInt(Card::getNumber)).collect(Collectors.toList());
        if (!trumpCards.isEmpty()) {
            // ���� Analyst ������������ � �� � �������� ������ � �� ����� ������� ������ (��������� ��� ���������)
            if (analystPresent && roundNumber > totalRounds / 4 && roundNumber < totalRounds - 2) {
                // ����� ����� ��������� ������
                return trumpCards.getFirst();
            } else {
                return trumpCards.getFirst();
            }
        }

        // 4) ��� ������� � ����� ����� ������ �� ������ �����
        return cards.stream().min(Comparator.comparingDouble(c -> heuristicCardScore(c, trump))).orElseThrow();
    }

    // ����� ����� ��� ������� �������� � ���������� �����������, � ����������� WIZARD/top-trump
    private Card chooseCardToWin(Set<Card> cards, Card trump, int roundNumber, int totalRounds, boolean analystPresent) {
        // 1) ������� ����� ������� ���������� ������� (���� �� �������� ����� ������� ��� ���������� ������)
        List<Card> nonTrumpsSortedDesc = cards.stream()
                .filter(c -> !isTrump(c, trump) && !isWizard(c))
                .sorted(Comparator.comparingInt(Card::getNumber).reversed())
                .toList();
        if (!nonTrumpsSortedDesc.isEmpty()) {
            // ������������ �� ������� ������ �� ������, ����� ����������/������� �����, �� �� ���
            Card candidate = nonTrumpsSortedDesc.getFirst();
            if (nonTrumpsSortedDesc.size() > 1) {
                // ���� ���� ������ �� ���� � �������� �� �������, ���������� ��� (�������� ���)
                candidate = nonTrumpsSortedDesc.get(Math.min(1, nonTrumpsSortedDesc.size() - 1));
            }
            return candidate;
        }

        // 2) ������������ ������� ������ (�� ���) � ����� �� ����������� �� ������ ������
        List<Card> trumpCards = cards.stream().filter(c -> isTrump(c, trump) && !isWizard(c)).sorted(Comparator.comparingInt(Card::getNumber)).collect(Collectors.toList());
        if (!trumpCards.isEmpty()) {
            // �������� "�������" ������
            int idx = Math.max(0, trumpCards.size() / 2 - 1);
            return trumpCards.get(idx);
        }

        // 3) ���� ���� WIZARD � ������������ ������ ���� ��� ����������� � ���� ��� ����� ����� (� ����� ������)
        List<Card> wizards = cards.stream().filter(this::isWizard).toList();
        if (!wizards.isEmpty()) {
            // ���������� � ����� ������ ��� ���� � ��� ����� ������ ����������� �������� ��� ����
            if (roundNumber > totalRounds - 3 || rnd.nextDouble() < 0.06) {
                return wizards.getFirst();
            }
        }

        // 4) fallback � ����� ������� �� ����� �������
        return cards.stream().max(Comparator.comparingDouble(c -> heuristicCardScore(c, trump))).orElseThrow();
    }

    // -------------------- Utilities similar to CardManager --------------------
    private List<Card> determineCardsBeatCard(Set<Card> cards, Card cardToBeat, CardType trumpCardType) {
        List<Card> res = new ArrayList<>();
        for (Card beatCard : cards) {
            CardType bt = beatCard.getType();
            int bn = beatCard.getNumber();
            CardType ct = cardToBeat.getType();
            int cn = cardToBeat.getNumber();

            if (ct == CardType.WIZARD) {
                // ���������� ������ WIZARD
                continue;
            }

            if (bt == CardType.WIZARD) {
                res.add(beatCard);
                continue;
            }
            if (ct == CardType.JESTER) {
                if (bt != CardType.JESTER) res.add(beatCard);
                continue;
            }

            if (bt == ct && bn > cn) {
                res.add(beatCard);
                continue;
            }

            if (bt == trumpCardType && ct != trumpCardType) {
                res.add(beatCard);
            }
        }
        return res;
    }

    private Set<Card> determineAllowedCards(Set<CardType> allowedTypes, Set<Card> cards) {
        return cards.stream().filter(c -> allowedTypes.contains(c.getType())).collect(Collectors.toSet());
    }
}

