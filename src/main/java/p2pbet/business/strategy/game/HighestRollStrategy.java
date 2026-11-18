package p2pbet.business.strategy.game;
import p2pbet.core.entities.Participant;
import org.springframework.stereotype.Component;
import p2pbet.core.entities.GameType;
import java.util.List;
import java.util.Random;
@Component
public class HighestRollStrategy implements IGameStrategy {
    private final Random random = new Random();
    @Override
    public Participant determineWinner(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return null;
        }
        Participant highestParticipant = null;
        int maxRoll = -1;
        boolean isTie = false;
        for (Participant p : participants) {
            int roll = random.nextInt(100) + 1; // Roll between 1 and 100
            System.out.println("HighestRollStrategy: Participant " + p.getUserId() + " rolled " + roll);
            if (roll > maxRoll) {
                maxRoll = roll;
                highestParticipant = p;
                isTie = false; // Reset tie if we found a new highest
            } else if (roll == maxRoll) {
                isTie = true; // Tie for the highest roll
            }
        }
        if (isTie) {
            System.out.println("HighestRollStrategy: Tie detected with max roll " + maxRoll + ". No winner.");
            return null; // Tie means refund
        }
        return highestParticipant;
    }
    @Override
    public GameType getSupportedGameType() {
        return GameType.HIGHEST_ROLL;
    }
}
