package com.owlexa.owlexabackend.modules.file.storage;

import com.owlexa.owlexabackend.modules.file.entity.StorageProvider;

public record StoredObject(String path, String url, StorageProvider provider) {
}
