package ro.church_office.info.events.DAO;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventTaskRepository extends JpaRepository<EventTask, Long> {
    default List<EventTask> findByEventIdOrderByOrderIndexAscIdAsc(Long eventId){ return List.of(); }
}
