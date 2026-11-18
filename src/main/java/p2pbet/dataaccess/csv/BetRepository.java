package p2pbet.dataaccess.csv;
import p2pbet.core.entities.Bet;
import p2pbet.core.entities.state.BetStatus;
import p2pbet.dataaccess.IBetRepository;
import p2pbet.dataaccess.IDataStorage;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
@Repository
public class BetRepository extends AbstractCsvRepository<Bet, Integer> implements IBetRepository {
    public BetRepository(IDataStorage dataStorage) {
        super(dataStorage, "bets.csv", "betId,creatorId,acceptorId,eventId,stake,creatorSelection,version,status,createdAt");
    }
    @Override
    protected Bet mapFromString(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length >= 9) {
            int id = Integer.parseInt(parts[0]);
            int creatorId = Integer.parseInt(parts[1]);
            int acceptorId = parts[2].equals("null") ? -1 : Integer.parseInt(parts[2]);
            int eventId = Integer.parseInt(parts[3]);
            double stake = Double.parseDouble(parts[4]);
            String creatorSelection = parts[5];
            int version = Integer.parseInt(parts[6]);
            BetStatus status = BetStatus.valueOf(parts[7]);
            String createdAt = parts[8];
            return new Bet(id, creatorId, acceptorId, eventId, stake, creatorSelection, version, status, createdAt);
        }
        return null;
    }
    @Override
    protected String mapToString(Bet bet) {
        return bet.getId() + "," +
               bet.getCreatorId() + "," +
               (bet.getAcceptorId() == -1 ? "null" : bet.getAcceptorId()) + "," +
               bet.getEventId() + "," +
               bet.getStake() + "," +
               bet.getCreatorSelection() + "," +
               bet.getVersion() + "," +
               bet.getStatus().name() + "," +
               bet.getCreatedAt();
    }
    @Override
    protected Integer getId(Bet bet) {
        return bet.getId();
    }
    @Override
    public List<Bet> findByEventIdAndStatus(int eventId, BetStatus status) {
        return cache.values().stream()
                .filter(b -> b.getEventId() == eventId && b.getStatus() == status)
                .collect(Collectors.toList());
    }
    @Override
    public List<Bet> findByUserId(int userId) {
        return cache.values().stream()
                .filter(b -> b.getCreatorId() == userId || b.getAcceptorId() == userId)
                .collect(Collectors.toList());
    }
}
