package ro.church_office.info.visits;

import ro.church_office.info.visits.dao.Visit;

import java.util.List;
import java.util.Optional;

public interface VisitService {
    default List<VisitDTO> findAll(Long churchId) { return List.of(); }
    default VisitDTO save(VisitDTO dto, Long churchId) { return dto; }
    default void delete(Long id, Long churchId) {}
    default List<Visit> getAllVisits(Long churchId){ return List.of(); }
    default Visit saveVisit(Visit visit){ return visit; }
    default Visit getVisitById(Long id, Long churchId){ return null; }
    default void deleteVisit(Long id, Long churchId) {}
}
