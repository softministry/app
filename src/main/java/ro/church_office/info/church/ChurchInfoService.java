package ro.church_office.info.church;

import ro.church_office.info.church.DTO.ChurchInfoDTO;

import java.util.List;
import java.util.Optional;

public interface ChurchInfoService {
    default Optional<ChurchInfoDTO> get() { return Optional.empty(); }
    default ChurchInfoDTO save(ChurchInfoDTO dto) { return dto; }
    default ChurchInfoDTO create(ChurchInfoDTO dto) { return dto; }
    default Long getDefaultChurchId() { return 1L; }
    default void setDefaultChurchId(Long churchId) {}
    default List<ChurchInfoDTO> getAll(){ return List.of(); }
}
