package ro.church_office.info.visits;

import org.springframework.stereotype.Service;
import ro.church_office.info.visits.dao.Visit;
import ro.church_office.info.visits.dao.VisitRepository;

import java.util.List;

@Service
public class InMemoryVisitService implements VisitService {
    private final VisitRepository visitRepository;

    public InMemoryVisitService(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    @Override
    public List<Visit> getAllVisits(Long churchId) {
        return visitRepository.findAllByChurchIdOrderByVisitDateDescIdDesc(churchId);
    }

    @Override
    public Visit saveVisit(Visit visit){
        return visitRepository.save(visit);
    }

    @Override
    public Visit getVisitById(Long id, Long churchId){
        return visitRepository.findByIdAndChurchId(id, churchId).orElse(null);
    }

    @Override
    public void deleteVisit(Long id, Long churchId) {
        visitRepository.findByIdAndChurchId(id, churchId).ifPresent(visitRepository::delete);
    }
}
