package br.ufu.pds.library.infrastructure.service;

import br.ufu.pds.library.core.domain.LoanStatus;
import br.ufu.pds.library.core.domain.User;
import br.ufu.pds.library.core.exceptions.DuplicateEmailException;
import br.ufu.pds.library.core.exceptions.UserHasActiveLoansException;
import br.ufu.pds.library.core.exceptions.UserNotFoundException;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;

    @Transactional
    public User save(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateEmailException(user.getEmail());
        }
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public User update(Long id, User updatedUser) {
        User existing =
                userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        if (!existing.getEmail().equals(updatedUser.getEmail())
                && userRepository.existsByEmail(updatedUser.getEmail())) {
            throw new DuplicateEmailException(updatedUser.getEmail());
        }

        existing.setName(updatedUser.getName());
        existing.setEmail(updatedUser.getEmail());
        existing.setPassword(updatedUser.getPassword());
        existing.setRole(updatedUser.getRole());

        return userRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        long activeLoans = loanRepository.findByUserIdAndStatus(id, LoanStatus.ACTIVE).size();
        if (activeLoans > 0) {
            throw new UserHasActiveLoansException(id);
        }

        user.setActive(false);
        userRepository.save(user);
    }
}
