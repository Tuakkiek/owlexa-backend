package com.owlexa.owlexabackend.common.richtext;

import com.owlexa.owlexabackend.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RichTextDocumentServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RichTextDocumentService service = new RichTextDocumentService(objectMapper);

    @Test
    void serializeAndDeserialize_preservesRichNodesAndMarks() {
        JsonNode document = objectMapper.readTree("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "heading",
                      "attrs": {"level": 2},
                      "content": [
                        {
                          "type": "text",
                          "text": "Exam",
                          "marks": [
                            {"type": "bold"},
                            {"type": "textStyle", "attrs": {"color": "#dc2626"}}
                          ]
                        }
                      ]
                    },
                    {"type": "image", "attrs": {"fileId": 15, "src": "/uploads/image.png", "width": 420}},
                    {"type": "audio", "attrs": {"fileId": 16, "src": "/uploads/audio.mp3"}},
                    {"type": "video", "attrs": {"fileId": 17, "src": "/uploads/video.mp4"}},
                    {"type": "pdfAttachment", "attrs": {"fileId": 18, "src": "/uploads/file.pdf"}},
                    {"type": "table", "content": []}
                  ]
                }
                """);

        String serialized = service.serialize(document);
        JsonNode restored = service.deserialize(serialized);

        assertThat(restored).isEqualTo(document);
        assertThat(restored.toString())
                .contains("\"image\"")
                .contains("\"audio\"")
                .contains("\"video\"")
                .contains("\"pdfAttachment\"")
                .contains("\"textStyle\"");
    }

    @Test
    void normalize_convertsLegacyPlainTextToProseMirrorJson() {
        JsonNode document = service.normalize(null, "Legacy instructions");

        assertThat(document.path("type").asText()).isEqualTo("doc");
        assertThat(document.toString()).contains("Legacy instructions");
    }

    @Test
    void normalize_rejectsNonDocumentJson() {
        JsonNode invalid = objectMapper.readTree("{\"type\":\"paragraph\"}");

        assertThatThrownBy(() -> service.normalize(invalid, null))
                .isInstanceOf(BadRequestException.class);
    }
}
