package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.DTOs.TrickBidDTO;
import JC.Training.src.WizardGame.enums.TrickBidExists;
import JC.Training.src.WizardGame.handlers.CardManager;
import JC.Training.src.WizardGame.DTOs.BeatCardContextDTO;
import JC.Training.src.WizardGame.DTOs.CreateTrickBidsContextDTO;
import JC.Training.src.WizardGame.DTOs.DropCardContextDTO;
import JC.Training.src.WizardGame.handlers.GameManager;
import JC.Training.src.WizardGame.models.Card;

import java.util.*;

public class Me implements GameStrategy{
    private final Scanner scanner = new Scanner(System.in);

    @Override
    public Card dropCard(DropCardContextDTO dropCardContextDTO) {
        System.out.println("DROP CARD");

        System.out.println("Trump card: " + dropCardContextDTO.roundContext().trumpCard().getType());
        List<Card> ownCards = new ArrayList<>(dropCardContextDTO.ownCards());



        return chooseCard(ownCards);
    }

    @Override
    public Card beatCard(BeatCardContextDTO beatCardContextDTO) {
        System.out.println("BEAT CARD");
        System.out.println("Trump card: " + beatCardContextDTO.roundContext().trumpCard().getType());

        System.out.println("Card to beat: " + beatCardContextDTO.trickContextDTO().firstDroppedCard());
        Set<Card> allowedCards = CardManager.determineAllowedCards(beatCardContextDTO.allowedResponses(), beatCardContextDTO.ownCards());


        return chooseCard(new ArrayList<>(allowedCards));
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO createTrickBidsContextDTO) {
        System.out.println("=========Round: " + createTrickBidsContextDTO.gameContextDTO().currentRoundNumber()+"===========");
        System.out.println("CREATE TRICK BIDS");

        System.out.println("Trump card: " + createTrickBidsContextDTO.trumpCard().getType());
        System.out.println("Your cards: ");

        createTrickBidsContextDTO.ownCards().forEach(c -> System.out.println(c.getType() + " " + c.getNumber()));

       Map<String, TrickBidDTO> map = createTrickBidsContextDTO.trickBids();

       if(createTrickBidsContextDTO.dealerId().equals(createTrickBidsContextDTO.ownID())){
           System.out.println("You create bids as first");
       }else{
           System.out.println("Bot bid: " + map.get(createTrickBidsContextDTO.dealerId()).trickBid());
       }


        System.out.println("Your bid: ");
       int bid = scanner.nextInt();
        return bid;
    }

    @Override
    public String getStrategyName() {
        return "Me";
    }

    private Card chooseCard(List<Card> ownCards){
        System.out.println("Your cards: ");

        for(int i = 0; i < ownCards.size(); i++){
            Card c = ownCards.get(i);

            System.out.println(i + ": " + c.getType() + " " + c.getNumber());
        }

        System.out.println("------------------");
        System.out.println("your card:");

        int index = scanner.nextInt();

        return ownCards.get(index);
    }
}
