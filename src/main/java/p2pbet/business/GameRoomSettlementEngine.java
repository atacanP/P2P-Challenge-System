package p2pbet.business;
import p2pbet.core.entities.GameRoom;
import p2pbet.core.entities.Participant;
import org.springframework.stereotype.Service;
@Service
public class GameRoomSettlementEngine {
    private final WalletService walletService;
    public GameRoomSettlementEngine(WalletService walletService) {
        this.walletService = walletService;
    }
    public void executeZeroSumPayout(GameRoom room, Participant winner) {
        int winnerId = winner.getUserId();
        for (Participant p : room.getParticipants()) {
            if (p.getUserId() == winnerId) {
                walletService.refundFunds(winnerId, room.getEntryFee());
            } else {
                walletService.releaseAndTransfer(p.getUserId(), winnerId, room.getEntryFee());
            }
        }
    }
    public void executeRefunds(GameRoom room) {
        System.out.println("No winner determined or game aborted. Refunding all participants.");
        for (Participant p : room.getParticipants()) {
            walletService.refundFunds(p.getUserId(), room.getEntryFee());
        }
    }
}
