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

    // Настраиваемые параметры — можешь изменять для тонкой калибровки
    private static final double EXTRA_BID_PROBABILITY = 0.18;   // с какой вероятностью добавляем +1 к ставке
    private static final double RANDOM_YIELD_PROBABILITY = 0.30; // при already fulfilled: вероятность уступить (дать взять)
    private static final double OVERRIDE_BEAT_HIGHER_PROB = 0.18; // при попытке взять: вероятность перебить выше, чтобы "провоцировать"
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
                // чуть более агрессивные требования для козырей
                if (c.getNumber() >= 8 || round <= 2) bid++;
            } else if (t != CardType.JESTER) {
                // для старших не-козырей требования строже
                if (c.getNumber() >= 12) bid++;
            }
        }

        // небольшая агрессия: иногда добавляем +1, чтобы "дразнить" Analyst (он заранее помечает taking cards)
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

        // Если уже набрал ставку — будем сбрасывать карты, которые Analyst скорее всего хочет сохранить.
        // Это мешает Analyst-у удерживать контроль над своими takingTrickCards.
        if (wins >= bid) {
            return getWorstForAnalyst(hand, trump);
        }

        // Если ещё не набрал — играем агрессивно, но иногда смешиваем с менее сильным ходом (рандом)
        if (RNG.nextDouble() < 0.12) {
            // редкий случай — сыграть не самую сильную, чтобы сохранять козы
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

        // допустимые ответы по типам (безопасно)
        Set<CardType> allowedTypes = ctx.allowedResponses();
        Set<Card> allowedCards = CardManager.determineAllowedCards(allowedTypes, ctx.ownCards());
        if (allowedCards == null || allowedCards.isEmpty()) {
            allowedCards = new HashSet<>(ctx.ownCards());
        }

        Card leadingCard = CardManager.defineLeadingCard(new HashSet<>(trick.responses()), firstDropped, trumpType);

        // Если уже набрал ставку — в основном сбрасываем слабую, но иногда "уступаем" намеренно
        if (wins >= bid) {
            if (RNG.nextDouble() < RANDOM_YIELD_PROBABILITY) {
                // намеренно не мешаем — даём противнику возможность взять (вынуждает Analyst менять планы)
                return CardManager.getWeakestCard(allowedCards, trumpType);
            } else {
                // в остальном — сбрасываем карту, которую Analyst считает важной (сильную)
                return getWorstForAnalyst(allowedCards, round.trumpCard());
            }
        }

        // Ещё не набрали — пробуем побить минимально. Но с небольшой вероятностью "перебиваем" выше,
        // чтобы вынудить противника тратить сильные карты.
        List<Card> beating = CardManager.determineCardsBeatCard(allowedCards, leadingCard, trumpType);
        if (beating != null && !beating.isEmpty()) {
            // сортируем по силе (слабей -> сильней)
            List<Card> sorted = beating.stream()
                    .sorted(Comparator.comparingInt(c -> cardStrength(c, round.trumpCard())))
                    .collect(Collectors.toList());

            if (RNG.nextDouble() < OVERRIDE_BEAT_HIGHER_PROB && sorted.size() > 1) {
                // берем чуть более сильную карту, чтобы "провоцировать" противника
                return sorted.get(sorted.size() - 1);
            } else {
                // минимально нужная карта
                return sorted.get(0);
            }
        }

        // Не можем побить — сбрасываем слабую (обычная тактика)
        return CardManager.getWeakestCard(allowedCards, trumpType);
    }

    // ----------------- вспомогательные методы -----------------

    private Card getWorstForAnalyst(Set<Card> cards, Card trump) {
        // "Хуже для Analyst" — это в нашем понимании самая сильная карта (которую он хотел бы сохранить).
        // Сортируем по силе и возвращаем самую сильную.
        return getStrongestCardSafe(cards, trump);
    }

    private Card getConservativeAggressive(Set<Card> cards, Card trump) {
        // Возвращаем сильную, но не самую-самую (например, второй сверху), чтобы сохранить топовые ресурсы
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
            return 200 + c.getNumber(); // козы выше простых
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

