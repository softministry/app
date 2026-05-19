package ro.church_office.info.events.DAO;

import ro.church_office.info.events.EventDTO;
import ro.church_office.info.events.EventMigrationDTO;
import ro.church_office.info.events.EventTaskDTO;

import java.util.List;

public interface EventService {
    default List<EventDTO> findAll(Long churchId) { return List.of(); }
    default List<EventDTO> getAllEvents() { return List.of(); }
    default EventDTO save(EventDTO dto, Long churchId) { return dto; }
    default Event saveEvent(EventDTO dto) { Event e = new Event(); e.setId(dto==null?null:dto.getId()); return e; }
    default void updateEvent(Event event) {}
    default void delete(Long id, Long churchId) {}
    default void deleteEventById(Long id) {}
    default void migrate(Long id, EventMigrationDTO dto, Long churchId) {}
    default void moveEvent(Long id, EventMigrationDTO dto) {}
    default EventTaskDTO saveTask(Long eventId, EventTaskDTO dto, Long churchId) { return dto; }
}
