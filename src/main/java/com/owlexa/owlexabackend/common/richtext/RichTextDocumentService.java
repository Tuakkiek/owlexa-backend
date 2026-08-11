package com.owlexa.owlexabackend.common.richtext;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Set;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class RichTextDocumentService {

    private static final int MAX_DOCUMENT_CHARACTERS = 5_000_000;
    private static final Set<String> MEANINGFUL_ATOM_NODES = Set.of(
            "image", "audio", "video", "pdfAttachment", "fileAttachment", "table"
    );

    private final ObjectMapper objectMapper;

    public JsonNode normalize(JsonNode content) {
        JsonNode document = content == null || content.isNull() ? emptyDocument() : content;
        validate(document);
        return document;
    }

    public JsonNode normalize(JsonNode content, String legacyPlainText) {
        JsonNode document = content == null || content.isNull()
                ? fromPlainText(legacyPlainText)
                : content;
        validate(document);
        return document;
    }

    public JsonNode normalizeOptional(JsonNode content) {
        if (content == null || content.isNull()) {
            return null;
        }
        validate(content);
        return hasMeaningfulContent(content) ? content : null;
    }

    public JsonNode deserialize(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return emptyDocument();
        }
        try {
            JsonNode document = objectMapper.readTree(serialized);
            validate(document);
            return document;
        } catch (RuntimeException exception) {
            throw new BadRequestException("Stored editor content is invalid");
        }
    }

    public JsonNode deserializeOptional(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return null;
        }
        return deserialize(serialized);
    }

    public String serialize(JsonNode document) {
        validate(document);
        String serialized = objectMapper.writeValueAsString(document);
        if (serialized.length() > MAX_DOCUMENT_CHARACTERS) {
            throw new BadRequestException("Editor content is too large");
        }
        return serialized;
    }

    public String serializeOptional(JsonNode document) {
        return document == null || document.isNull() ? null : serialize(document);
    }

    public String toPlainText(JsonNode document) {
        if (document == null || document.isNull()) {
            return "";
        }
        validate(document);
        StringJoiner text = new StringJoiner(" ");
        collectText(document, text);
        return text.toString().replaceAll("\\s+", " ").trim();
    }

    public boolean hasMeaningfulContent(JsonNode document) {
        if (document == null || document.isNull()) {
            return false;
        }
        validate(document);
        return hasMeaningfulNode(document);
    }

    public JsonNode emptyDocument() {
        ObjectNode document = objectMapper.createObjectNode();
        document.put("type", "doc");
        ArrayNode content = document.putArray("content");
        content.addObject().put("type", "paragraph");
        return document;
    }

    private JsonNode fromPlainText(String value) {
        if (value == null || value.isBlank()) {
            return emptyDocument();
        }
        ObjectNode document = objectMapper.createObjectNode();
        document.put("type", "doc");
        ObjectNode paragraph = document.putArray("content").addObject();
        paragraph.put("type", "paragraph");
        paragraph.putArray("content").addObject()
                .put("type", "text")
                .put("text", value.trim());
        return document;
    }

    private void validate(JsonNode document) {
        if (document == null
                || !document.isObject()
                || !"doc".equals(document.path("type").asText())
                || (!document.path("content").isMissingNode() && !document.path("content").isArray())) {
            throw new BadRequestException("Editor content must be a valid ProseMirror document");
        }
    }

    private void collectText(JsonNode node, StringJoiner text) {
        if ("text".equals(node.path("type").asText()) && node.path("text").isTextual()) {
            text.add(node.path("text").asText());
        }
        JsonNode content = node.path("content");
        if (content.isArray()) {
            for (JsonNode child : content) {
                collectText(child, text);
            }
        }
    }

    private boolean hasMeaningfulNode(JsonNode node) {
        String type = node.path("type").asText();
        if ("text".equals(type) && node.path("text").asText("").strip().length() > 0) {
            return true;
        }
        if (MEANINGFUL_ATOM_NODES.contains(type)) {
            return true;
        }
        JsonNode content = node.path("content");
        if (content.isArray()) {
            for (JsonNode child : content) {
                if (hasMeaningfulNode(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
