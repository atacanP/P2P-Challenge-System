package p2pbet.business;
import p2pbet.core.entities.Bet;
import p2pbet.dataaccess.IBetRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class BetService {
    private final IBetRepository betRepository;
    private final WalletService walletService;
    private final p2pbet.dataaccess.IEventRepository eventRepository;
    public BetService(IBetRepository betRepository, WalletService walletService, p2pbet.dataaccess.IEventRepository eventRepository) {
        this.betRepository = betRepository;
        this.walletService = walletService;
        this.eventRepository = eventRepository;
    }
    public Bet createOffer(int creatorId, int eventId, double stake, String creatorSelection) {
        p2pbet.core.entities.Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found. Cannot create bet."));
        if (event.getStatus() != p2pbet.core.entities.Event.EventStatus.UPCOMING) {
            throw new p2pbet.core.entities.exception.EventStartedException("Event is not UPCOMING. Cannot create bet.");
        }
        if (!creatorSelection.equals(event.getOptionA()) && !creatorSelection.equals(event.getOptionB())) {
            throw new IllegalArgumentException("Creator selection must match one of the event options.");
        }
        walletService.reserveFunds(creatorId, stake);
        int generatedId = Math.abs(java.util.UUID.randomUUID().hashCode() % 1000000);
        Bet bet = new Bet(generatedId, creatorId, eventId, stake, creatorSelection);
        betRepository.save(bet);
        System.out.println("Bet Offer Created: " + bet.getId());
        return bet;
    }
    public void acceptOffer(int betId, int acceptorId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found: " + betId));
        if (!bet.isMatchable(acceptorId)) {
            throw new p2pbet.core.entities.exception.InvalidBetOperationException("Bet is not matchable by this user or already taken.");
        }
        p2pbet.core.entities.Event event = eventRepository.findById(bet.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found for this bet."));
        if (event.getStatus() != p2pbet.core.entities.Event.EventStatus.UPCOMING) {
            throw new p2pbet.core.entities.exception.EventStartedException("Event is not UPCOMING. Betting window closed.");
        }
        int expectedVersion = bet.getVersion();
        walletService.reserveFunds(acceptorId, bet.getStake());
        try {
            bet.accept(acceptorId, expectedVersion);
            bet.incrementVersion();
            betRepository.save(bet);
            System.out.println("Bet " + betId + " successfully matched with user " + acceptorId);
        } catch (p2pbet.core.entities.exception.OptimisticLockException e) {
            System.err.println("Match failed for user " + acceptorId + ": " + e.getMessage());
            walletService.refundFunds(acceptorId, bet.getStake());
            throw e;
        } catch (Exception e) {
            System.err.println("Match failed for user " + acceptorId + ": " + e.getMessage());
            walletService.refundFunds(acceptorId, bet.getStake());
            throw new RuntimeException("Matchmaking failed: " + e.getMessage());
        }
    }
    public void cancelOffer(int betId, int requesterId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new IllegalArgumentException("Bet not found: " + betId));
        if (bet.getCreatorId() != requesterId) {
            throw new IllegalArgumentException("Only the creator can cancel this bet.");
        }
        if (bet.getStatus() != p2pbet.core.entities.state.BetStatus.PENDING) {
            throw new p2pbet.core.entities.exception.InvalidBetOperationException("Sadece henüz eşleşmemiş (PENDING) bahisler iptal edilebilir.");
        }
        p2pbet.core.entities.Event event = eventRepository.findById(bet.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found for this bet."));
        if (event.getStatus() != p2pbet.core.entities.Event.EventStatus.UPCOMING) {
            throw new p2pbet.core.entities.exception.EventStartedException("Event is not UPCOMING. Betting window closed.");
        }
        bet.cancel();
        walletService.refundFunds(requesterId, bet.getStake());
        betRepository.save(bet);
        System.out.println("Bet " + betId + " cancelled.");
    }
    public List<Bet> getAvailableBets() {
        return betRepository.findAll().stream()
                .filter(bet -> bet.getStatus() == p2pbet.core.entities.state.BetStatus.PENDING)
                .filter(bet -> {
                    p2pbet.core.entities.Event event = eventRepository.findById(bet.getEventId()).orElse(null);
                    return event != null && event.getStatus() == p2pbet.core.entities.Event.EventStatus.UPCOMING;
                })
                .collect(Collectors.toList());
    }
    public List<Bet> getAllBets() {
        return betRepository.findAll();
    }
    public void cancelUnmatchedBetsForEvent(int eventId) {
        List<Bet> pendingBets = betRepository.findAll().stream()
                .filter(b -> b.getEventId() == eventId && b.getStatus() == p2pbet.core.entities.state.BetStatus.PENDING)
                .collect(Collectors.toList());
        for (Bet bet : pendingBets) {
            bet.cancel();
            walletService.refundFunds(bet.getCreatorId(), bet.getStake());
            betRepository.save(bet);
            System.out.println("System automatically cancelled unmatched Bet " + bet.getId() + " because event started.");
        }
    }
}
