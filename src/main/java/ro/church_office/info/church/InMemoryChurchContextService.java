package ro.church_office.info.church;

import org.springframework.stereotype.Service;

@Service
public class InMemoryChurchContextService implements ChurchContextService {

    private final ChurchInfoService churchInfoService;

    public InMemoryChurchContextService(ChurchInfoService churchInfoService) {
        this.churchInfoService = churchInfoService;
    }

    @Override
    public Long currentChurchId() {
        return resolveChurchId();
    }

    @Override
    public Long getOrCreateActiveChurchId() {
        return resolveChurchId();
    }

    @Override
    public void setActiveChurchId(Long churchId) {
        if (churchId != null) {
            churchInfoService.setDefaultChurchId(churchId);
        }
    }

    private Long resolveChurchId() {
        Long churchId = churchInfoService.getDefaultChurchId();
        return churchId == null ? 1L : churchId;
    }
}
