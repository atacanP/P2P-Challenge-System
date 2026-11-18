package p2pbet.dataaccess;
import p2pbet.core.entities.GameRoom;
import java.util.List;
public interface IGameRoomRepository extends IRepository<GameRoom, Integer> {
    List<GameRoom> findByUserId(int userId);
}
