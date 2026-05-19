package ro.church_office.info.church;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class InMemoryChurchContextService implements ChurchContextService {

    private final AtomicLong activeChurchId = new AtomicLong(1L);

    @Override
    public Long currentChurchId() {
        return activeChurchId.get();
    }

    @Override
    public Long getOrCreateActiveChurchId() {
        return activeChurchId.get();
    }

    @Override
    public void setActiveChurchId(Long churchId) {
        if (churchId != null) {
            activeChurchId.set(churchId);
        }
    }
}
