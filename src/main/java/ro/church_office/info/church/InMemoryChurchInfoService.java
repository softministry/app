package ro.church_office.info.church;

import org.springframework.stereotype.Service;
import ro.church_office.info.church.DTO.ChurchInfoDTO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InMemoryChurchInfoService implements ChurchInfoService {

    private final AtomicLong defaultChurchId = new AtomicLong(1L);
    private final ChurchInfoDTO churchInfo;

    public InMemoryChurchInfoService() {
        ChurchInfoDTO dto = new ChurchInfoDTO();
        dto.setId(1L);
        dto.setName("Default Church");
        this.churchInfo = dto;
    }

    @Override
    public Optional<ChurchInfoDTO> get() {
        return Optional.of(churchInfo);
    }

    @Override
    public ChurchInfoDTO save(ChurchInfoDTO dto) {
        if (dto != null) {
            if (dto.getId() != null) {
                churchInfo.setId(dto.getId());
                defaultChurchId.set(dto.getId());
            }
            churchInfo.setName(dto.getName());
            churchInfo.avatarUrl = dto.avatarUrl;
            churchInfo.address = dto.address;
            churchInfo.pastorName = dto.pastorName;
            churchInfo.pastorPhone = dto.pastorPhone;
            churchInfo.secretaryName = dto.secretaryName;
            churchInfo.secretaryPhone = dto.secretaryPhone;
            churchInfo.treasurerName = dto.treasurerName;
            churchInfo.treasurerPhone = dto.treasurerPhone;
        }
        return churchInfo;
    }

    @Override
    public ChurchInfoDTO create(ChurchInfoDTO dto) {
        return save(dto);
    }

    @Override
    public Long getDefaultChurchId() {
        return defaultChurchId.get();
    }

    @Override
    public void setDefaultChurchId(Long churchId) {
        if (churchId != null) {
            defaultChurchId.set(churchId);
            churchInfo.setId(churchId);
        }
    }

    @Override
    public List<ChurchInfoDTO> getAll() {
        return List.of(churchInfo);
    }
}
