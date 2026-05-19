package ro.church_office.info.events.DAO;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventTaskRepository extends JpaRepository<EventTask, Long> {
    default List<EventTask> findByEventIdOrderByOrderIndexAscIdAsc(Long eventId){
        return findAll().stream()
                .filter(task -> task.getEvent() != null && eventId != null && eventId.equals(task.getEvent().getId()))
                .sorted(java.util.Comparator
                        .comparing(EventTask::getOrderIndex, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                        .thenComparing(EventTask::getId, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
    }
}
