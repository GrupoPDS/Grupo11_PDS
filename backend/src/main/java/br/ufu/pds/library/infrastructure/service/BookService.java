package br.ufu.pds.library.infrastructure.service;

import br.ufu.pds.library.core.domain.Book;
import br.ufu.pds.library.core.domain.BookCopy;
import br.ufu.pds.library.core.domain.Loan;
import br.ufu.pds.library.core.domain.LoanStatus;
import br.ufu.pds.library.core.domain.ReservationStatus;
import br.ufu.pds.library.core.exceptions.BookNotFoundException;
import br.ufu.pds.library.core.exceptions.DuplicateIsbnException;
import br.ufu.pds.library.entrypoint.api.dto.BookCopyDetailResponse;
import br.ufu.pds.library.infrastructure.persistence.BookCopyRepository;
import br.ufu.pds.library.infrastructure.persistence.BookRepository;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import br.ufu.pds.library.infrastructure.persistence.ReservationRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Book save(Book book) {
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new DuplicateIsbnException(book.getIsbn());
        }
        Book saved = bookRepository.save(book);
        generateCopies(saved, saved.getQuantity());
        return saved;
    }

    /** Gera exemplares individuais com código hash único para um livro. */
    private void generateCopies(Book book, int count) {
        for (int i = 0; i < count; i++) {
            String code = generateUniqueCopyCode();
            BookCopy copy = BookCopy.builder().book(book).copyCode(code).build();
            bookCopyRepository.save(copy);
        }
    }

    /** Gera um código hash único de 8 caracteres hexadecimais. */
    private String generateUniqueCopyCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (bookCopyRepository.existsByCopyCode(code));
        return code;
    }

    /** Retorna todos os exemplares de um livro. */
    @Transactional(readOnly = true)
    public List<BookCopy> findCopiesByBookId(Long bookId) {
        return bookCopyRepository.findByBookId(bookId);
    }

    /** Retorna exemplares agrupados por livro para uma lista de IDs. */
    @Transactional(readOnly = true)
    public Map<Long, List<BookCopy>> findCopiesByBookIds(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Map.of();
        }
        List<BookCopy> allCopies = bookCopyRepository.findByBookIdIn(bookIds);
        Map<Long, List<BookCopy>> result = new HashMap<>();
        for (BookCopy copy : allCopies) {
            result.computeIfAbsent(copy.getBook().getId(), k -> new java.util.ArrayList<>())
                    .add(copy);
        }
        return result;
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
        existing.setCategory(updatedBook.getCategory());

        return bookRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        bookRepository.deleteById(id);
    }

    // Busca de livros por query (título ou autor)
    // Se query for vazia ou nula, retorna todos os livros
    // Caso contrário, realiza busca filtrada no repositório
    public List<Book> search(String query) {
        List<Book> books;

        String trimmedQuery = query != null ? query.trim() : "";

        if (trimmedQuery.isEmpty()) {
            books = bookRepository.findAll();
        } else {
            books = bookRepository.search(trimmedQuery);
        }

        return books;
    }

    /**
     * Calcula a quantidade de cópias disponíveis para empréstimo de cada livro. Disponível =
     * quantity - (empréstimos ACTIVE + OVERDUE) - reservas AVAILABLE_FOR_PICKUP
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> calculateAvailability(List<Book> books) {
        Map<Long, Integer> availability = new HashMap<>();
        for (Book book : books) {
            long activeLoans =
                    loanRepository.countByBookIdAndStatus(book.getId(), LoanStatus.ACTIVE);
            long overdueLoans =
                    loanRepository.countByBookIdAndStatus(book.getId(), LoanStatus.OVERDUE);
            long pickupReservations =
                    reservationRepository.countByBookIdAndStatus(
                            book.getId(), ReservationStatus.AVAILABLE_FOR_PICKUP);
            int available =
                    (int)
                            Math.max(
                                    0,
                                    book.getQuantity()
                                            - activeLoans
                                            - overdueLoans
                                            - pickupReservations);
            availability.put(book.getId(), available);
        }
        return availability;
    }

    /**
     * Retorna detalhes de todos os exemplares de um livro, incluindo status de empréstimo. Cada
     * exemplar indica se está DISPONIVEL, EMPRESTADO ou ATRASADO, e caso emprestado, mostra o
     * nome/e-mail do usuário e data de devolução.
     */
    @Transactional(readOnly = true)
    public List<BookCopyDetailResponse> getBookCopyDetails(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException(bookId);
        }

        List<BookCopy> copies = bookCopyRepository.findByBookId(bookId);

        if (copies.isEmpty()) {
            return List.of();
        }

        List<Long> copyIds = copies.stream().map(BookCopy::getId).toList();
        List<Loan> activeLoans =
                loanRepository.findActiveLoansByCopyIds(
                        copyIds, List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE));

        // Mapa de copyId → Loan (empréstimo ativo/atrasado)
        Map<Long, Loan> loanByCopyId =
                activeLoans.stream()
                        .filter(l -> l.getBookCopy() != null)
                        .collect(
                                Collectors.toMap(
                                        l -> l.getBookCopy().getId(), l -> l, (a, b) -> a));

        return copies.stream()
                .map(
                        copy -> {
                            Loan loan = loanByCopyId.get(copy.getId());
                            if (loan != null) {
                                String status =
                                        loan.getStatus() == LoanStatus.OVERDUE
                                                ? "ATRASADO"
                                                : "EMPRESTADO";
                                return new BookCopyDetailResponse(
                                        copy.getId(),
                                        copy.getCopyCode(),
                                        status,
                                        loan.getUser().getName(),
                                        loan.getUser().getEmail(),
                                        loan.getLoanDate(),
                                        loan.getDueDate());
                            }
                            return new BookCopyDetailResponse(
                                    copy.getId(),
                                    copy.getCopyCode(),
                                    "DISPONIVEL",
                                    null,
                                    null,
                                    null,
                                    null);
                        })
                .toList();
    }
}
