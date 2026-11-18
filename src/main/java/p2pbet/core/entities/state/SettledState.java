package p2pbet.core.entities.state;
import p2pbet.core.entities.Bet;
public class SettledState implements BetState {
    @Override
    public void accept(Bet bet, int acceptorId, int expectedVersion) {
        throw new IllegalStateException("Bet is already settled.");
    }
    @Override
    public void cancel(Bet bet) {
        throw new IllegalStateException("Cannot cancel a settled bet.");
    }
    @Override
    public void settle(Bet bet) {
        throw new IllegalStateException("Bet is already settled.");
    }
    @Override
    public BetStatus getStatus() {
        return BetStatus.SETTLED;
    }
}
