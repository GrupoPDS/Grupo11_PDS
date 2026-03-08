package br.ufu.pds.library.infrastructure.persistence;

import br.ufu.pds.library.core.domain.Book;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    // Busca parcial no título (case-insensitive)
    List<Book> findByTitleContainingIgnoreCase(String title);

    // Busca parcial no autor (case-insensitive)
    List<Book> findByAuthorContainingIgnoreCase(String author);

    // Busca unificada: título, autor, ISBN ou categoria (case-insensitive)
    // Ordena por relevância: match no título primeiro, depois autor, depois ISBN/categoria
    @Query(
            "SELECT b FROM Book b WHERE "
                    + "LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%')) OR "
                    + "LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%')) OR "
                    + "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :q, '%')) OR "
                    + "LOWER(b.category) LIKE LOWER(CONCAT('%', :q, '%')) "
                    + "ORDER BY "
                    + "CASE WHEN LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%')) THEN 0 "
                    + "     WHEN LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%')) THEN 1 "
                    + "     ELSE 2 END, "
                    + "b.title ASC")
    List<Book> search(@Param("q") String query);
}
