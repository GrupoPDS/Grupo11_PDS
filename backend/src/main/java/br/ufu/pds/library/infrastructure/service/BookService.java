package br.ufu.pds.library.infrastructure.service;

import br.ufu.pds.library.core.domain.Book;
import br.ufu.pds.library.core.exceptions.BookNotFoundException;
import br.ufu.pds.library.core.exceptions.DuplicateIsbnException;
import br.ufu.pds.library.infrastructure.persistence.BookRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional
    public Book save(Book book) {
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new DuplicateIsbnException(book.getIsbn());
        }
        return bookRepository.save(book);
    }

    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public Book update(Long id, Book updatedBook) {
        Book existing =
                bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));

        // Se o ISBN mudou, verificar se o novo já existe em outro livro
        if (!existing.getIsbn().equals(updatedBook.getIsbn())
                && bookRepository.existsByIsbn(updatedBook.getIsbn())) {
            throw new DuplicateIsbnException(updatedBook.getIsbn());
        }

        existing.setTitle(updatedBook.getTitle());
        existing.setAuthor(updatedBook.getAuthor());
        existing.setIsbn(updatedBook.getIsbn());
        existing.setPublisher(updatedBook.getPublisher());
        existing.setYear(updatedBook.getYear());
        existing.setQuantity(updatedBook.getQuantity());

        return bookRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        bookRepository.deleteById(id);
    }
}
