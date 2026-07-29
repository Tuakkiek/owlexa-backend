package db.migration;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

class V9__unify_rich_text_documentsTest {

    private final V9__unify_rich_text_documents migration =
            new V9__unify_rich_text_documents();

    @Test
    void convertsLegacyHtmlWithoutDroppingStructureOrMarks() {
        JsonNode document = migration.fromHtml("""
                <h2>Heading</h2>
                <p>Plain <strong>bold</strong> <em>italic</em>
                  <a href="https://owlexa.vn">link</a></p>
                <ul><li>First</li><li>Second</li></ul>
                """);

        assertThat(document.path("type").asText()).isEqualTo("doc");
        assertThat(document.toString())
                .contains("\"type\":\"heading\"")
                .contains("\"level\":2")
                .contains("\"type\":\"bold\"")
                .contains("\"type\":\"italic\"")
                .contains("\"type\":\"link\"")
                .contains("https://owlexa.vn")
                .contains("\"type\":\"bulletList\"")
                .contains("First")
                .contains("Second");
    }

    @Test
    void convertsEmptyLegacyValueToEditableEmptyDocument() {
        JsonNode document = migration.fromHtml("");

        assertThat(document.path("type").asText()).isEqualTo("doc");
        assertThat(document.path("content").get(0).path("type").asText())
                .isEqualTo("paragraph");
    }

    @Test
    void preservesExistingProseMirrorJsonInsteadOfTreatingItAsHtml() throws Exception {
        String existing = """
                {"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Already JSON"}]}]}
                """;

        String converted = migration.convertLegacyValue(existing);

        assertThat(converted).isEqualTo(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Already JSON\"}]}]}"
        );
    }
}
