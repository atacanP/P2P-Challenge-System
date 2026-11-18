package p2pbet.business.strategy.game;
import p2pbet.core.entities.Participant;
import p2pbet.core.entities.GameType;
import java.util.List;
public interface IGameStrategy {
    Participant determineWinner(List<Participant> participants);
    GameType getSupportedGameType();
}
