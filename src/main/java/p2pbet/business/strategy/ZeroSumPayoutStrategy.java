package p2pbet.business.strategy;
import p2pbet.core.entities.Bet;
import p2pbet.business.WalletService;
import org.springframework.stereotype.Component;
@Component
public class ZeroSumPayoutStrategy implements PayoutStrategy {
    private final WalletService walletService;
    public ZeroSumPayoutStrategy(WalletService walletService) {
        this.walletService = walletService;
    }
    @Override
    public void executePayout(Bet bet, String eventResult) {
        int winnerId;
        int loserId;
        if (eventResult.equalsIgnoreCase(bet.getCreatorSelection())) {
            winnerId = bet.getCreatorId();
            loserId = bet.getAcceptorId();
        } else {
            winnerId = bet.getAcceptorId();
            loserId = bet.getCreatorId();
        }
        double stake = bet.getStake();
        System.out.println("Executing Zero-Sum Payout for Bet ID: " + bet.getId() + ". Winner: " + winnerId);
        walletService.refundFunds(winnerId, stake);
        walletService.releaseAndTransfer(loserId, winnerId, stake);
    }
}
