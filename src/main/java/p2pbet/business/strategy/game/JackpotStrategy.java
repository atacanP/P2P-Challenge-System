package p2pbet.business.strategy.game;
import p2pbet.core.entities.Participant;
import org.springframework.stereotype.Component;
import p2pbet.core.entities.GameType;
import java.util.List;
import java.util.Random;
@Component
public class JackpotStrategy implements IGameStrategy {
    private final Random random = new Random();
    @Override
    public Participant determineWinner(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return null;
        }
        int winnerIndex = random.nextInt(participants.size());
        Participant winner = participants.get(winnerIndex);
        System.out.println("Jackpot drawn! Winner is randomly selected: " + winner.getUserId());
        return winner;
    }
    @Override
    public GameType getSupportedGameType() {
        return GameType.JACKPOT;
    }
}
