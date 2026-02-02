package WizardGame.strategies;

import WizardGame.DTOs.BeatCardContextDTO;
import WizardGame.DTOs.CreateTrickBidsContextDTO;
import WizardGame.DTOs.DropCardContextDTO;
import WizardGame.models.Card;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Hybrid implements GameStrategy {
private final AnalystV1 analystV1 = new AnalystV1();
private final ChatGPTV4 chatGPTV4 = new ChatGPTV4();
private final ChatGPTV5 chatGPTV5 = new ChatGPTV5();

    @Override
    public Card dropCard(DropCardContextDTO dropCardContextDTO) {
       return chatGPTV5.dropCard(dropCardContextDTO);
    }

    @Override
    public Card beatCard(BeatCardContextDTO beatCardContextDTO) {
        return analystV1.beatCard(beatCardContextDTO);
    }

    @Override
    public Integer createTrickBids(CreateTrickBidsContextDTO createTrickBidsContextDTO) {
       Set<Card> ownCards = createTrickBidsContextDTO.ownCards();
        Map<String, DropCardContextDTO> dropCardContextDTOMap = new HashMap<>();

        return analystV1.createTrickBids(createTrickBidsContextDTO);
    }

    @Override
    public String getStrategyName() {
        return "Hybrid";
    }
}
