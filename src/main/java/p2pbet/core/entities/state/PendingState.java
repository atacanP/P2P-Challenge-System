package p2pbet.core.entities.state;
import p2pbet.core.entities.Bet;
public class PendingState implements BetState {
    @Override
    public void accept(Bet bet, int acceptorId, int expectedVersion) {
        if (bet.getVersion() != expectedVersion) {
            throw new p2pbet.core.entities.exception.OptimisticLockException("OptimisticLockException: Bet version mismatch.");
        }
        bet.setAcceptorId(acceptorId);
        bet.setState(new MatchedState());
    }
    @Override
    public void cancel(Bet bet) {
        bet.setState(new CancelledState());
        bet.incrementVersion();
    }
    @Override
    public void settle(Bet bet) {
        throw new IllegalStateException("Cannot settle a pending bet.");
    }
    @Override
    public BetStatus getStatus() {
        return BetStatus.PENDING;
    }
}
