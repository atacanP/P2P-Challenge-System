package p2pbet.core.entities.state;
import p2pbet.core.entities.Bet;
public class MatchedState implements BetState {
    @Override
    public void accept(Bet bet, int acceptorId, int expectedVersion) {
        throw new IllegalStateException("Bet is already matched.");
    }
    @Override
    public void cancel(Bet bet) {
        throw new IllegalStateException("Cannot cancel a matched bet.");
    }
    @Override
    public void settle(Bet bet) {
        bet.setState(new SettledState());
        bet.incrementVersion();
    }
    @Override
    public BetStatus getStatus() {
        return BetStatus.MATCHED;
    }
}
