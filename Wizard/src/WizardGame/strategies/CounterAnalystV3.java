package WizardGame.strategies;

import WizardGame.DTOs.*;
import WizardGame.contexts.RoundContext;
import WizardGame.enums.CardType;
import WizardGame.handlers.CardManager;
import WizardGame.models.Card;

import java.util.*;
import java.util.stream.Collectors;

public class CounterAnalystV3 implements GameStrategy {

    // Истории последних N раундов по игрокам (ставки и взятия) — используются для детекции паттернов
    private final int HISTORY_SIZE = 8;
    private final Map<String, Deque<Integer>> recentBids = new HashMap<>();
    private final Map<String, Deque<Integer>> recentWins = new HashMap<>();

    // Параметры стратегии (можно тонко настраивать)
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
        // Если новый раунд 1 — сбрасываем историю между играми
        int roundNumber = ctx.gameContextDTO().currentRoundNumber();
        if (roundNumber == 1) {
            recentBids.clear();
            recentWins.clear();
        }

        Set<Card> hand = ctx.ownCards();
        Card trump = ctx.trumpCard();
        int handSize = hand.size();

        // 1) Оценка силы руки: суммарная "оценочная" вероятность взятия штихов
        double scoreSum = 0.0;
        for (Card c : hand) {
            scoreSum += heuristicCardScore(c, trump);
        }

        // Нормируем: чем больше потенциальная сумма — тем выше raw expectation
        // приблизим: средняя сила карты ~ 100..300 => expectedTricks ~ scoreSum / (handSize * 200)
        double avgCardScore = (handSize > 0) ? scoreSum / handSize : 0.0;
        double expectedTricks = (scoreSum) / 200.0; // грубая шкала; можно менять

        // 2) Собираем "агрессию" оппонентов: средняя их последней ставки (если есть)
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

        // 3) Детекция Analyst-like оппонентов: если у кого-то часто bid == wins или малая дисперсия —
        // делаем пометку и скорректируем свою стратегию
        boolean analystPresent = detectAnalystPattern(ctx.ownID());

        // 4) Параметр риска — уменьшаем риск если суммарные чужие ставки близки к текущему roundNumber
        // Используем суммарный known bids (последние) как proxy
        double riskFactor = getRiskFactor(handSize);

        // 5) Exploit: если нашли Analyst-like оппонента — немного смещаем ставку в ту сторону,
        // которая заставит его чаще ошибаться: на средних длинах партий (round 6..(total-3)) — становимся чуть агрессивнее,
        // на очень длинных — более осторожны (Analyst лучше на дистанции)
        double exploitBias = 0.0;
        if (analystPresent) {
            int totalRounds = ctx.gameContextDTO().totalRoundAmount();
            if (roundNumber >= Math.max(1, totalRounds / 4) && roundNumber <= Math.max(1, totalRounds - totalRounds / 6)) {
                // середина партии — агрессия
                exploitBias = -5;
            } else {
                // начало/конец — меньше риска
                exploitBias = -0.1;
            }
        }

        // Compose final raw bid (non-negative)
        double rawBid = Math.max(0.0, expectedTricks * riskFactor + exploitBias);

        // Стабильный округ — используем округление вниз с небольшим шансом +1, чтобы добавить непредсказуемость
        int bid = (int) Math.floor(rawBid);
        if (rnd.nextDouble() < 0.08) bid++; // небольшой стохастический элемент

        // Защита: не ставим больше, чем карт
        bid = Math.max(0, Math.min(bid, handSize));

        // Сохраняем свою ставку в истории
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
        // более осторожно, если crowdPressure высоко relative to handSize
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

        // Обновляем истории (получаем последние bids/wins от контекста)
        updateHistoriesFromRoundContext(round);

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().get(ownId).trickBid();
        int roundNumber = round.number();
        int totalRounds = ctx.gameContextDTO().totalRoundAmount();

        boolean analystPresent = detectAnalystPattern(ctx.ownId());

        // Если уже собрали столько штихов, сколько сказали — сливаем наименее опасную карту
        if (wins >= bid) {
            return chooseCardToLose(hand, trump, roundNumber, totalRounds, analystPresent);
        }

        // Надо брать: выбираем минимально-убойную карту, но с учётом сохранения ключевых ресурсов
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

        // Обновляем истории
        updateHistoriesFromRoundContext(round);

        int wins = round.fullTrickWins().getOrDefault(ownId, 0);
        int bid = round.fullTrickBids().get(ownId).trickBid();

        Set<CardType> allowedTypes = ctx.allowedResponses();
        Set<Card> hand = ctx.ownCards();
        Set<Card> allowed = determineAllowedCards(allowedTypes, hand);
        if (allowed.isEmpty()) allowed = new HashSet<>(hand);

        boolean analystPresent = detectAnalystPattern(ctx.ownId());

        // Если уже выполнили заявку — сбрасываем максимально безопасную карту
        if (wins >= bid) {
            return chooseCardToLose(allowed, trump, round.number(), ctx.gameContextDTO().totalRoundAmount(), analystPresent);
        }

        // Попытка побить минимально возможной картой
        List<Card> canBeat = determineCardsBeatCard(allowed, first, trump.getType());
        if (!canBeat.isEmpty()) {
            // сортируем по "стоимости" (слабее дешевле)
            CardManager.sortCardsFromMinToMax(new HashSet<>(canBeat));

            // prefer not to spend WIZARD if we can avoid
            Card pick = canBeat.get(0);
            if (isWizard(pick) && canBeat.size() > 1) {
                // если есть второй вариант — используем его
                pick = canBeat.get(1);
            }

            // Если Analyst выявлен и мы в середине партии — можем предпочесть средний козырь, чтобы сломать планы
            if (analystPresent && isTrump(pick, trump) && rnd.nextDouble() < 0.12) {
                // попытка выбрать средний козырь вместо самого слабого победного
                Optional<Card> midTrump = canBeat.stream().filter(c -> isTrump(c, trump) && c.getNumber() >= 7 && c.getNumber() <= 11).findFirst();
                if (midTrump.isPresent()) pick = midTrump.get();
            }

            return pick;
        }

        // Не можем побить — слить лучшее "неопасное" (jester -> low non-trump)
        return chooseCardToLose(allowed, trump, round.number(), ctx.gameContextDTO().totalRoundAmount(), analystPresent);
    }

    // -------------------- Helpers --------------------

    // обрезка очереди до HISTORY_SIZE
    private void trimDeque(Deque<Integer> dq) {
        while (dq.size() > HISTORY_SIZE) dq.removeFirst();
    }

    // обновление recentBids/recentWins по RoundContext (используем при drop/beat, т.к. там присутствует fullTrickBids/wins)
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

    // очень простая детекция Analyst-паттерна: часто bid == wins и малая дисперсия ставок
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
                // Если в >60% последних раундов у игрока bid == wins -> candidate
                if ((double) matches / Math.max(1, i) > 0.60) return true;

                // Также проверим дисперсию ставок: если stddev маленькая (почти постоянная ставка)
                double avg = bids.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                double var = bids.stream().mapToDouble(v -> (v - avg) * (v - avg)).average().orElse(0.0);
                if (var < 1.2) return true;
            }
        }
        return false;
    }

    // Оценка "силы" карты — улучшенная метрика, аналогичная CardManager.cardStrength, но даёт double
    private double heuristicCardScore(Card c, Card trump) {
        if (c.getType() == CardType.WIZARD) return WIZARD_SCORE;
        if (c.getType() == CardType.JESTER) return JESTER_SCORE;
        if (trump != null && c.getType() == trump.getType()) {
            // козыри сильнее
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

    // выбор карты для сливания (минимизируем вероятность случайного взятия)
    private Card chooseCardToLose(Set<Card> cards, Card trump, int roundNumber, int totalRounds, boolean analystPresent) {
        // 1) Jester — всегда первый кандидат
        List<Card> jesters = cards.stream().filter(this::isJester).toList();
        if (!jesters.isEmpty()) return jesters.getFirst();

        // 2) низкие не-козыри (ортогонально — не тратить масть, которой уже несколько карт в руке)
        List<Card> nonTrump = cards.stream().filter(c -> !isTrump(c, trump) && !isWizard(c) && !isJester(c))
                .sorted(Comparator.comparingInt(Card::getNumber))
                .toList();
        if (!nonTrump.isEmpty()) return nonTrump.getFirst();

        // 3) если только козыри/визы остаются — отдаём самый слабый козырь (при этом на поздних раундах можно сберечь средние)
        List<Card> trumpCards = cards.stream().filter(c -> isTrump(c, trump) && !isWizard(c)).sorted(Comparator.comparingInt(Card::getNumber)).collect(Collectors.toList());
        if (!trumpCards.isEmpty()) {
            // если Analyst присутствует и мы в середине партии — не отдаём средние козыри (сохраняем для эксплойта)
            if (analystPresent && roundNumber > totalRounds / 4 && roundNumber < totalRounds - 2) {
                // отдаём самый маленький козырь
                return trumpCards.getFirst();
            } else {
                return trumpCards.getFirst();
            }
        }

        // 4) как крайний — отдаём самый слабый по общему рангу
        return cards.stream().min(Comparator.comparingDouble(c -> heuristicCardScore(c, trump))).orElseThrow();
    }

    // выбор карты для попытки выиграть — минимально достаточная, с сохранением WIZARD/top-trump
    private Card chooseCardToWin(Set<Card> cards, Card trump, int roundNumber, int totalRounds, boolean analystPresent) {
        // 1) попытка найти сильный некозырный высокий (если он вероятно будет лидером при отсутствии козыря)
        List<Card> nonTrumpsSortedDesc = cards.stream()
                .filter(c -> !isTrump(c, trump) && !isWizard(c))
                .sorted(Comparator.comparingInt(Card::getNumber).reversed())
                .toList();
        if (!nonTrumpsSortedDesc.isEmpty()) {
            // предпочитаем не тратить лучший из лучших, берем середнячок/высокую карту, но не топ
            Card candidate = nonTrumpsSortedDesc.getFirst();
            if (nonTrumpsSortedDesc.size() > 1) {
                // если есть второй по силе — возможно он победит, используем его (экономим топ)
                candidate = nonTrumpsSortedDesc.get(Math.min(1, nonTrumpsSortedDesc.size() - 1));
            }
            return candidate;
        }

        // 2) использовать средний козырь (не топ) — чтобы не расходовать на ранних этапах
        List<Card> trumpCards = cards.stream().filter(c -> isTrump(c, trump) && !isWizard(c)).sorted(Comparator.comparingInt(Card::getNumber)).collect(Collectors.toList());
        if (!trumpCards.isEmpty()) {
            // выбираем "средний" козырь
            int idx = Math.max(0, trumpCards.size() / 2 - 1);
            return trumpCards.get(idx);
        }

        // 3) Если есть WIZARD — использовать только если нет альтернатив и если нам очень нужно (в конце партии)
        List<Card> wizards = cards.stream().filter(this::isWizard).toList();
        if (!wizards.isEmpty()) {
            // используем в конце партии или если у нас очень низкая вероятность выиграть без него
            if (roundNumber > totalRounds - 3 || rnd.nextDouble() < 0.06) {
                return wizards.getFirst();
            }
        }

        // 4) fallback — самый сильный по общей метрике
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
                // невозможно побить WIZARD
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

