package com.gloyoo.backend.answer.entity;

import com.gloyoo.backend.question.entity.Question;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name="answer")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String answer;

    @Column(nullable = false)
    private UUID userId; // from JWT, not from frontend

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
