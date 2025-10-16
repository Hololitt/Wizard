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
 * PrecisionCounter — стратегия, ориентированная на аккуратную реализацию ставок и минимизацию overtricks.
 * Идея: копировать сильные стороны AnalystV1 (определение "taking" карт), но:
 *  - уменьшать ставку на 1 при наличии риска overtrick,
 *  - минимально биться если нужно,
 *  - при уже выполненной ставке отдавать карты, которые мешают Analyst'у сохранить taking-cards.
 */
public class CounterAnalystV2 implements GameStrategy {

    private final Set<Card> predictedTakingCards = new HashSet<>();


    @Override
    public String getStrategyName() {
        return "CounterAnalystV2";
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        // Определяем candidate taking cards по правилам, близким к AnalystV1,
        // но уменьшаем итоговую ставку на 1 (если >0) чтобы избежать overtricks.
        Set<Card> cards = ctx.ownCards();
        Card trump = ctx.trumpCard();
        int round = ctx.gameContextDTO().currentRoundNumber();

        Set<Card> taking = new HashSet<>();
        for (Card c : cards) {
            if (isForTakingTrick(c, trump, round)) {
                taking.add(c);
            }
        }

        // Копируем прогноз для use в выборе карт
        predictedTakingCards.clear();
        predictedTakingCards.addAll(taking);

        int predicted = taking.size();
        // Консервативное снижение риска: если predicted > 0, уменьшаем на 1 (но не меньше 0)
        int bid = Math.max(0, predicted - 1);

        // Ещё небольшая коррекция: если рука явно сильная (много WIZARD), вернуть реальные количество
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

        // Если уже выполнил ставку — сбрасываем "больные" для Analyst карты (сильные/козы),
        // чтобы он не смог реализовать свои taking-cards.
        if (wins >= bids) {
            // 1) Если есть козы/вишисты среди allowed (у нас все allowed на drop), сбрасываем самую сильную козу
            Optional<Card> strongestTrump = hand.stream()
                    .filter(c -> c.getType() == trumpType && c.getType() != CardType.WIZARD)
                    .max(Comparator.comparingInt(c -> c.getNumber()));
            if (strongestTrump.isPresent()) return strongestTrump.get();

            // 2) Иначе отдать самый сильный (портить планы)
            return safeGetStrongest(hand, round.trumpCard());
        }

        // Если ещё не добрал — пытаемся играть аккуратно, оставляя топовые ресурсы:
        // Выбираем сильную карту, но не самый топ (чтобы сохранить WIZARD/топ козы для решающих моментов).
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

        // Определяем допустимые карты (не мутируем входные коллекции)
        Set<CardType> allowedTypes = ctx.allowedResponses();
        Set<Card> allowed = CardManager.determineAllowedCards(allowedTypes, ctx.ownCards());
        if (allowed == null || allowed.isEmpty()) {
            allowed = new HashSet<>(ctx.ownCards());
        }

        // Определяем ведущую карту корректно
        Card leading = CardManager.defineLeadingCard(new HashSet<>(trick.responses()), firstDropped, trumpType);

        // Если уже набрал ставку — не бьём (если есть не-бьющие карты), иначе сбрасываем "вредные" карты
        if (wins >= bids) {
            // Попробуем найти карту, которая не бьёт ведущую (чтобы не дать лишний штих)
            List<Card> nonBeating = allowed.stream()
                    .filter(c -> !canBeat(c, leading, round.trumpCard()))
                    .toList();
            if (!nonBeating.isEmpty()) {
                // отдаём наименее вредную (самую слабую из non-beating)
                Set<Card> finalAllowed = allowed;
                return nonBeating.stream()
                        .min(Comparator.comparingInt(c -> cardStrength(c, round.trumpCard())))
                        .orElseGet(() -> safeGetWeakest(finalAllowed, round.trumpCard()));
            } else {
                // Если нет non-beating (все бьют) — отдаём weakest чтобы минимально пострадать
                return safeGetWeakest(allowed, round.trumpCard());
            }
        }

        // Ещё не добрал — нужно попытаться взять, но минимально. Анализируем возможные beating карты:
        List<Card> beating = CardManager.determineCardsBeatCard(allowed, leading, trumpType);
        if (beating != null && !beating.isEmpty()) {
            // Выберем минимальную по силе карту, которая побьёт (чтобы тратить минимально)
            Card minimal = beating.stream()
                    .min(Comparator.comparingInt(c -> cardStrength(c, round.trumpCard())))
                    .orElse(null);
            if (minimal != null) {
                return minimal;
            }
        }

        // Если не можем побить — сбрасываем самую слабую
        return safeGetWeakest(allowed, round.trumpCard());
    }

    // ----------------- Вспомогательные и безопасные методы -----------------

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
        // Возвращаем карту: сильная, но не самую сильную — оставляем WIZARD/топ козу
        List<Card> ordered = hand.stream()
                .sorted(Comparator.comparingInt((Card c) -> cardStrength(c, trump)))
                .collect(Collectors.toList());
        int n = ordered.size();
        if (n == 0) throw new IllegalStateException("Empty hand");
        if (n == 1) return ordered.get(0);

        // Если есть WIZARD — не тратить его, вернём вторую сверху
        if (ordered.get(n - 1).getType() == CardType.WIZARD) {
            return ordered.get(Math.max(0, n - 2));
        }
        // вернуть вторую сильнейшую, чтобы сохранить топ-ресурсы
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

