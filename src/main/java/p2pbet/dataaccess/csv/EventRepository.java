package p2pbet.dataaccess.csv;
import p2pbet.core.entities.Event;
import p2pbet.core.entities.Event.EventStatus;
import p2pbet.dataaccess.IEventRepository;
import p2pbet.dataaccess.IDataStorage;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
@Repository
public class EventRepository extends AbstractCsvRepository<Event, Integer> implements IEventRepository {
    public EventRepository(IDataStorage dataStorage) {
        super(dataStorage, "events.csv", "eventId,description,optionA,optionB,finalResult,status,createdAt");
    }
    @Override
    protected Event mapFromString(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length >= 7) {
            int id = Integer.parseInt(parts[0]);
            String description = parts[1];
            String optionA = parts[2];
            String optionB = parts[3];
            String finalResult = parts[4].equals("null") ? null : parts[4];
            EventStatus status = EventStatus.valueOf(parts[5]);
            String createdAt = parts[6];
            Event event = new Event(id, description, optionA, optionB, finalResult, status, createdAt);
            return event;
        }
        return null;
    }
    @Override
    protected String mapToString(Event event) {
        return event.getId() + "," +
               event.getDescription() + "," +
               event.getOptionA() + "," +
               event.getOptionB() + "," +
               (event.getFinalResult() == null ? "null" : event.getFinalResult()) + "," +
               event.getStatus().name() + "," +
               event.getCreatedAt();
    }
    @Override
    protected Integer getId(Event event) {
        return event.getId();
    }
    @Override
    public List<Event> findByStatus(EventStatus status) {
        return cache.values().stream()
                .filter(e -> e.getStatus() == status)
                .collect(Collectors.toList());
    }
}
