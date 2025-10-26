package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.DTOs.*;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.handlers.CardManager;
import JC.Training.src.WizardGame.models.Card;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AdaptiveMasterV1
 *
 * ������������� ���������� ���������:
 * - ������������� ������ ������ ������ ����� ����� ����;
 * - ��������� expectedTricks = sum(probabilities);
 * - ������������� �� ������� �������� (bid vs wins) ����� roundContexts;
 * - ��������� � ����� ������� (1v1 <-> group);
 * - ��������� ������������� WIZARD / ���-�������;
 * - �������������� ���������� ������ ��� �����������������.
 */
public class AdaptiveMasterV1 implements GameStrategy {

    // --- ������������� ��������� ---
    private final double WIZARD_BASE_PROB = 0.98;       // ����������� W -> ������ (� ������)
    private final double JESTER_BASE_PROB = 0.02;       // Jester ����� �� ����
    private final double TRUMP_SCALE = 0.75;            // ������� ��� �������
    private final double NONTRUMP_SCALE = 0.55;         // ������� ��� ��-�������
    private final double GROUP_PENALTY_PER_OPP = 0.12;  // ��� ������ �������, ��� ����������
    private final double SELF_LEARN_RATE = 0.45;        // ��� ������ �������������� �� �������
    private final double RANDOM_BID_EPS = 0.09;         // ���� ��������� +1 � ������ (������)

    // ���������� ������� � ������ ����� ������
    // key "ME" ������ ����������� bids; also track wins
    private final Deque<Integer> myBids = new ArrayDeque<>();
    private final Deque<Integer> myWins = new ArrayDeque<>();
    private final int HISTORY_LIMIT = 8;

    private final Random rnd = new Random();


    @Override
    public String getStrategyName() {
        return "AdaptiveMasterV1";
    }

    // ---------------- createTrickBids ----------------
    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        // ����� ������� ��� ������ ����� ������ (round 1)
        int currentRound = ctx.gameContextDTO().currentRoundNumber();
        if (currentRound == 1) {
            myBids.clear();
            myWins.clear();
        }

        Set<Card> hand = ctx.ownCards();
        Card trump = ctx.trumpCard();
        int botAmount = ctx.gameContextDTO().botAmount();
        Stack<RoundContext> roundContexts = ctx.gameContextDTO().roundContexts();

        // 1) ��������� ����������� ������ �� ������ ����� (���������)
        double expectedTricks = 0.0;
        for (Card c : hand) {
            expectedTricks += estimateCardWinProb(c, hand.size(), trump, botAmount);
        }

        // 2) ��������� �� ���� ����� (� ��������� - ����������)
        double groupPenalty = 1.0 - Math.max(0.0, (botAmount - 2) * GROUP_PENALTY_PER_OPP);
        expectedTricks *= clamp(groupPenalty, 0.4, 1.0);

        // 3) �������������: ���� � ������� ������� ���� ���������� ����/�������� -> ������� expectation
        double selfAccuracyFactor = computeSelfAccuracyFactor(roundContexts, currentRound, ctx.ownID());
        expectedTricks *= (1.0 + SELF_LEARN_RATE * (selfAccuracyFactor - 1.0)); // ���� <1 -> ��������

        // 4) ��������� ��������� ��������� ������ ������ (���� ���� � trickBids) � �� ��� ����� ���� ������ "�����������"
        int sumOtherBids = 0;
        int knownOtherCount = 0;
        for (Map.Entry<String, TrickBidDTO> e : ctx.trickBids().entrySet()) {
            String id = e.getKey();
            if (!id.equals(ctx.ownID())) {
                sumOtherBids += e.getValue().trickBid();
                knownOtherCount++;
            }
        }
        if (knownOtherCount > 0) {
            double avgOtherBid = (double) sumOtherBids / knownOtherCount;
            // ���� ������� ����� ������ ����������� ������ �������� -> ��������� ����
            if (avgOtherBid > expectedTricks + 0.8) {
                expectedTricks *= 0.85;
            }
        }

        // 5) �����������, ��� expectedTricks <= hand.size()
        expectedTricks = Math.max(0.0, Math.min(expectedTricks, hand.size()));

        // 6) �������������� ����������: floor + ����������� �������� 1 � ����������� �� ������� �����
        int bid = (int) Math.floor(expectedTricks);
        double frac = expectedTricks - bid;
        if (rnd.nextDouble() < frac || rnd.nextDouble() < RANDOM_BID_EPS) {
            bid++;
        }

        // 7) Safety clamp
        if (bid < 0) bid = 0;
        if (bid > hand.size()) bid = hand.size();

        // ��������� � ������� (myBids). ��������� HISTORY_LIMIT �������.
        myBids.addLast(bid);
        while (myBids.size() > HISTORY_LIMIT) myBids.removeFirst();

        return bid;
    }

    // ---------------- dropCard (lead) ----------------
    @Override
    public Card dropCard(DropCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();
        Set<Card> hand = ctx.ownCards();
        Card trump = round.trumpCard();

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().get(ownId).trickBid();

        int totalRounds = ctx.gameContextDTO().totalRoundAmount();
        int roundNumber = round.number();
        int botAmount = ctx.gameContextDTO().botAmount();

        // ���� ��� ������� ������ � ������� ���������
        if (wins >= bid) {
            return chooseCardToLose(hand, trump, botAmount, roundNumber, totalRounds);
        }

        // ����� �����: �������� ���������� ����������� ����� � ������ ����������� ������
        // 1) ��������� ����� ��-���������� �������� ����� (�� WIZARD) � ���������� ������������
        List<Card> candidates = new ArrayList<>(hand);
        candidates.sort(Comparator.comparingDouble(c -> estimateCardUsageCost(c, hand.size(), trump, botAmount)));
        // ������������ �� ������� WIZARD, ���� ���� ��������
        for (Card c : candidates) {
            if (!isWizard(c)) {
                // ���� ����������� ���� ����� �������� ���������� ������ � ����������
                double p = estimateCardWinProb(c, hand.size(), trump, botAmount);
                if (p >= 0.40 || (roundNumber > totalRounds - 3 && p >= 0.30)) {
                    return c;
                }
            }
        }

        // 2) ���� ��� ���������� ���������� �����, ���������� ������� ������
        List<Card> trumps = hand.stream().filter(h -> isTrump(h, trump) && !isWizard(h)).sorted(Comparator.comparingInt(Card::getNumber)).collect(Collectors.toList());
        if (!trumps.isEmpty()) {
            int idx = Math.max(0, trumps.size() / 2 - 1);
            return trumps.get(idx);
        }

        // 3) ��� ������� ���� � WIZARD ���� ������ ����� (late-game) ��� ����� �������
        List<Card> wizards = hand.stream().filter(this::isWizard).collect(Collectors.toList());
        if (!wizards.isEmpty()) {
            if (roundNumber > totalRounds - 2) return wizards.get(0);
            // ����� ��������� ������� � ������ ���������� ��������� �����, �� �� ������
        }

        // 4) fallback � ��������� ����� (�� �������)
        return hand.stream().min(Comparator.comparingDouble(c -> estimateCardUsageCost(c, hand.size(), trump, botAmount))).orElseThrow();
    }

    // ---------------- beatCard (respond) ----------------
    @Override
    public Card beatCard(BeatCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext round = ctx.roundContext();
        TrickContextDTO trick = ctx.trickContextDTO();
        Card first = trick.firstDroppedCard();
        Card trump = round.trumpCard();

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().get(ownId).trickBid();
        int botAmount = ctx.gameContextDTO().botAmount();
        int totalRounds = ctx.gameContextDTO().totalRoundAmount();

        Set<CardType> allowedTypes = ctx.allowedResponses();
        Set<Card> hand = ctx.ownCards();
        Set<Card> allowed = CardManager.determineAllowedCards(allowedTypes, hand);
        if (allowed.isEmpty()) allowed = new HashSet<>(hand);

        // ���� ��� ��������� ������ � ����������
        if (wins >= bid) {
            return chooseCardToLose(allowed, trump, botAmount, round.number(), totalRounds);
        }

        // ������� ������ ���������� ��������� ������ (������������ ���������)
        List<Card> canBeat = CardManager.determineCardsBeatCard(allowed, first, trump.getType());
        if (!canBeat.isEmpty()) {
            canBeat.sort(Comparator.comparingDouble(c -> estimateCardUsageCost(c, hand.size(), trump, botAmount)));
            // ���� ������ � WIZARD � ���� ������������ � ���� ������������
            Card pick = canBeat.get(0);
            if (isWizard(pick) && canBeat.size() > 1) pick = canBeat.get(1);
            // ��������� ����������: ������ ����� �������, ����� ������� ������
            if (rnd.nextDouble() < 0.06 && canBeat.size() > 1) {
                pick = canBeat.get(Math.min(canBeat.size() - 1, 1 + rnd.nextInt(Math.min(2, canBeat.size()))));
            }
            return pick;
        }

        // �� ����� ������ � �������
        return chooseCardToLose(allowed, trump, botAmount, round.number(), totalRounds);
    }

    // ---------------- Helpers ----------------

    private double estimateCardWinProb(Card c, int handSize, Card trump, int botAmount) {
        // ������� ���������
        if (isWizard(c)) return WIZARD_BASE_PROB;
        if (isJester(c)) return JESTER_BASE_PROB;

        double base;
        if (isTrump(c, trump)) {
            base = TRUMP_SCALE * ((double) c.getNumber() / 14.0) + 0.10; // ������ �������
        } else {
            base = NONTRUMP_SCALE * ((double) c.getNumber() / 14.0) + 0.05;
        }

        // ��������� ������ �����: ��� ������ ����������� � ��� ������ ����
        double opponentsFactor = 1.0 - (botAmount - 2) * 0.06;
        base *= clamp(opponentsFactor, 0.5, 1.0);

        // ��������� �������� �� handSize � � ��������� ����� ����� ������
        if (handSize <= 4) base += 0.06;
        if (handSize >= 12) base -= 0.04;

        return clamp(base, 0.01, 0.995);
    }

    // ��������� ������������� ����� � ��� ������, ��� "�������" �������
    private double estimateCardUsageCost(Card c, int handSize, Card trump, int botAmount) {
        if (isWizard(c)) return 10000.0; // ����� ������ � �������� �� �������
        if (isJester(c)) return -100.0;  // ����� ��������� ��������

        double cost = 0.0;
        if (isTrump(c, trump)) {
            // ���-������ ������, ������� � �������
            cost = 500.0 + (14 - c.getNumber()) * 8.0;
        } else {
            // ��-������: ������� ����� ������, ������ �������
            cost = 200.0 + (14 - c.getNumber()) * 6.0;
        }

        // ���� ����� ������� � ��������� ����� ������� ������� (����������� cost)
        cost *= (1.0 + (botAmount - 2) * 0.07);

        return cost;
    }

    private Card chooseCardToLose(Set<Card> cards, Card trump, int botAmount, int roundNumber, int totalRounds) {
        // 1) Jester
        List<Card> jesters = cards.stream().filter(this::isJester).collect(Collectors.toList());
        if (!jesters.isEmpty()) return jesters.get(0);

        // 2) ������ ��-������ (������������ ����������� ������)
        List<Card> lowNonTrump = cards.stream()
                .filter(c -> !isTrump(c, trump) && !isWizard(c) && !isJester(c))
                .sorted(Comparator.comparingInt(Card::getNumber))
                .collect(Collectors.toList());
        if (!lowNonTrump.isEmpty()) return lowNonTrump.get(0);

        // 3) ����� ������ ������
        List<Card> trumps = cards.stream().filter(c -> isTrump(c, trump) && !isWizard(c)).sorted(Comparator.comparingInt(Card::getNumber)).collect(Collectors.toList());
        if (!trumps.isEmpty()) return trumps.get(0);

        // 4) fallback weakest by usage cost
        return cards.stream().min(Comparator.comparingDouble(c -> estimateCardUsageCost(c, cards.size(), trump, botAmount))).orElseThrow();
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

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * ��������� ����������� selfAccuracyFactor:
     * - ���� � ������� ������� �� � ������� ������������� (bids > wins) -> factor < 1 (�������� ��������)
     * - ���� ������������� (bids < wins) -> factor > 1 (����� ���� ������)
     *
     * �������: 1 - 0.15 * avgError, ��� avgError = mean((bids - wins) / max(1, bids))
     */
    private double computeSelfAccuracyFactor(Stack<RoundContext> roundContexts, int currentRound, String ownId) {
        if (roundContexts == null || roundContexts.isEmpty()) return 1.0;

        int consider = Math.min(HISTORY_LIMIT, roundContexts.size());
        double sumRelError = 0.0;
        int counted = 0;

        // ��� � ����� �� ��������� completed rounds (���������� ������� �������������)
        for (int i = roundContexts.size() - 1; i >= 0 && counted < consider; i--) {
            RoundContext rc = roundContexts.get(i);
            Map<String, Integer> wins = rc.fullTrickWins();
            Map<String, TrickBidDTO> bids = rc.fullTrickBids();
            if (!wins.containsKey(ownId) || !bids.containsKey(ownId)) continue;
            int w = wins.get(ownId);
            int b = bids.get(ownId).trickBid();
            if (b == 0 && w == 0) {
                // neutral
                counted++;
                continue;
            }
            double rel = (double)(b - w) / Math.max(1, b);
            sumRelError += rel;
            counted++;
        }

        if (counted == 0) return 1.0;
        double avgRelError = sumRelError / counted; // positive -> overbid, negative -> underbid

        // map avgRelError to factor: avgRelError = 0 -> 1.0; if overbid 0.5 -> reduce; if underbid -0.5 -> increase
        double factor = 1.0 - 0.15 * avgRelError; // ���� avgRelError positive -> smaller factor
        return clamp(factor, 0.5, 1.5);
    }
}

