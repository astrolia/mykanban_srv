package com.example.demo.service;

import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.EnvironmentModel;
import com.example.demo.model.EnvironmentRepository;
import com.example.demo.model.UserModel;
import com.example.demo.model.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EnvironmentRepository environmentRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       EnvironmentRepository environmentRepository,
                       CardRepository cardRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.environmentRepository = environmentRepository;
        this.cardRepository = cardRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
        return userRepository.save(userModel);
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
        return userRepository.save(userModel);
    }

    @Transactional
    public void deleteById(Integer id) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        // Remove user from any cards they are assigned to
        List<CardModel> assignedCards = cardRepository.findByUsersContaining(user);
        for (CardModel card : assignedCards) {
            card.getUsers().removeIf(u -> u.getId().equals(user.getId()));
        }
        cardRepository.saveAll(assignedCards);

        // Remove user from environments where they are a member (not owner)
        List<EnvironmentModel> memberEnvs = environmentRepository.findByUsersContaining(user);
        for (EnvironmentModel env : memberEnvs) {
            if (!env.getOwner().getId().equals(user.getId())) {
                env.getUsers().removeIf(u -> u.getId().equals(user.getId()));
            }
        }
        environmentRepository.saveAll(memberEnvs);

        // Detach cards in owned environments before deleting so FK constraints (column/priority/users) never fail
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
    }
}
