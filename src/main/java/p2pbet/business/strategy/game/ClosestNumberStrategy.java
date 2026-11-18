package p2pbet.business.strategy.game;
import p2pbet.core.entities.Participant;
import org.springframework.stereotype.Component;
import p2pbet.core.entities.GameType;
import java.util.List;
import java.util.Random;
@Component
public class ClosestNumberStrategy implements IGameStrategy {
    private final Random random = new Random();
    @Override
    public Participant determineWinner(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return null;
        }
        int target = random.nextInt(1000) + 1; // 1 to 1000
        System.out.println("ClosestNumberStrategy: Target number is " + target);
        Participant closestParticipant = null;
        int minDifference = Integer.MAX_VALUE;
        boolean isTie = false;
        for (Participant p : participants) {
            try {
                int choice = Integer.parseInt(p.getChoice());
                int diff = Math.abs(target - choice);
                if (diff < minDifference) {
                    minDifference = diff;
                    closestParticipant = p;
                    isTie = false;
                } else if (diff == minDifference) {
                    isTie = true;
                }
            } catch (NumberFormatException e) {
                System.out.println("ClosestNumberStrategy: Invalid numeric choice for participant " + p.getUserId() + " - skipping.");
            }
        }
        if (isTie || closestParticipant == null) {
            System.out.println("ClosestNumberStrategy: Tie detected or no valid players. No winner.");
            return null; // Tie means refund
        }
        return closestParticipant;
    }
    @Override
    public GameType getSupportedGameType() {
        return GameType.CLOSEST_NUMBER;
    }
}
