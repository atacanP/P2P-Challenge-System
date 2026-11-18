package p2pbet.dataaccess;
import p2pbet.core.entities.Event;
import p2pbet.core.entities.Event.EventStatus;
import java.util.List;
public interface IEventRepository extends IRepository<Event, Integer> {
    List<Event> findByStatus(EventStatus status);
}
