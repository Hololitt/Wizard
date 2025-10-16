package JC.Training.src.WizardGame.strategies;

import JC.Training.src.WizardGame.DTOs.BeatCardContextDTO;
import JC.Training.src.WizardGame.DTOs.CreateTrickBidsContextDTO;
import JC.Training.src.WizardGame.DTOs.DropCardContextDTO;
import JC.Training.src.WizardGame.models.Card;

public class Hybrid implements GameStrategy{
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
        return analystV1.createTrickBids(createTrickBidsContextDTO);
    }

    @Override
    public String getStrategyName() {
        return "Hybrid";
    }
}
