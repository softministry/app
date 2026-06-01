package ro.church_office.info.church;

import org.springframework.stereotype.Service;
import ro.church_office.info.church.DAO.ChurchInfo;
import ro.church_office.info.church.DTO.ChurchInfoDTO;
import ro.church_office.info.church.Repository.ChurchInfoRepository;
import ro.church_office.info.users.DAO.GlobalSetting;
import ro.church_office.info.users.DAO.GlobalSettingRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class InMemoryChurchInfoService implements ChurchInfoService {

    private static final String DEFAULT_CHURCH_ID_KEY = "default_church_id";
    private final ChurchInfoRepository churchInfoRepository;
    private final GlobalSettingRepository globalSettingRepository;

    public InMemoryChurchInfoService(ChurchInfoRepository churchInfoRepository,
                                     GlobalSettingRepository globalSettingRepository) {
        this.churchInfoRepository = churchInfoRepository;
        this.globalSettingRepository = globalSettingRepository;
    }

    @Override
    public Optional<ChurchInfoDTO> get() {
        Long id = getDefaultChurchId();
        if (id != null) {
            Optional<ChurchInfoDTO> selected = churchInfoRepository.findById(id).map(this::toDto);
            if (selected.isPresent()) {
                return selected;
            }
        }
        return churchInfoRepository.findAll().stream()
                .sorted(Comparator.comparing(ChurchInfo::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toDto)
                .findFirst();
    }

    @Override
    public ChurchInfoDTO save(ChurchInfoDTO dto) {
        if (dto == null) {
            return get().orElseGet(() -> {
                ChurchInfoDTO fallback = new ChurchInfoDTO();
                fallback.id = 1L;
                fallback.name = "Default Church";
                return fallback;
            });
        }
        ChurchInfo entity = dto.getId() == null
                ? new ChurchInfo()
                : churchInfoRepository.findById(dto.getId()).orElseGet(() -> {
                    ChurchInfo created = new ChurchInfo();
                    created.setId(dto.getId());
                    return created;
                });
        String name = dto.getName() == null || dto.getName().isBlank() ? "Default Church" : dto.getName().trim();
        entity.setName(name);
        entity.setAvatarUrl(dto.avatarUrl);
        entity.setAddress(dto.address);
        entity.setPastorName(dto.pastorName);
        entity.setPastorPhone(dto.pastorPhone);
        entity.setSecretaryName(dto.secretaryName);
        entity.setSecretaryPhone(dto.secretaryPhone);
        entity.setTreasurerName(dto.treasurerName);
        entity.setTreasurerPhone(dto.treasurerPhone);
        ChurchInfo saved = churchInfoRepository.save(entity);
        if (dto.getId() != null && dto.getId().equals(getDefaultChurchId())) {
            setDefaultChurchId(saved.getId());
        }
        return toDto(saved);
    }

    @Override
    public ChurchInfoDTO create(ChurchInfoDTO dto) {
        return save(dto);
    }

    @Override
    public Long getDefaultChurchId() {
        Long fromSettings = globalSettingRepository.findBySettingKey(DEFAULT_CHURCH_ID_KEY)
                .map(GlobalSetting::getSettingValue)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    try {
                        return Long.valueOf(value);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .orElse(null);
        if (fromSettings != null && churchInfoRepository.existsById(fromSettings)) {
            return fromSettings;
        }
        return churchInfoRepository.findAll().stream()
                .map(ChurchInfo::getId)
                .filter(id -> id != null)
                .min(Long::compareTo)
                .orElse(1L);
    }

    @Override
    public void setDefaultChurchId(Long churchId) {
        if (churchId == null || !churchInfoRepository.existsById(churchId)) {
            return;
        }
        GlobalSetting setting = globalSettingRepository.findBySettingKey(DEFAULT_CHURCH_ID_KEY)
                .orElseGet(() -> new GlobalSetting(DEFAULT_CHURCH_ID_KEY, String.valueOf(churchId)));
        setting.setSettingValue(String.valueOf(churchId));
        globalSettingRepository.save(setting);
    }

    @Override
    public List<ChurchInfoDTO> getAll() {
        return churchInfoRepository.findAll().stream()
                .sorted(Comparator.comparing(ChurchInfo::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toDto)
                .toList();
    }

    private ChurchInfoDTO toDto(ChurchInfo entity) {
        ChurchInfoDTO dto = new ChurchInfoDTO();
        dto.id = entity.getId();
        dto.name = entity.getName();
        dto.avatarUrl = entity.getAvatarUrl();
        dto.address = entity.getAddress();
        dto.pastorName = entity.getPastorName();
        dto.pastorPhone = entity.getPastorPhone();
        dto.secretaryName = entity.getSecretaryName();
        dto.secretaryPhone = entity.getSecretaryPhone();
        dto.treasurerName = entity.getTreasurerName();
        dto.treasurerPhone = entity.getTreasurerPhone();
        return dto;
    }
}
