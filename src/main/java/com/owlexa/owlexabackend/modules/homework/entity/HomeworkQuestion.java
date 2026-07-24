package com.owlexa.owlexabackend.modules.homework.entity;

import com.owlexa.owlexabackend.modules.homework.enums.HomeworkQuestionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "homework_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeworkQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_template_id", nullable = false)
    private HomeworkTemplate homeworkTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private HomeworkQuestionType type;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "attached_image_url", length = 1000)
    private String attachedImageUrl;

    @Column(name = "attached_audio_url", length = 1000)
    private String attachedAudioUrl;

    @Column(name = "attached_file_url", length = 1000)
    private String attachedFileUrl;

    @Column(name = "max_score")
    private Double maxScore;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HomeworkQuestionOption> options = new ArrayList<>();

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private HomeworkRubric rubric;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
