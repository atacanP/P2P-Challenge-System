package p2pbet.business.strategy;
import p2pbet.core.entities.Bet;
public interface PayoutStrategy {
    void executePayout(Bet bet, String eventResult);
}
