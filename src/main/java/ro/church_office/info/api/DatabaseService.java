package ro.church_office.info.api;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public abstract class DatabaseService {
    public abstract InputStream exportDatabase(boolean fresh) throws IOException;
    public abstract void importDatabase(MultipartFile file) throws IOException;
}
