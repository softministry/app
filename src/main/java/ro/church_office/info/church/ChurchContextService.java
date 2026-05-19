package ro.church_office.info.church;

public interface ChurchContextService {
    default Long currentChurchId() { return 1L; }
    default Long getOrCreateActiveChurchId() { return 1L; }
    default void setActiveChurchId(Long churchId) {}
}
