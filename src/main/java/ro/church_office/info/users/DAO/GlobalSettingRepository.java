package ro.church_office.info.users.DAO;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GlobalSettingRepository extends JpaRepository<GlobalSetting, Long> {
    Optional<GlobalSetting> findBySettingKey(String settingKey);
    default Optional<GlobalSetting> findByKey(String settingKey) { return findBySettingKey(settingKey); }
}
