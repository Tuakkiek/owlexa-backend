package com.owlexa.owlexabackend.modules.file.storage;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorage {

    StoredObject store(String key, InputStream content, long contentLength, String contentType) throws IOException;

    void delete(String path) throws IOException;
}
