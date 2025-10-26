package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.handlers.CardManager;
import JC.Training.src.WizardGame.enums.CardType;
import JC.Training.src.WizardGame.DTOs.BeatCardContextDTO;
import JC.Training.src.WizardGame.DTOs.CreateTrickBidsContextDTO;
import JC.Training.src.WizardGame.DTOs.DropCardContextDTO;
import JC.Training.src.WizardGame.contexts.RoundContext;
import JC.Training.src.WizardGame.models.Card;

import java.util.*;

public class ChatGPTV3 implements GameStrategy{

    private final DefaultStrategy defaultStrategy = new DefaultStrategy();

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO ctx) {
        Set<Card> cards = ctx.ownCards();
        Card trumpCard = ctx.trumpCard();
        int roundNumber = ctx.gameContextDTO().currentRoundNumber();

        int bid = 0;

        for (Card card : cards) {
            if (card.getType() == CardType.WIZARD) {
                bid++;
            } else if (card.getType() == trumpCard.getType()) {
                if (roundNumber <= 2 || card.getNumber() >= 9) {
                    bid++;
                }
            } else if (card.getType() != CardType.JESTER && card.getNumber() >= 11) {
                bid++;
            }
        }

        return bid;
    }

    @Override
    public Card dropCard(DropCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext roundContext = ctx.roundContext();
        Set<Card> ownCards = ctx.ownCards();
        Card trumpCard = roundContext.trumpCard();

        int bid = roundContext.fullTrickBids().get(ownId).trickBid();
        int wins = roundContext.fullTrickWins().getOrDefault(ownId, 0);

        // ���������� ���������� ���� (����������� ����)
        Set<Card> allowedCards = ownCards; // ��� ����� ��������� �� ������ ����

        Card chosenCard;
        if (bid == 0 || wins >= bid) {
            chosenCard = allowedCards.stream()
                    .min(Comparator.comparingInt(c -> cardStrength(c, trumpCard)))
                    .orElseThrow();
        } else {
            chosenCard = allowedCards.stream()
                    .max(Comparator.comparingInt(c -> cardStrength(c, trumpCard)))
                    .orElseThrow();
        }

        return chosenCard;
    }

    @Override
    public Card beatCard(BeatCardContextDTO ctx) {
        String ownId = ctx.ownId();
        RoundContext roundContext = ctx.roundContext();
        Card trumpCard = roundContext.trumpCard();
        Card firstDroppedCard = ctx.trickContextDTO().firstDroppedCard();

        int bid = roundContext.fullTrickBids().get(ownId).trickBid();
        int wins = roundContext.fullTrickWins().getOrDefault(ownId, 0);

        // �������� ���������� ���� � ��������� �����
        Set<CardType> allowedTypes = CardManager.defineAllowedResponseCardTypes(firstDroppedCard, ctx.ownCards());
        Set<Card> allowedCards = CardManager.determineAllowedCards(allowedTypes, ctx.ownCards());

        if (allowedCards.isEmpty()) {
            // fallback (�� ����, ���� �� ������, �� �� ������)
            return defaultStrategy.beatCard(ctx);
        }

        Card leadingCard = CardManager.defineLeadingCard(new HashSet<>(ctx.trickContextDTO().responses()), firstDroppedCard, trumpCard.getType());
        List<Card> beatingCards = CardManager.determineCardsBeatCard(allowedCards, leadingCard, trumpCard.getType());

        if (wins >= bid) {
            // ���������� ������ �����
            return CardManager.getWeakestCard(allowedCards, trumpCard.getType());
        }

        if (!beatingCards.isEmpty()) {
            // ��������� ������ ���������� ��������� ������
            return beatingCards.stream()
                    .min(Comparator.comparingInt(c -> cardStrength(c, trumpCard)))
                    .orElse(CardManager.getWeakestCard(allowedCards, trumpCard.getType()));
        }

        // �� ����� ������ � ����������
        return CardManager.getWeakestCard(allowedCards, trumpCard.getType());
    }

    @Override
    public String getStrategyName() {
        return "ChatGPT_V3";
    }

    // === ��������������� ������ ===

    private int cardStrength(Card card, Card trumpCard) {
        if (card.getType() == CardType.WIZARD) return 1000;
        if (card.getType() == CardType.JESTER) return -1;
        if (trumpCard != null && card.getType() == trumpCard.getType()) {
            return 100 + card.getNumber();
        }
        return card.getNumber();
    }
}
