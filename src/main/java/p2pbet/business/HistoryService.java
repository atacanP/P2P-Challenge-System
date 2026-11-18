package p2pbet.business;
import p2pbet.core.entities.Bet;
import p2pbet.core.entities.GameRoom;
import p2pbet.dataaccess.IBetRepository;
import p2pbet.dataaccess.IGameRoomRepository;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class HistoryService {
    private final IBetRepository betRepository;
    private final IGameRoomRepository gameRoomRepository;
    public HistoryService(IBetRepository betRepository, IGameRoomRepository gameRoomRepository) {
        this.betRepository = betRepository;
        this.gameRoomRepository = gameRoomRepository;
    }
    public List<Bet> getUserBetHistory(int userId) {
        return betRepository.findByUserId(userId);
    }
    public List<GameRoom> getUserGameHistory(int userId) {
        return gameRoomRepository.findByUserId(userId);
    }
}
