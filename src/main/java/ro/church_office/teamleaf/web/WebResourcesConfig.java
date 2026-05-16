package ro.church_office.teamleaf.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebResourcesConfig implements WebMvcConfigurer {

    @Value("${ministryadmin.web.i18n.external-dir:../church-office-frontend/src/assets/i18n}")
    private String i18nExternalDir;

    @Value("${ministryadmin.desktop.uploads-dir:uploads}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path i18nPath = Paths.get(i18nExternalDir).toAbsolutePath().normalize();
        if (Files.isDirectory(i18nPath)) {
            registry.addResourceHandler("/i18n/**")
                    .addResourceLocations(i18nPath.toUri().toString());
        }

        Path uploadsPath = Paths.get(uploadsDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsPath.toUri().toString());
    }
}
