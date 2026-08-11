package com.owlexa.owlexabackend.modules.assessment_builder.service.validation;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssessmentDocumentContentValidatorTest {

    private ObjectMapper objectMapper;
    private AssessmentDocumentContentValidator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new AssessmentDocumentContentValidator(objectMapper);
    }

    @Test
    void acceptsSupportedProseMirrorNodesMarksAndAttributes() {
        JsonNode content = read("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "heading",
                      "attrs": {"level": 2, "textAlign": "center"},
                      "content": [
                        {
                          "type": "text",
                          "text": "Title",
                          "marks": [
                            {"type": "bold"},
                            {"type": "italic"},
                            {"type": "underline"}
                          ]
                        }
                      ]
                    },
                    {
                      "type": "paragraph",
                      "attrs": {"textAlign": "justify"},
                      "content": [
                        {
                          "type": "text",
                          "text": "OpenAI",
                          "marks": [
                            {
                              "type": "link",
                              "attrs": {
                                "href": "https://example.com",
                                "target": "_blank",
                                "rel": "noopener"
                              }
                            }
                          ]
                        },
                        {"type": "hardBreak"}
                      ]
                    },
                    {
                      "type": "bulletList",
                      "content": [
                        {
                          "type": "listItem",
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [{"type": "text", "text": "Bullet"}]
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "type": "orderedList",
                      "attrs": {"order": 1},
                      "content": [
                        {
                          "type": "listItem",
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [{"type": "text", "text": "Ordered"}]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """);

        String serialized = validator.serializeRichText(content, 1);

        assertThat(serialized).contains("\"heading\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://example.com",
            "mailto:test@example.com",
            "/local/path",
            "#section",
            "example.com/path",
            "../relative/path",
            "?query=value"
    })
    void acceptsAllowedLinks(String href) {
        validator.validateRichText(linkDoc(href, "_self", "noopener"), 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "image",
            "audio",
            "table",
            "unknown"
    })
    void rejectsUnsupportedNodeTypes(String type) {
        JsonNode content = read("""
                {
                  "type": "doc",
                  "content": [
                    {"type": "%s"}
                  ]
                }
                """.formatted(type));

        assertThatThrownBy(() -> validator.validateRichText(content, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("type is not supported");
    }

    @Test
    void rejectsUnknownMark() {
        JsonNode content = read("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        {
                          "type": "text",
                          "text": "x",
                          "marks": [{"type": "strike"}]
                        }
                      ]
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validateRichText(content, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("marks[].type is not supported");
    }

    @Test
    void rejectsNestedDoc() {
        JsonNode content = read("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "doc",
                      "content": []
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validateRichText(content, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("nested doc nodes are not allowed");
    }

    @Test
    void rejectsUnsupportedAttributes() {
        JsonNode content = read("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "attrs": {"class": "danger"}
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validateRichText(content, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("attrs.class is not supported");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "//example.com",
            " JAVASCRIPT:alert(1)",
            "data:text/plain,test",
            "ftp://example.com"
    })
    void rejectsDisallowedUrlSchemes(String href) {
        assertThatThrownBy(() -> validator.validateRichText(linkDoc(href, null, null), 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("href is invalid");
    }

    @Test
    void rejectsVbscriptUrlScheme() {
        assertThatThrownBy(() -> validator.validateRichText(linkDoc("vbscript:msgbox(1)", null, null), 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("href is invalid");
    }

    @Test
    void rejectsControlCharacterUrl() {
        assertThatThrownBy(() -> validator.validateRichText(linkDoc("java\u0000script:alert(1)", null, null), 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("href is invalid");
    }

    @Test
    void rejectsInvalidTarget() {
        assertThatThrownBy(() -> validator.validateRichText(linkDoc("https://example.com", "_parent", null), 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("target is invalid");
    }

    @Test
    void rejectsOversizedRel() {
        assertThatThrownBy(() -> validator.validateRichText(linkDoc("https://example.com", null, "r".repeat(513)), 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("rel is invalid");
    }

    @Test
    void acceptsDepthAtThirtyTwo() {
        String node = "{\"type\":\"paragraph\"}";
        for (int i = 0; i < 30; i++) {
            node = """
                    {"type":"paragraph","content":[%s]}
                    """.formatted(node);
        }
        JsonNode content = read("""
                {"type":"doc","content":[%s]}
                """.formatted(node));

        validator.validateRichText(content, 1);
    }

    @Test
    void rejectsDepthGreaterThanThirtyTwo() {
        String node = "{\"type\":\"paragraph\"}";
        for (int i = 0; i < 32; i++) {
            node = """
                    {"type":"paragraph","content":[%s]}
                    """.formatted(node);
        }
        JsonNode content = read("""
                {"type":"doc","content":[%s]}
                """.formatted(node));

        assertThatThrownBy(() -> validator.validateRichText(content, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("nesting depth");
    }

    @Test
    void acceptsTenThousandNodes() {
        StringBuilder children = new StringBuilder();
        for (int i = 0; i < 9_998; i++) {
            if (i > 0) {
                children.append(',');
            }
            children.append("{\"type\":\"hardBreak\"}");
        }
        JsonNode content = read("""
                {"type":"doc","content":[{"type":"paragraph","content":[%s]}]}
                """.formatted(children));

        validator.validateRichText(content, 1);
    }

    @Test
    void rejectsMoreThanTenThousandNodes() {
        StringBuilder children = new StringBuilder();
        for (int i = 0; i < 10_001; i++) {
            if (i > 0) {
                children.append(',');
            }
            children.append("{\"type\":\"hardBreak\"}");
        }
        JsonNode content = read("""
                {"type":"doc","content":[{"type":"paragraph","content":[%s]}]}
                """.formatted(children));

        assertThatThrownBy(() -> validator.validateRichText(content, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too many nodes");
    }

    @Test
    void acceptsTotalTextAtLimit() {
        JsonNode content = read("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"%s"}]}]}
                """.formatted("x".repeat(200_000)));

        validator.validateRichText(content, 1);
    }

    @Test
    void rejectsTotalTextOverLimit() {
        JsonNode content = read("""
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"%s"}]}]}
                """.formatted("x".repeat(200_001)));

        assertThatThrownBy(() -> validator.validateRichText(content, 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("text is too large");
    }

    @Test
    void acceptsHrefAtLimit() {
        String prefix = "https://example.com/";
        String href = prefix + "x".repeat(2_048 - prefix.length());

        validator.validateRichText(linkDoc(href, null, null), 1);
    }

    @Test
    void rejectsLinkOverLimit() {
        String href = "https://example.com/" + "x".repeat(2_048);

        assertThatThrownBy(() -> validator.validateRichText(linkDoc(href, null, null), 1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("href is invalid");
    }

    private JsonNode linkDoc(String href, String target, String rel) {
        String attrs = "\"href\":\"%s\"".formatted(escape(href));
        if (target != null) {
            attrs += ",\"target\":\"%s\"".formatted(escape(target));
        }
        if (rel != null) {
            attrs += ",\"rel\":\"%s\"".formatted(escape(rel));
        }
        return read("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        {
                          "type": "text",
                          "text": "link",
                          "marks": [
                            {
                              "type": "link",
                              "attrs": {%s}
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """.formatted(attrs));
    }

    private JsonNode read(String value) {
        return objectMapper.readTree(value);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\u0000", "\\u0000");
    }
}
