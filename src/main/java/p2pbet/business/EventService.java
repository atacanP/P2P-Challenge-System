package p2pbet.business;
import p2pbet.core.entities.Event;
import p2pbet.dataaccess.IEventRepository;
import org.springframework.stereotype.Service;
@Service
public class EventService {
    private final IEventRepository eventRepository;
    public EventService(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    public java.util.List<Event> getAllEvents() {
        return eventRepository.findAll();
    }
    public java.util.List<Event> getAvailableEvents() {
        return eventRepository.findAll().stream()
                .filter(e -> e.getStatus() == Event.EventStatus.UPCOMING)
                .collect(java.util.stream.Collectors.toList());
    }
    public Event getEventById(int id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));
    }
    public Event createEvent(String description, String optionA, String optionB) {
        int id = Math.abs(java.util.UUID.randomUUID().hashCode() % 1000000);
        while(eventRepository.findById(id).isPresent()) {
            id = Math.abs(java.util.UUID.randomUUID().hashCode() % 1000000);
        }
        Event event = new Event(id, description, optionA, optionB, Event.EventStatus.UPCOMING);
        eventRepository.save(event);
        return event;
    }
    public void startEvent(int eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        event.startEvent();
        eventRepository.save(event);
        System.out.println("Event " + eventId + " started. Betting window closed.");
    }
    public void registerResult(int eventId, String result) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        event.finishEvent(result);
        eventRepository.save(event);
        System.out.println("Event " + eventId + " finished with result: " + result);
    }
}
