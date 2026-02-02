package WizardGame.strategies;

import WizardGame.handlers.CardManager;
import WizardGame.enums.CardType;
import WizardGame.DTOs.*;
import WizardGame.contexts.RoundContext;
import WizardGame.models.Card;

import java.util.*;
import java.util.stream.Collectors;

public class MetaCrusherV1 implements GameStrategy {

    // История ставок/выигрышей по игрокам (сбрасывается в начале новой партии)
    private final Map<String, List<Integer>> bidsHistory = new HashMap<>();
    private final Map<String, List<Integer>> winsHistory = new HashMap<>();

    // Параметры модели (тонкая настройка возможна)
    private final double WIZARD_WEIGHT = 1.0;
    private final double HIGH_TRUMP_WEIGHT = 0.9;
    private final double MEDIUM_TRUMP_WEIGHT = 0.6;
    private final double HIGH_NONTRUMP_WEIGHT = 0.6;
    private final double MEDIUM_NONTRUMP_WEIGHT = 0.35;
    private final double RISK_AVERSION_BASE = 0.25; // меньше — более агрессивно


    @Override
    public String getStrategyName() {
        return "MetaCrusherV1";
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        // Сброс истории в начале партии (если нужно)
        int round = ctx.gameContextDTO().currentRoundNumber();
        if (round == 1) {
            bidsHistory.clear();
            winsHistory.clear();
        }

        Set<Card> hand = ctx.ownCards();
        Card trump = ctx.trumpCard();
        GameContextDTO gameCtx = ctx.gameContextDTO();
        int totalRoundAmount = gameCtx.totalRoundAmount(); // не обязателен
        int botAmount = gameCtx.botAmount();

        // 1) Оцениваем «сильные» компоненты руки вероятностно
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

        // 2) Узнаём, как часто оппоненты в прошлых раундах делали большие ставки (простая метрика «агрессивности»)
        double opponentsAggression = 0.0;
        int opponentsCounted = 0;
        for (Map.Entry<String, Integer> entry : ctx.gameContextDTO().botScores().entrySet()) {
            String id = entry.getKey();
            // Пропускаем себя если вдруг
        }
        // используем current round context bids if available (faster сигнал)
        // NOTE: RoundContext не передан в CreateTrickBidsContextDTO, но у тебя в AnalystV1 использовалось roundContext в Drop/Beat
        // Здесь ориентируемся на прошлую историю (если есть)
        for (Map.Entry<String, List<Integer>> e : bidsHistory.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            opponentsAggression += avg;
            opponentsCounted++;
        }
        if (opponentsCounted > 0) opponentsAggression /= opponentsCounted; // средняя ставка оппонента

        // 3) Корректируем ожидание исходя из "внешнего спроса" на штихи:
        int knownAllBids = 0;
        for (List<Integer> v : bidsHistory.values()) {
            if (!v.isEmpty()) {
                knownAllBids += v.getLast(); // последняя заявка игрока
            }
        }
        // Используем коэффициент: если суммарные чужие ставки близки к текущему номеру раунда, рискуем меньше
        double crowdPressure = knownAllBids; // простая метрика

        // 4) Риск-адаптация: уменьшаем ставку, когда вероятность перебора высока
        double riskAversion = RISK_AVERSION_BASE;
        if (crowdPressure > hand.size() * 0.8) {
            riskAversion += 0.35; // сильнее боимся перебора
        } else if (crowdPressure < hand.size() * 0.4) {
            riskAversion -= 0.1; // более агрессивно
        }

        // 5) Окончательная ставка: округляем expectedTricks с учётом риска
        double rawBid = Math.max(0.0, expectedTricks * (1.0 - riskAversion));
        int bid = (int) Math.round(rawBid);

        // защитный fallback: не давать ставку, превышающую количество карт
        bid = Math.max(0, Math.min(bid, hand.size()));

        // сохраняем свою ставку в истории (идентификатор собственный недоступен, но вызывающий код в тестах даст контекст)
        // Для универсальности предположим, что реализация контролирует сохранение в game loop, но мы также можем
        // пополнить поле bidsHistory под ключ "ME" чтобы потом его использовать.
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
        int bid = round.fullTrickBids().get(ownId).trickBid();

        // Если уже взяли столько штихов, сколько заявили — слить: минимально ценный из тех, которые не убьют штих
        if (wins >= bid) {
            // попытаться слить карту, которая с наибольшей вероятностью НЕ возьмёт штих
            return chooseCardToLose(hand, trump, round);
        }

        // Если нужно взять штихи — сыграть «экономично сильную» карту:
        // 1) не тратить WIZARD если есть альтернативы, 2) не тратить топ-козыри на первом ходу, если можно выиграть средней картой
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
        int bid = round.fullTrickBids().get(ownId).trickBid();

        Set<CardType> allowedTypes = ctx.allowedResponses();
        Set<Card> hand = ctx.ownCards();
        Set<Card> allowedCards = CardManager.determineAllowedCards(allowedTypes, hand);

        if (allowedCards.isEmpty()) {
            allowedCards = hand;
        }

        // Если уже выполнил заявку — сбрасываем слабую из допустимых
        if (wins >= bid) {
            return getWeakest(allowedCards, ctx.roundContext().trumpCard());
        }

        // Иначе пытаемся побить минимальной возможной картой, сохранив ключевые ресурсы
        List<Card> canBeat = CardManager.determineCardsBeatCard(allowedCards, firstDropped, trumpType);

        if (!canBeat.isEmpty()) {
            // выбираем ту, которая побьёт, но с минимальной «стоимостью»
            canBeat.sort(Comparator.comparingInt(c -> cardStrength(c, ctx.roundContext().trumpCard())));
            // если самая слабая победная карта — WIZARD или сильный козырь, попробуем найти следующую минимально-убойную
            Card pick = canBeat.get(0);
            if (isWizard(pick) && canBeat.size() > 1) {
                // предпочитаем не тратить WIZARD, если можно
                return canBeat.get(1);
            }
            return pick;
        }

        // Не можем побить — сбрасываем самую бесполезную карту
        return chooseCardToLose(allowedCards, ctx.roundContext().trumpCard(), round);
    }

    // ===== Вспомогательные методы =====

    private Card chooseCardToLose(Set<Card> cards, Card trump, RoundContext round) {
        // 1) предпочитаем JESTER
        List<Card> jesters = cards.stream().filter(c -> c.getType() == CardType.JESTER).toList();
        if (!jesters.isEmpty()) return jesters.getFirst();

        // 2) предпочитаем низкие карты не-козыри и не-мачт (чтобы не случайно вытолкнуть взятие)
        List<Card> nonTrumpNonWizard = cards.stream()
                .filter(c -> c.getType() != trump.getType() && c.getType() != CardType.WIZARD)
                .sorted(Comparator.comparingInt(Card::getNumber))
                .toList();
        if (!nonTrumpNonWizard.isEmpty()) return nonTrumpNonWizard.getFirst();

        // 3) иначе — самое слабое по общей метрике
        return getWeakest(cards, trump);
    }

    private Card chooseCardToWin(Set<Card> cards, Card trump, RoundContext round) {
        // Не тратим WIZARD на ранних этапах, если можно
        List<Card> wizard = cards.stream().filter(c -> c.getType() == CardType.WIZARD).toList();
        List<Card> trumpCards = CardManager.getCardsOfType(cards, trump.getType()).stream().sorted(Comparator.comparingInt(Card::getNumber)).collect(Collectors.toList());

        // Ищем «дешёвую победную карту»: высокий не-козырь, или средний козырь, но не самый сильный
        List<Card> candidate = cards.stream()
                .filter(c -> c.getType() != CardType.WIZARD && c.getType() != CardType.JESTER)
                .sorted(Comparator.comparingInt(Card::getNumber).reversed())
                .toList();

        // Если есть средние козыри (не самые большие) — предпочтём их перед тратой WIZARD
        if (!trumpCards.isEmpty()) {
            // возьмём средний козырь (не самый маленький и не самый большой)
            if (trumpCards.size() >= 3) {
                return trumpCards.get(trumpCards.size() / 2);
            } else {
                // если есть хотя бы один козырь, но нет средних — возьмём самый маленький козырь, который всё ещё сильный
                return trumpCards.getFirst();
            }
        }

        // Если нет козырей — возьмём самый слабый из кандидатов, который скорее всего выиграет
        if (!candidate.isEmpty()) {
            return candidate.getFirst();
        }

        // Если ничего разумного — отдать WIZARD (крайняя мера)
        if (!wizard.isEmpty()) return wizard.getFirst();

        // Фоллбек
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
