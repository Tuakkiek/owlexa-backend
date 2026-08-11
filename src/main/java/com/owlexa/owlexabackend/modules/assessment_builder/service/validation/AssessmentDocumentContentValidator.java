package com.owlexa.owlexabackend.modules.assessment_builder.service.validation;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AssessmentDocumentContentValidator {

    private static final int MAX_DEPTH = 32;
    private static final int MAX_NODES = 10_000;
    private static final int MAX_TOTAL_TEXT_LENGTH = 200_000;
    private static final int MAX_LINK_LENGTH = 2_048;
    private static final int MAX_REL_LENGTH = 512;
    private static final Set<String> ALLOWED_NODE_TYPES = Set.of(
            "doc",
            "paragraph",
            "heading",
            "text",
            "bulletList",
            "orderedList",
            "listItem",
            "hardBreak",
            "assessmentQuestion",
            "taskList",
            "taskItem"
    );
    private static final Set<String> ALLOWED_MARK_TYPES = Set.of("bold", "italic", "underline", "link", "highlight", "textStyle", "color");
    private static final Set<String> ALLOWED_TEXT_ALIGN_VALUES = Set.of("left", "center", "right", "justify");
    private static final Set<String> ALLOWED_TARGET_VALUES = Set.of("_self", "_blank");

    private final ObjectMapper objectMapper;

    public String serializeRichText(JsonNode content, int blockIndex) {
        validateRichText(content, blockIndex);
        try {
            return objectMapper.writeValueAsString(content);
        } catch (RuntimeException exception) {
            throw new BadRequestException("Block " + blockIndex + " content is not serializable");
        }
    }

    public void validateRichText(JsonNode content, int blockIndex) {
        if (content == null || content.isNull()) {
            throw new BadRequestException("Block " + blockIndex + " content is required");
        }
        ValidationContext context = new ValidationContext();
        validateNode(content, blockIndex, "content", true, 1, context);
    }

    public void validateSerializedRichText(String serialized, int blockIndex) {
        if (serialized == null || serialized.isBlank()) {
            throw new BadRequestException("Block " + blockIndex + " content is required");
        }
        try {
            validateRichText(objectMapper.readTree(serialized), blockIndex);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BadRequestException("Block " + blockIndex + " content is invalid");
        }
    }

    private void validateNode(
            JsonNode node,
            int blockIndex,
            String path,
            boolean root,
            int depth,
            ValidationContext context
    ) {
        if (depth > MAX_DEPTH) {
            throw new BadRequestException("Block " + blockIndex + " content exceeds maximum nesting depth");
        }
        context.nodeCount++;
        if (context.nodeCount > MAX_NODES) {
            throw new BadRequestException("Block " + blockIndex + " content contains too many nodes");
        }
        if (node == null || !node.isObject()) {
            throw new BadRequestException("Block " + blockIndex + " " + path + " must be an object");
        }
        String type = node.path("type").asText(null);
        if (type == null || !ALLOWED_NODE_TYPES.contains(type)) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".type is not supported: " + type);
        }
        if (root && !"doc".equals(type)) {
            throw new BadRequestException("Block " + blockIndex + " content must be a ProseMirror doc");
        }
        if (!root && "doc".equals(type)) {
            throw new BadRequestException("Block " + blockIndex + " nested doc nodes are not allowed");
        }

        validateAllowedNodeProperties(node, type, blockIndex, path);
        validateNodeAttributes(node, type, blockIndex, path);
        validateMarks(node, type, blockIndex, path);
        validateText(node, type, blockIndex, path, context);
        validateChildren(node, type, blockIndex, path, depth, context);
    }

    private void validateAllowedNodeProperties(JsonNode node, String type, int blockIndex, String path) {
        Set<String> allowedProperties = switch (type) {
            case "doc" -> Set.of("type", "content");
            case "text" -> Set.of("type", "text", "marks");
            case "hardBreak" -> Set.of("type");
            default -> Set.of("type", "attrs", "content");
        };
        for (String propertyName : node.propertyNames()) {
            if (!allowedProperties.contains(propertyName)) {
                throw new BadRequestException("Block " + blockIndex + " " + path + "." + propertyName + " is not supported");
            }
        }
    }

    private void validateNodeAttributes(JsonNode node, String type, int blockIndex, String path) {
        JsonNode attrs = node.path("attrs");
        if ("assessmentQuestion".equals(type)) {
            if (attrs.isMissingNode() || !attrs.isObject()) {
                throw new BadRequestException("Block " + blockIndex + " " + path + ".attrs is required for assessmentQuestion");
            }
            JsonNode questionId = attrs.path("questionId");
            if (!questionId.isNumber()) {
                throw new BadRequestException("Block " + blockIndex + " " + path + ".attrs.questionId is required and must be a number");
            }
            return;
        }

        if (attrs.isMissingNode() || attrs.isNull()) {
            return;
        }
        if (!attrs.isObject()) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".attrs must be an object");
        }

        Set<String> allowedAttributes = switch (type) {
            case "paragraph" -> Set.of("textAlign");
            case "heading" -> Set.of("level", "textAlign");
            case "orderedList" -> Set.of("order");
            case "image" -> Set.of("fileId", "src", "alt", "title", "width", "height", "alignment");
            case "audio", "video", "pdfAttachment", "fileAttachment" -> Set.of("fileId", "src", "originalName", "mimeType", "size");
            case "tableCell", "tableHeader" -> Set.of("colspan", "rowspan", "colwidth", "background");
            case "taskItem" -> Set.of("checked");
            default -> Set.of();
        };
        for (String attributeName : attrs.propertyNames()) {
            if (!allowedAttributes.contains(attributeName)) {
                throw new BadRequestException("Block " + blockIndex + " " + path + ".attrs." + attributeName + " is not supported");
            }
        }

        JsonNode level = attrs.path("level");
        if (!level.isMissingNode() && (!level.canConvertToInt() || level.asInt() < 1 || level.asInt() > 6)) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".attrs.level must be between 1 and 6");
        }

        JsonNode order = attrs.path("order");
        if (!order.isMissingNode() && (!order.canConvertToInt() || order.asInt() < 1)) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".attrs.order must be greater than zero");
        }

        JsonNode textAlign = attrs.path("textAlign");
        if (!textAlign.isMissingNode()
                && (!textAlign.isTextual() || !ALLOWED_TEXT_ALIGN_VALUES.contains(textAlign.asText()))) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".attrs.textAlign is not supported");
        }
    }

    private void validateMarks(JsonNode node, String type, int blockIndex, String path) {
        JsonNode marks = node.path("marks");
        if (marks.isMissingNode() || marks.isNull()) {
            return;
        }
        if (!"text".equals(type)) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".marks is not supported");
        }
        if (!marks.isArray()) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".marks must be an array");
        }

        for (JsonNode mark : marks) {
            validateMark(mark, blockIndex, path);
        }
    }

    private void validateMark(JsonNode mark, int blockIndex, String path) {
        if (mark == null || !mark.isObject()) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".marks[] must be an object");
        }
        String markType = mark.path("type").asText(null);
        if (markType == null || !ALLOWED_MARK_TYPES.contains(markType)) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".marks[].type is not supported");
        }
        Set<String> allowedProperties = "link".equals(markType) ? Set.of("type", "attrs") : Set.of("type");
        for (String propertyName : mark.propertyNames()) {
            if (!allowedProperties.contains(propertyName)) {
                throw new BadRequestException("Block " + blockIndex + " " + path + ".marks[]." + propertyName + " is not supported");
            }
        }

        JsonNode attrs = mark.path("attrs");
        if (!"link".equals(markType)) {
            if (!attrs.isMissingNode() && !attrs.isNull()) {
                throw new BadRequestException("Block " + blockIndex + " " + path + ".marks[].attrs is not supported");
            }
            return;
        }
        if (!attrs.isObject()) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".marks[].attrs is required");
        }
        for (String attributeName : attrs.propertyNames()) {
            if (!Set.of("href", "target", "rel").contains(attributeName)) {
                throw new BadRequestException("Block " + blockIndex + " " + path + ".marks[].attrs." + attributeName + " is not supported");
            }
        }
        JsonNode href = attrs.path("href");
        if (!href.isTextual() || !isAllowedHref(href.asText())) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".marks[].attrs.href is invalid");
        }

        JsonNode target = attrs.path("target");
        if (!target.isMissingNode() && !target.isNull()
                && (!target.isTextual() || !ALLOWED_TARGET_VALUES.contains(target.asText()))) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".marks[].attrs.target is invalid");
        }

        JsonNode rel = attrs.path("rel");
        if (!rel.isMissingNode() && !rel.isNull()
                && (!rel.isTextual() || rel.asText().length() > MAX_REL_LENGTH || containsControlCharacter(rel.asText()))) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".marks[].attrs.rel is invalid");
        }
    }

    private boolean isAllowedHref(String href) {
        String normalized = href.strip();
        if (normalized.isBlank()
                || normalized.length() > MAX_LINK_LENGTH
                || containsControlCharacter(normalized)
                || normalized.startsWith("//")) {
            return false;
        }

        String lowercase = normalized.toLowerCase(Locale.ROOT);
        if (lowercase.startsWith("http://")
                || lowercase.startsWith("https://")
                || lowercase.startsWith("mailto:")
                || normalized.startsWith("/")
                || normalized.startsWith("#")) {
            return true;
        }

        int colonIndex = normalized.indexOf(':');
        return colonIndex < 0;
    }

    private boolean containsControlCharacter(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private void validateText(JsonNode node, String type, int blockIndex, String path, ValidationContext context) {
        JsonNode text = node.path("text");
        if ("text".equals(type)) {
            if (!text.isTextual()) {
                throw new BadRequestException("Block " + blockIndex + " " + path + ".text is required");
            }
            context.totalTextLength += text.asText().length();
            if (context.totalTextLength > MAX_TOTAL_TEXT_LENGTH) {
                throw new BadRequestException("Block " + blockIndex + " content text is too large");
            }
            return;
        }
        if (!text.isMissingNode()) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".text is not supported");
        }
    }

    private void validateChildren(
            JsonNode node,
            String type,
            int blockIndex,
            String path,
            int depth,
            ValidationContext context
    ) {
        JsonNode content = node.path("content");
        if (content.isMissingNode() || content.isNull()) {
            if ("doc".equals(type)) {
                throw new BadRequestException("Block " + blockIndex + " content.content is required");
            }
            return;
        }
        if (!content.isArray()) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".content must be an array");
        }
        if ("text".equals(type) || "hardBreak".equals(type)) {
            throw new BadRequestException("Block " + blockIndex + " " + path + ".content is not supported");
        }

        int childIndex = 0;
        for (JsonNode child : content) {
            childIndex++;
            validateNode(child, blockIndex, path + ".content[" + childIndex + "]", false, depth + 1, context);
        }
    }

    private static class ValidationContext {
        private int nodeCount;
        private int totalTextLength;
    }
}
