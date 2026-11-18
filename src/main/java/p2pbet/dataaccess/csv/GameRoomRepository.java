package p2pbet.dataaccess.csv;
import p2pbet.core.entities.GameRoom;
import p2pbet.core.entities.GameRoomStatus;
import p2pbet.core.entities.GameType;
import p2pbet.core.entities.Participant;
import p2pbet.dataaccess.IDataStorage;
import p2pbet.dataaccess.IGameRoomRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;
@Repository
public class GameRoomRepository extends AbstractCsvRepository<GameRoom, Integer> implements IGameRoomRepository {
    public GameRoomRepository(IDataStorage dataStorage) {
        super(dataStorage, "game_rooms.csv", "roomId,gameType,entryFee,capacity,poolAmount,status,participants,winnerId,createdAt");
    }
    @Override
    protected GameRoom mapFromString(String line) {
        String[] parts = line.split(",", -1); // Keep empty trailing strings
        if (parts.length >= 8) {
            int roomId = Integer.parseInt(parts[0]);
            GameType gameType = GameType.valueOf(parts[1]);
            double entryFee = Double.parseDouble(parts[2]);
            int capacity = Integer.parseInt(parts[3]);
            double poolAmount = Double.parseDouble(parts[4]);
            GameRoomStatus status = GameRoomStatus.valueOf(parts[5]);
            List<Participant> participants = new ArrayList<>();
            String participantsStr = parts[6];
            if (!participantsStr.equals("NONE") && !participantsStr.isEmpty()) {
                String[] pArray = participantsStr.split("\\|");
                for (String pData : pArray) {
                    String[] pParts = pData.split(":");
                    if (pParts.length == 2) {
                        participants.add(new Participant(Integer.parseInt(pParts[0]), pParts[1]));
                    }
                }
            }
            Integer winnerId = null;
            if (!parts[7].isEmpty()) {
                winnerId = Integer.parseInt(parts[7]);
            }
            String createdAt = "";
            if (parts.length >= 9 && !parts[8].isEmpty()) {
                createdAt = parts[8];
            }
            return new GameRoom(roomId, gameType, entryFee, capacity, poolAmount, status, participants, winnerId, createdAt);
        }
        return null;
    }
    @Override
    protected String mapToString(GameRoom room) {
        return room.getRoomId() + "," +
               room.getGameType().name() + "," +
               room.getEntryFee() + "," +
               room.getCapacity() + "," +
               room.getPoolAmount() + "," +
               room.getStatus().name() + "," +
               room.getParticipantsAsString() + "," +
               (room.getWinnerId() != null ? room.getWinnerId() : "") + "," +
               room.getCreatedAt();
    }
    @Override
    protected Integer getId(GameRoom room) {
        return room.getRoomId();
    }
    @Override
    public List<GameRoom> findByUserId(int userId) {
        return cache.values().stream()
                .filter(room -> room.getParticipants().stream()
                        .anyMatch(p -> p.getUserId() == userId))
                .collect(java.util.stream.Collectors.toList());
    }
}
