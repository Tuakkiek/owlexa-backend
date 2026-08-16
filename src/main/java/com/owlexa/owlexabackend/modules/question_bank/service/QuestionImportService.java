package com.owlexa.owlexabackend.modules.question_bank.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionOptionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.request.QuestionRequest;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionImportPreviewItemResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionImportResultResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionImportValidationResponse;
import com.owlexa.owlexabackend.modules.question_bank.dto.response.QuestionResponse;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionCollection;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionDifficulty;
import com.owlexa.owlexabackend.modules.question_bank.entity.QuestionType;
import com.owlexa.owlexabackend.modules.user.entity.Role;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.MembershipRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionImportService {

    private static final String SUPPORTED_VERSION = "2.0";
    private static final int MAX_QUESTIONS_PER_IMPORT = 200;
    private static final int MAX_JSON_CHARACTERS = 1_000_000;

    private final QuestionService questionService;
    private final QuestionCollectionService collectionService;
    private final ObjectMapper objectMapper;
    private final AuthorizationService authorizationService;
    private final MembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public QuestionImportValidationResponse validate(Long collectionId, String json) {
        requireTeacherInCurrentCenter();
        QuestionCollection collection = collectionService.requireActiveById(collectionId);
        ParsedImport parsedImport = parseAndValidate(json);
        List<QuestionRequest> requests = bindCollection(parsedImport.requests(), collection.getId());
        questionService.validateImportBatch(requests);
        return QuestionImportValidationResponse.builder()
                .version(SUPPORTED_VERSION)
                .collectionId(collection.getId())
                .collectionName(collection.getName())
                .collectionCode(collection.getCode())
                .questionCount(requests.size())
                .questions(toPreviewItems(requests))
                .build();
    }

    @Transactional
    public QuestionImportResultResponse importQuestions(Long collectionId, String json) {
        requireTeacherInCurrentCenter();
        QuestionCollection collection = collectionService.requireActiveById(collectionId);
        ParsedImport parsedImport = parseAndValidate(json);
        List<QuestionRequest> requests = bindCollection(parsedImport.requests(), collection.getId());
        questionService.validateImportBatch(requests);
        List<QuestionResponse> createdQuestions = new ArrayList<>();
        for (QuestionRequest request : requests) {
            createdQuestions.add(questionService.create(request));
        }
        return QuestionImportResultResponse.builder()
                .importedCount(createdQuestions.size())
                .questions(createdQuestions)
                .build();
    }

    private ParsedImport parseAndValidate(String json) {
        if (json == null || json.isBlank()) {
            throw new BadRequestException("Import JSON is required.");
        }
        if (json.length() > MAX_JSON_CHARACTERS) {
            throw new BadRequestException("Import JSON is too large.");
        }
        JsonNode payload;
        try {
            payload = objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new BadRequestException("Import JSON is malformed.");
        }
        if (payload == null || payload.isNull()) {
            throw new BadRequestException("Import JSON is required.");
        }
        if (!payload.isObject()) {
            throw new BadRequestException("Import JSON root must be an object.");
        }
        if (!payload.path("version").isTextual()) {
            throw new BadRequestException("Missing version.");
        }
        String version = payload.path("version").asText();
        if (!SUPPORTED_VERSION.equals(version)) {
            throw new BadRequestException("Unsupported import version: " + version + ".");
        }
        JsonNode questionsNode = payload.path("questions");
        if (!questionsNode.isArray()) {
            throw new BadRequestException("Missing questions.");
        }
        if (questionsNode.size() == 0) {
            throw new BadRequestException("Questions must not be empty.");
        }
        if (questionsNode.size() > MAX_QUESTIONS_PER_IMPORT) {
            throw new BadRequestException("Maximum 200 questions per import.");
        }

        List<QuestionRequest> requests = new ArrayList<>();
        for (int index = 0; index < questionsNode.size(); index++) {
            requests.add(toQuestionRequest(questionsNode.get(index), index + 1));
        }
        return new ParsedImport(requests);
    }

    private QuestionRequest toQuestionRequest(JsonNode questionNode, int questionNumber) {
        if (questionNode == null || !questionNode.isObject()) {
            throw questionError(questionNumber, "Question must be an object.");
        }

        QuestionType type = parseType(questionNode.path("type"), questionNumber);
        if (type != QuestionType.MULTIPLE_CHOICE && type != QuestionType.ESSAY) {
            throw questionError(questionNumber, "Only MULTIPLE_CHOICE and ESSAY are supported for JSON import.");
        }

        String sectionCode = requiredText(
                questionNode.path("sectionCode"),
                questionNumber,
                "Missing sectionCode."
        );
        Integer displayOrder = parseDisplayOrder(questionNode.path("displayOrder"), questionNumber);
        JsonNode content = parseRichText(questionNode.path("content"), questionNumber, "Content");
        JsonNode explanation = parseRichText(questionNode.path("explanation"), questionNumber, "Explanation");
        JsonNode sampleAnswer = parseRichText(questionNode.path("sampleAnswer"), questionNumber, "SampleAnswer");

        QuestionDifficulty difficulty = parseDifficulty(questionNode.path("difficulty"), questionNumber);
        BigDecimal points = parsePoints(questionNode.path("points"), questionNumber);
        List<QuestionOptionRequest> options = null;
        if (type == QuestionType.MULTIPLE_CHOICE) {
            options = parseOptions(questionNode.path("options"), questionNumber);
        }

        return QuestionRequest.builder()
                .sectionCode(sectionCode)
                .displayOrder(displayOrder)
                .type(type)
                .content(content)
                .explanation(explanation)
                .sampleAnswer(sampleAnswer)
                .difficulty(difficulty)
                .points(points)
                .options(options)
                .build();
    }

    private Integer parseDisplayOrder(JsonNode node, int questionNumber) {
        if (!node.isIntegralNumber() || !node.canConvertToInt() || node.intValue() < 1) {
            throw questionError(questionNumber, "Display order must be a positive integer.");
        }
        return node.intValue();
    }

    private QuestionType parseType(JsonNode typeNode, int questionNumber) {
        String type = requiredText(typeNode, questionNumber, "Missing type.");
        try {
            return QuestionType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw questionError(questionNumber, "Unsupported question type: " + type + ".");
        }
    }

    private QuestionDifficulty parseDifficulty(JsonNode difficultyNode, int questionNumber) {
        if (difficultyNode.isMissingNode() || difficultyNode.isNull()) {
            return null;
        }
        if (!difficultyNode.isTextual()) {
            throw questionError(questionNumber, "Difficulty must be a string.");
        }
        String difficulty = difficultyNode.asText();
        try {
            return QuestionDifficulty.valueOf(difficulty);
        } catch (IllegalArgumentException exception) {
            throw questionError(questionNumber, "Invalid difficulty: " + difficulty + ".");
        }
    }

    private BigDecimal parsePoints(JsonNode pointsNode, int questionNumber) {
        if (pointsNode.isMissingNode() || pointsNode.isNull()) {
            return null;
        }
        if (!pointsNode.isNumber()) {
            throw questionError(questionNumber, "Points must be a number.");
        }
        BigDecimal points = pointsNode.decimalValue();
        if (points.compareTo(BigDecimal.ZERO) <= 0) {
            throw questionError(questionNumber, "Points must be greater than 0.");
        }
        return points;
    }

    private List<QuestionOptionRequest> parseOptions(JsonNode optionsNode, int questionNumber) {
        if (!optionsNode.isArray()) {
            throw questionError(questionNumber, "Missing options.");
        }
        if (optionsNode.size() < 2) {
            throw questionError(questionNumber, "Multiple choice questions must have at least 2 options.");
        }

        List<QuestionOptionRequest> options = new ArrayList<>();
        int correctOptionCount = 0;
        for (int index = 0; index < optionsNode.size(); index++) {
            JsonNode optionNode = optionsNode.get(index);
            if (optionNode == null || !optionNode.isObject()) {
                throw questionError(questionNumber, "Option " + (index + 1) + " must be an object.");
            }
            String content = optionalText(optionNode.path("content"), questionNumber, "Option " + (index + 1) + " content must be a string.");
            JsonNode isCorrectNode = optionNode.path("isCorrect");
            if (!isCorrectNode.isBoolean()) {
                throw questionError(questionNumber, "Option " + (index + 1) + " isCorrect must be boolean.");
            }
            boolean isCorrect = isCorrectNode.booleanValue();
            if (isCorrect) {
                correctOptionCount++;
            }
            options.add(QuestionOptionRequest.builder()
                    .content(content == null ? "" : content)
                    .isCorrect(isCorrect)
                    .displayOrder(index + 1)
                    .build());
        }
        if (correctOptionCount != 1) {
            throw questionError(questionNumber, "Exactly one correct option is required.");
        }
        return options;
    }

    private String requiredText(JsonNode node, int questionNumber, String message) {
        if (node.isMissingNode() || node.isNull() || !node.isTextual() || node.asText().trim().isBlank()) {
            throw questionError(questionNumber, message);
        }
        return node.asText().trim();
    }

    private String optionalText(JsonNode node, int questionNumber, String message) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw questionError(questionNumber, message);
        }
        String value = node.asText().trim();
        return value.isBlank() ? null : value;
    }

    private JsonNode parseRichText(JsonNode node, int questionNumber, String fieldName) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String value = node.asText().trim();
            if (value.isBlank()) {
                return null;
            }
            return plainTextDocument(value);
        }
        if (node.isObject()) {
            return node;
        }
        throw questionError(questionNumber, fieldName + " must be a string or a rich text object.");
    }

    private JsonNode plainTextDocument(String value) {
        ObjectNode document = objectMapper.createObjectNode();
        document.put("type", "doc");
        ObjectNode paragraph = document.putArray("content").addObject();
        paragraph.put("type", "paragraph");
        ArrayNode content = paragraph.putArray("content");
        content.addObject()
                .put("type", "text")
                .put("text", value);
        return document;
    }

    private List<QuestionImportPreviewItemResponse> toPreviewItems(List<QuestionRequest> requests) {
        List<QuestionImportPreviewItemResponse> items = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            QuestionRequest request = requests.get(index);
            items.add(QuestionImportPreviewItemResponse.builder()
                    .questionNumber(index + 1)
                    .sectionCode(request.getSectionCode())
                    .displayOrder(request.getDisplayOrder())
                    .type(request.getType().name())
                    .content(extractPlainText(request.getContent()))
                    .difficulty(request.getDifficulty())
                    .points(request.getPoints())
                    .optionCount(request.getOptions() == null ? 0 : request.getOptions().size())
                    .build());
        }
        return items;
    }

    private List<QuestionRequest> bindCollection(
            List<QuestionRequest> requests,
            Long collectionId
    ) {
        requests.forEach(request -> request.setCollectionId(collectionId));
        return requests;
    }

    private String extractPlainText(JsonNode content) {
        if (content == null || content.isNull()) {
            return "";
        }
        return content.path("content").path(0).path("content").path(0).path("text").asText("");
    }

    private BadRequestException questionError(int questionNumber, String message) {
        return new BadRequestException("Question " + questionNumber + ": " + message);
    }

    private void requireTeacherInCurrentCenter() {
        User currentUser = authorizationService.getCurrentUser();
        Long centerId = TenantContext.getCurrentTenantId();
        if (centerId == null) {
            throw new BadRequestException("Tenant context not resolved");
        }
        if (currentUser.getRole() != Role.TEACHER) {
            throw new AccessDeniedException("Only TEACHER can manage questions");
        }
        boolean hasMembership = membershipRepository.existsByUser_IdAndCenter_Id(currentUser.getId(), centerId);
        if (!hasMembership) {
            throw new AccessDeniedException("User is not a member of this center");
        }
    }

    private record ParsedImport(List<QuestionRequest> requests) {
    }
}
