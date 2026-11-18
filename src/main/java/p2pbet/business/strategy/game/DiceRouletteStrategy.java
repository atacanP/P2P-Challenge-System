package p2pbet.business.strategy.game;
import p2pbet.core.entities.Participant;
import org.springframework.stereotype.Component;
import p2pbet.core.entities.GameType;
import java.util.List;
import java.util.Random;
@Component
public class DiceRouletteStrategy implements IGameStrategy {
    private final Random random = new Random();
    @Override
    public Participant determineWinner(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return null;
        }
        int winningNumber = random.nextInt(6) + 1;
        String winningStr = String.valueOf(winningNumber);
        System.out.println("Dice rolled! The winning number is: " + winningStr);
        for (Participant p : participants) {
            if (winningStr.equals(p.getChoice())) {
                System.out.println("Winner found: " + p.getUserId() + " with choice " + winningStr);
                return p;
            }
        }
        System.out.println("No one guessed the winning number.");
        return null; // No winner
    }
    @Override
    public GameType getSupportedGameType() {
        return GameType.DICE_ROULETTE;
    }
}
