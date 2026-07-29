package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Converts every legacy HTML rich-text column to the canonical ProseMirror JSON
 * document used by the frontend editor. The converter preserves supported block
 * structure, inline marks, links, code, lists and media URLs instead of stripping
 * legacy markup to plain text.
 */
public class V9__unify_rich_text_documents extends BaseJavaMigration {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        renameLegacyColumns(connection);

        convertColumn(connection, "questions", "content_json", false);
        convertColumn(connection, "questions", "explanation_json", true);
        convertColumn(connection, "questions", "sample_answer_json", true);
        convertColumn(connection, "grading_criteria", "content_json", false);
        convertColumn(connection, "assessment_items", "content_json", false);
        convertColumn(connection, "assessment_items", "explanation_json", true);
        convertColumn(connection, "assessment_items", "sample_answer_json", true);
        convertColumn(connection, "assessment_items", "grading_criteria_content_json", true);
        convertColumn(connection, "assignment_items", "content_json", false);
        convertColumn(connection, "assignment_items", "explanation_json", true);
        convertColumn(connection, "assignment_items", "sample_answer_json", true);
        convertColumn(connection, "assignment_items", "grading_criteria_content_json", true);

        addJsonConstraints(connection);
        execute(connection, """
                ALTER TABLE `file_references`
                MODIFY COLUMN `owner_type`
                  ENUM('ASSESSMENT','QUESTION','GRADING_CRITERIA','ASSIGNMENT')
                  COLLATE utf8mb4_unicode_ci NOT NULL
                """);
    }

    private void renameLegacyColumns(Connection connection) throws Exception {
        execute(connection, """
                ALTER TABLE `questions`
                  CHANGE COLUMN `content` `content_json` LONGTEXT COLLATE utf8mb4_bin NOT NULL,
                  CHANGE COLUMN `explanation` `explanation_json` LONGTEXT COLLATE utf8mb4_bin NULL,
                  CHANGE COLUMN `sample_answer` `sample_answer_json` LONGTEXT COLLATE utf8mb4_bin NULL
                """);
        execute(connection, """
                ALTER TABLE `grading_criteria`
                  CHANGE COLUMN `content` `content_json` LONGTEXT COLLATE utf8mb4_bin NOT NULL
                """);
        execute(connection, """
                ALTER TABLE `assessment_items`
                  CHANGE COLUMN `content` `content_json` LONGTEXT COLLATE utf8mb4_bin NOT NULL,
                  CHANGE COLUMN `explanation` `explanation_json` LONGTEXT COLLATE utf8mb4_bin NULL,
                  CHANGE COLUMN `sample_answer` `sample_answer_json` LONGTEXT COLLATE utf8mb4_bin NULL,
                  CHANGE COLUMN `grading_criteria_content`
                    `grading_criteria_content_json` LONGTEXT COLLATE utf8mb4_bin NULL
                """);
        execute(connection, """
                ALTER TABLE `assignment_items`
                  CHANGE COLUMN `content` `content_json` LONGTEXT COLLATE utf8mb4_bin NOT NULL,
                  CHANGE COLUMN `explanation` `explanation_json` LONGTEXT COLLATE utf8mb4_bin NULL,
                  CHANGE COLUMN `sample_answer` `sample_answer_json` LONGTEXT COLLATE utf8mb4_bin NULL,
                  CHANGE COLUMN `grading_criteria_content`
                    `grading_criteria_content_json` LONGTEXT COLLATE utf8mb4_bin NULL
                """);
    }

    private void convertColumn(
            Connection connection,
            String table,
            String column,
            boolean nullable
    ) throws Exception {
        List<RowValue> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT `id`, `" + column + "` FROM `" + table + "`"
             )) {
            while (resultSet.next()) {
                rows.add(new RowValue(resultSet.getLong(1), resultSet.getString(2)));
            }
        }

        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE `" + table + "` SET `" + column + "` = ? WHERE `id` = ?"
        )) {
            for (RowValue row : rows) {
                String converted = row.value() == null && nullable
                        ? null
                        : convertLegacyValue(row.value());
                update.setString(1, converted);
                update.setLong(2, row.id());
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    String convertLegacyValue(String value) throws Exception {
        if (value != null && !value.isBlank()) {
            try {
                var parsed = objectMapper.readTree(value);
                if (parsed.isObject() && "doc".equals(parsed.path("type").asText())) {
                    return objectMapper.writeValueAsString(parsed);
                }
            } catch (Exception ignored) {
                // Legacy HTML/plain text is converted below.
            }
        }
        return objectMapper.writeValueAsString(fromHtml(value));
    }

    ObjectNode fromHtml(String html) {
        ObjectNode document = node("doc");
        ArrayNode content = document.putArray("content");
        if (html == null || html.isBlank()) {
            content.add(node("paragraph"));
            return document;
        }

        Element body = Jsoup.parseBodyFragment(html).body();
        List<Node> pendingInline = new ArrayList<>();
        for (Node child : body.childNodes()) {
            if (isBlock(child)) {
                flushInlineParagraph(content, pendingInline);
                appendBlock(content, child);
            } else {
                pendingInline.add(child);
            }
        }
        flushInlineParagraph(content, pendingInline);
        if (content.isEmpty()) {
            content.add(node("paragraph"));
        }
        return document;
    }

    private void appendBlock(ArrayNode target, Node source) {
        if (!(source instanceof Element element)) {
            ObjectNode paragraph = node("paragraph");
            appendInlineChildren(paragraph.putArray("content"), List.of(source), List.of());
            if (paragraph.path("content").size() > 0) {
                target.add(paragraph);
            }
            return;
        }

        String tag = element.normalName();
        switch (tag) {
            case "p", "div", "section", "article" ->
                    target.add(textBlock("paragraph", element));
            case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                ObjectNode heading = textBlock("heading", element);
                heading.putObject("attrs").put(
                        "level",
                        Integer.parseInt(tag.substring(1))
                );
                target.add(heading);
            }
            case "ul", "ol" -> target.add(listBlock(element));
            case "blockquote" -> {
                ObjectNode blockquote = node("blockquote");
                ArrayNode children = blockquote.putArray("content");
                for (Node child : element.childNodes()) {
                    appendBlock(children, child);
                }
                if (children.isEmpty()) {
                    children.add(textBlock("paragraph", element));
                }
                target.add(blockquote);
            }
            case "pre" -> {
                ObjectNode code = node("codeBlock");
                String value = element.wholeText();
                if (!value.isEmpty()) {
                    code.putArray("content").add(text(value, List.of()));
                }
                target.add(code);
            }
            case "hr" -> target.add(node("horizontalRule"));
            case "img" -> target.add(imageNode(element));
            case "audio", "video" -> target.add(mediaNode(element));
            default -> target.add(textBlock("paragraph", element));
        }
    }

    private ObjectNode textBlock(String type, Element element) {
        ObjectNode block = node(type);
        ArrayNode content = block.putArray("content");
        appendInlineChildren(content, element.childNodes(), List.of());
        return block;
    }

    private ObjectNode listBlock(Element list) {
        ObjectNode result = node("ol".equals(list.normalName()) ? "orderedList" : "bulletList");
        ArrayNode items = result.putArray("content");
        for (Element child : list.children()) {
            if (!"li".equals(child.normalName())) {
                continue;
            }
            ObjectNode listItem = node("listItem");
            ArrayNode itemContent = listItem.putArray("content");
            ObjectNode paragraph = node("paragraph");
            ArrayNode inline = paragraph.putArray("content");
            List<Node> inlineNodes = child.childNodes().stream()
                    .filter(node -> !(node instanceof Element nested
                            && ("ul".equals(nested.normalName()) || "ol".equals(nested.normalName()))))
                    .toList();
            appendInlineChildren(inline, inlineNodes, List.of());
            itemContent.add(paragraph);
            for (Element nested : child.children()) {
                if ("ul".equals(nested.normalName()) || "ol".equals(nested.normalName())) {
                    itemContent.add(listBlock(nested));
                }
            }
            items.add(listItem);
        }
        return result;
    }

    private void appendInlineChildren(
            ArrayNode target,
            List<Node> sources,
            List<ObjectNode> inheritedMarks
    ) {
        for (Node source : sources) {
            if (source instanceof TextNode textNode) {
                String value = textNode.getWholeText();
                if (!value.isEmpty()) {
                    target.add(text(value, inheritedMarks));
                }
                continue;
            }
            if (!(source instanceof Element element)) {
                continue;
            }

            String tag = element.normalName();
            if ("br".equals(tag)) {
                target.add(node("hardBreak"));
                continue;
            }
            if ("img".equals(tag)) {
                target.add(imageNode(element));
                continue;
            }

            List<ObjectNode> marks = new ArrayList<>(inheritedMarks);
            ObjectNode mark = markFor(element);
            if (mark != null) {
                marks.add(mark);
            }
            appendInlineChildren(target, element.childNodes(), marks);
        }
    }

    private ObjectNode markFor(Element element) {
        return switch (element.normalName()) {
            case "strong", "b" -> node("bold");
            case "em", "i" -> node("italic");
            case "u" -> node("underline");
            case "s", "strike", "del" -> node("strike");
            case "code" -> node("code");
            case "a" -> {
                ObjectNode link = node("link");
                link.putObject("attrs")
                        .put("href", element.attr("href"))
                        .put("target", "_blank")
                        .put("rel", "noopener noreferrer nofollow");
                yield link;
            }
            default -> null;
        };
    }

    private ObjectNode imageNode(Element image) {
        ObjectNode result = node("image");
        ObjectNode attrs = result.putObject("attrs");
        attrs.put("src", image.attr("src"));
        attrs.put("alt", image.attr("alt"));
        attrs.put("title", image.attr("title"));
        return result;
    }

    private ObjectNode mediaNode(Element media) {
        ObjectNode result = node(media.normalName().toLowerCase(Locale.ROOT));
        String src = media.attr("src");
        if (src.isBlank()) {
            Element source = media.selectFirst("source[src]");
            src = source == null ? "" : source.attr("src");
        }
        result.putObject("attrs").put("src", src);
        return result;
    }

    private ObjectNode text(String value, List<ObjectNode> marks) {
        ObjectNode result = node("text");
        result.put("text", value);
        if (!marks.isEmpty()) {
            ArrayNode markArray = result.putArray("marks");
            marks.forEach(markArray::add);
        }
        return result;
    }

    private ObjectNode node(String type) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("type", type);
        return result;
    }

    private boolean isBlock(Node node) {
        return node instanceof Element element && switch (element.normalName()) {
            case "p", "div", "section", "article",
                    "h1", "h2", "h3", "h4", "h5", "h6",
                    "ul", "ol", "blockquote", "pre", "hr",
                    "img", "audio", "video" -> true;
            default -> false;
        };
    }

    private void flushInlineParagraph(ArrayNode target, List<Node> pendingInline) {
        if (pendingInline.isEmpty()) {
            return;
        }
        ObjectNode paragraph = node("paragraph");
        ArrayNode inline = paragraph.putArray("content");
        appendInlineChildren(inline, pendingInline, List.of());
        if (!inline.isEmpty()) {
            target.add(paragraph);
        }
        pendingInline.clear();
    }

    private void addJsonConstraints(Connection connection) throws Exception {
        addConstraint(connection, "questions", "content_json", false);
        addConstraint(connection, "questions", "explanation_json", true);
        addConstraint(connection, "questions", "sample_answer_json", true);
        addConstraint(connection, "grading_criteria", "content_json", false);
        addConstraint(connection, "assessment_items", "content_json", false);
        addConstraint(connection, "assessment_items", "explanation_json", true);
        addConstraint(connection, "assessment_items", "sample_answer_json", true);
        addConstraint(connection, "assessment_items", "grading_criteria_content_json", true);
        addConstraint(connection, "assignment_items", "content_json", false);
        addConstraint(connection, "assignment_items", "explanation_json", true);
        addConstraint(connection, "assignment_items", "sample_answer_json", true);
        addConstraint(connection, "assignment_items", "grading_criteria_content_json", true);
    }

    private void addConstraint(
            Connection connection,
            String table,
            String column,
            boolean nullable
    ) throws Exception {
        String condition = nullable
                ? "`" + column + "` IS NULL OR JSON_VALID(`" + column + "`)"
                : "JSON_VALID(`" + column + "`)";
        execute(
                connection,
                "ALTER TABLE `" + table + "` ADD CONSTRAINT `chk_" + table + "_" + column
                        + "` CHECK (" + condition + ")"
        );
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private record RowValue(long id, String value) {
    }
}
