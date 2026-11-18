package p2pbet.dataaccess;
import p2pbet.core.entities.Bet;
import p2pbet.core.entities.state.BetStatus;
import java.util.List;
public interface IBetRepository extends IRepository<Bet, Integer> {
    List<Bet> findByEventIdAndStatus(int eventId, BetStatus status);
    List<Bet> findByUserId(int userId);
}
