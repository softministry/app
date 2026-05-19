package ro.church_office.info.events.DAO;

import org.springframework.stereotype.Service;
import ro.church_office.info.church.ChurchContextService;
import ro.church_office.info.events.EventDTO;
import ro.church_office.info.events.EventMigrationDTO;
import ro.church_office.info.events.EventTaskDTO;

import java.util.List;

@Service
public class InMemoryEventService implements EventService {
    private final EventRepository eventRepository;
    private final ChurchContextService churchContextService;

    public InMemoryEventService(EventRepository eventRepository,
                                ChurchContextService churchContextService) {
        this.eventRepository = eventRepository;
        this.churchContextService = churchContextService;
    }

    @Override
    public List<EventDTO> getAllEvents() {
        return eventRepository.findAll().stream().map(EventDTO::fromEntity).toList();
    }

    @Override
    public Event saveEvent(EventDTO dto) {
        Event event = dto.toEntity();
        if (event.getChurchId() == null) {
            event.setChurchId(churchContextService.getOrCreateActiveChurchId());
        }
        return eventRepository.save(event);
    }

    @Override
    public void updateEvent(Event event) {
        if (event.getChurchId() == null) {
            event.setChurchId(churchContextService.getOrCreateActiveChurchId());
        }
        eventRepository.save(event);
    }

    @Override
    public void deleteEventById(Long id) {
        eventRepository.deleteById(id);
    }

    @Override
    public void moveEvent(Long id, EventMigrationDTO dto) {
        if (id == null || dto == null || dto.getTargetChurchId() == null) {
            return;
        }
        eventRepository.findById(id).ifPresent(event -> {
            event.setChurchId(dto.getTargetChurchId());
            if (Boolean.TRUE.equals(dto.getClearImplementedBy())) {
                event.setImplementedBy(null);
            }
            eventRepository.save(event);
        });
    }

    @Override
    public EventTaskDTO saveTask(Long eventId, EventTaskDTO dto, Long churchId) {
        return dto;
    }
}
