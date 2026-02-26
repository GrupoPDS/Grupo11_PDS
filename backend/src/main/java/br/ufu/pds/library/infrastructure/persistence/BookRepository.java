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

    // Busca unificada: título OU autor (case-insensitive)
    // Utiliza @Query porque métodos derivados só suportam AND entre condições
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Book> search(@Param("q") String query);
}
