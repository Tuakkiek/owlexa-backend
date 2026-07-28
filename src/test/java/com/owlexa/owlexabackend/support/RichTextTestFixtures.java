package com.owlexa.owlexabackend.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class RichTextTestFixtures {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RichTextTestFixtures() {
    }

    public static JsonNode document(String value) {
        ObjectNode document = OBJECT_MAPPER.createObjectNode();
        document.put("type", "doc");
        ObjectNode paragraph = document.putArray("content").addObject();
        paragraph.put("type", "paragraph");
        if (value != null && !value.isEmpty()) {
            paragraph.putArray("content").addObject()
                    .put("type", "text")
                    .put("text", value);
        }
        return document;
    }

    public static String serializedDocument(String value) {
        return OBJECT_MAPPER.writeValueAsString(document(value));
    }
}
