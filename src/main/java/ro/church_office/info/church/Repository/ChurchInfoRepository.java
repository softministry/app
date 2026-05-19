package ro.church_office.info.church.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.church_office.info.church.DAO.ChurchInfo;

import java.util.Optional;

public interface ChurchInfoRepository extends JpaRepository<ChurchInfo, Long> {
    default Optional<ChurchInfo> findByNameIgnoreCase(String name){ return Optional.empty(); }
}
