package com.example.demo.service;

import com.example.demo.dto.EnvironmentRequestDTO;
import com.example.demo.dto.EnvironmentResponseDTO;
import com.example.demo.dto.UserEnvDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.ColumnModel;
import com.example.demo.model.EnvironmentModel;
import com.example.demo.model.EnvironmentRepository;
import com.example.demo.model.UserModel;
import com.example.demo.model.UserRepository;
import com.example.demo.model.enums.EnvironmentType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final UserService userService;

    public EnvironmentService(EnvironmentRepository environmentRepository,
                              UserRepository userRepository,
                              CardRepository cardRepository,
                              UserService userService) {
        this.environmentRepository = environmentRepository;
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponseDTO> listForUser(String userEmail) {
        UserModel user = userService.requireByEmail(userEmail);
        return environmentRepository
                .findDistinctByOwnerOrUsersContainingOrderByPositionAscIdAsc(user, user)
                .stream()
                .map(EnvironmentResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponseDTO getEnvironment(Integer envId, String userEmail) {
        EnvironmentModel env = requireMember(envId, userEmail);
        return EnvironmentResponseDTO.from(env);
    }

    @Transactional
    public EnvironmentResponseDTO createEnvironment(EnvironmentRequestDTO req, String userEmail) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nome do ambiente é obrigatório");
        }

        UserModel owner = userService.requireByEmail(userEmail);
        List<EnvironmentModel> existing = environmentRepository
                .findDistinctByOwnerOrUsersContainingOrderByPositionAscIdAsc(owner, owner);

        EnvironmentModel env = new EnvironmentModel();
        env.setName(req.name().trim());
        env.setDescription(req.description() != null ? req.description().trim() : "");
        env.setPosition(req.position() != null ? req.position() : existing.size());
        env.setArchived(req.archived() != null ? req.archived() : false);
        env.setType(req.type() != null ? req.type() : EnvironmentType.DEFAULT);
        env.setOwner(owner);
        env.getUsers().add(owner);

        if (env.getType() == EnvironmentType.DEFAULT) {
            env.getColumns().add(new ColumnModel("To Do", 0, env));
            env.getColumns().add(new ColumnModel("In Progress", 1, env));
            env.getColumns().add(new ColumnModel("Done", 2, env));
        }

        EnvironmentModel saved = environmentRepository.save(env);
        return EnvironmentResponseDTO.from(saved);
    }

    @Transactional
    public EnvironmentResponseDTO updateEnvironment(Integer envId, EnvironmentRequestDTO req, String userEmail) {
        EnvironmentModel env = requireMember(envId, userEmail);

        if (req.name() != null) {
            if (req.name().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Nome do ambiente é obrigatório");
            }
            env.setName(req.name().trim());
        }
        if (req.description() != null) {
            env.setDescription(req.description().trim());
        }
        if (req.position() != null) {
            env.setPosition(req.position());
        }
        if (req.archived() != null) {
            env.setArchived(req.archived());
        }
        if (req.type() != null) {
            env.setType(req.type());
        }

        return EnvironmentResponseDTO.from(environmentRepository.save(env));
    }

    @Transactional
    public EnvironmentResponseDTO setArchiveStatus(Integer envId, Boolean archived, String userEmail) {
        EnvironmentModel env = requireMember(envId, userEmail);
        env.setArchived(Boolean.TRUE.equals(archived));
        return EnvironmentResponseDTO.from(environmentRepository.save(env));
    }

    @Transactional
    public void deleteEnvironment(Integer envId, String userEmail) {
        EnvironmentModel env = requireMember(envId, userEmail);
        UserModel user = userService.requireByEmail(userEmail);
        if (!env.getOwner().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Apenas o dono pode excluir o ambiente");
        }
        for (CardModel card : env.getCards()) {
            card.setColumn(null);
            card.setPriority(null);
            card.getUsers().clear();
        }
        cardRepository.saveAll(env.getCards());
        cardRepository.flush();
        env.getUsers().clear();
        environmentRepository.delete(env);
    }

    @Transactional
    public UserEnvDTO addUser(Integer envId, Integer userId, String requesterEmail) {
        EnvironmentModel env = requireMember(envId, requesterEmail);
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "userId é obrigatório");
        }

        UserModel target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        boolean alreadyMember = env.getUsers().stream()
                .anyMatch(u -> u.getId().equals(target.getId()));
        if (alreadyMember) {
            throw new ApiException(HttpStatus.CONFLICT, "Esse usuário já faz parte do ambiente");
        }

        env.getUsers().add(target);
        environmentRepository.save(env);
        return UserEnvDTO.from(target);
    }

    @Transactional
    public void removeUser(Integer envId, Integer userId, String requesterEmail) {
        EnvironmentModel env = requireMember(envId, requesterEmail);
        if (userId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "userId é obrigatório");
        }

        if (env.getOwner().getId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O dono do ambiente não pode ser removido");
        }

        UserModel target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        boolean removed = env.getUsers().removeIf(u -> u.getId().equals(target.getId()));
        if (!removed) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Usuário não faz parte deste ambiente");
        }

        List<CardModel> cards = cardRepository.findByEnvironmentIdAndUsersContaining(envId, target);
        for (CardModel card : cards) {
            card.getUsers().removeIf(u -> u.getId().equals(target.getId()));
        }
        cardRepository.saveAll(cards);
        environmentRepository.save(env);
    }

    public EnvironmentModel requireMember(Integer envId, String userEmail) {
        UserModel user = userService.requireByEmail(userEmail);
        EnvironmentModel env = environmentRepository.findById(envId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ambiente não encontrado"));

        boolean isOwner = env.getOwner().getId().equals(user.getId());
        boolean isMember = env.getUsers().stream().anyMatch(u -> u.getId().equals(user.getId()));

        if (!isOwner && !isMember) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Você não tem acesso a este ambiente");
        }
        return env;
    }
}
