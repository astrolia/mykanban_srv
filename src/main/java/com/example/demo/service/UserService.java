package com.example.demo.service;

import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.EnvironmentModel;
import com.example.demo.model.EnvironmentRepository;
import com.example.demo.model.UserModel;
import com.example.demo.model.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EnvironmentRepository environmentRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;

    public Iterable<UserModel> getAll() {
        return userRepository.findAll();
    }

    @Transactional
    public UserModel saveUser(UserModel userModel) {
        String normalizedEmail = userModel.getEmail().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Este email já está em uso");
        }
        userModel.setName(userModel.getName().trim());
        userModel.setEmail(normalizedEmail);
        userModel.setPassword(passwordEncoder.encode(userModel.getPassword()));
        UserModel saved = userRepository.save(userModel);
        log.info("Usuário cadastrado com id={}", saved.getId());
        return saved;
    }

    public Optional<UserModel> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    public UserModel requireByEmail(String email) {
        return findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public Optional<UserModel> findById(Integer id) {
        return userRepository.findById(id);
    }

    @Transactional
    public UserModel update(UserModel userModel) {
        UserModel updated = userRepository.save(userModel);
        log.info("Usuário atualizado id={}", updated.getId());
        return updated;
    }

    @Transactional
    public void deleteById(Integer id) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        List<CardModel> assignedCards = cardRepository.findByUsersContaining(user);
        for (CardModel card : assignedCards) {
            card.getUsers().removeIf(u -> u.getId().equals(user.getId()));
        }
        cardRepository.saveAll(assignedCards);

        List<EnvironmentModel> memberEnvs = environmentRepository.findByUsersContaining(user);
        for (EnvironmentModel env : memberEnvs) {
            if (!env.getOwner().getId().equals(user.getId())) {
                env.removeUser(user);
            }
        }
        environmentRepository.saveAll(memberEnvs);

        for (EnvironmentModel ownedEnv : user.getOwnedEnvironments()) {
            for (CardModel card : ownedEnv.getCards()) {
                card.setColumn(null);
                card.setPriority(null);
                card.getUsers().clear();
            }
            cardRepository.saveAll(ownedEnv.getCards());
            ownedEnv.getUsers().clear();
        }
        cardRepository.flush();

        userRepository.delete(user);
        log.info("Usuário excluído id={}", id);
    }
}
