package ro.church_office.info.api;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public abstract class DatabaseService {
    public abstract InputStream exportDatabase(boolean fresh) throws IOException;
    public abstract void importDatabase(MultipartFile file) throws IOException;

    public void resetApplicationData() throws IOException {
        throw new IOException("Resetarea datelor nu este suportată pentru această bază de date.");
    }
}
