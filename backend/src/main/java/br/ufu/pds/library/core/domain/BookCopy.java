package br.ufu.pds.library.core.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Representa um exemplar físico individual de um livro. Cada exemplar possui um código hash único
 * para rastreamento.
 */
@Entity
@Table(name = "book_copies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "copy_code", nullable = false, unique = true, length = 20)
    private String copyCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
