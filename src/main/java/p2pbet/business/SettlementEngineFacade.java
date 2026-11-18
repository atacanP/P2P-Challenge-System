package p2pbet.business;
import p2pbet.core.entities.Bet;
import p2pbet.core.entities.state.BetStatus;
import p2pbet.dataaccess.IBetRepository;
import p2pbet.business.strategy.PayoutStrategy;
import java.util.List;
import org.springframework.stereotype.Service;
@Service
public class SettlementEngineFacade {
    private final IBetRepository betRepository;
    private final PayoutStrategy payoutStrategy;
    public SettlementEngineFacade(IBetRepository betRepository, PayoutStrategy payoutStrategy) {
        this.betRepository = betRepository;
        this.payoutStrategy = payoutStrategy;
    }
    public void triggerSettlement(int eventId, String finalResult) {
        System.out.println("Triggering settlement for event: " + eventId);
        List<Bet> matchedBets = betRepository.findByEventIdAndStatus(eventId, BetStatus.MATCHED);
        for (Bet bet : matchedBets) {
            try {
                payoutStrategy.executePayout(bet, finalResult);
                bet.settle();
                betRepository.save(bet);
            } catch (Exception e) {
                System.err.println("Failed to settle bet ID " + bet.getId() + ": " + e.getMessage());
            }
        }
        System.out.println("Settlement completed for event: " + eventId);
    }
}
