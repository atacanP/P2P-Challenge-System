package p2pbet.core.entities.state;
import p2pbet.core.entities.Bet;
public interface BetState {
    void accept(Bet bet, int acceptorId, int expectedVersion);
    void cancel(Bet bet);
    void settle(Bet bet);
    BetStatus getStatus();
}
