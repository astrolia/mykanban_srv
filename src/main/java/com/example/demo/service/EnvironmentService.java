package com.example.demo.service;

import com.example.demo.dto.EnvironmentRequestDTO;
import com.example.demo.dto.EnvironmentResponseDTO;
import com.example.demo.dto.EnvironmentUpdateRequestDTO;
import com.example.demo.dto.UserEnvDTO;
import com.example.demo.dto.UserEnvironmentDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.ColumnModel;
import com.example.demo.model.EnvironmentModel;
import com.example.demo.model.EnvironmentRepository;
import com.example.demo.model.UserModel;
import com.example.demo.model.UserRepository;
import com.example.demo.model.enums.EnvironmentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final UserService userService;

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
    public List<UserEnvironmentDTO> listSummariesForUser(Integer userId, String userEmail) {
        UserModel requester = userService.requireByEmail(userEmail);
        if (!requester.getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Você só pode visualizar seus próprios ambientes");
        }
        return environmentRepository
                .findDistinctByOwnerOrUsersContainingOrderByPositionAscIdAsc(requester, requester)
                .stream()
                .map(UserEnvironmentDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponseDTO getEnvironment(Integer envId, String userEmail) {
        EnvironmentModel env = requireMember(envId, userEmail);
        if (Boolean.TRUE.equals(env.getArchived())) {
            UserModel user = userService.requireByEmail(userEmail);
            if (!env.getOwner().getId().equals(user.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Este ambiente está arquivado");
            }
        }
        return EnvironmentResponseDTO.from(env);
    }

    @Transactional
    public EnvironmentResponseDTO createEnvironment(EnvironmentRequestDTO req, String userEmail) {
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
        env.addUser(owner);

        if (env.getType() == EnvironmentType.DEFAULT) {
            env.getColumns().add(new ColumnModel("To Do", 0, env));
            env.getColumns().add(new ColumnModel("In Progress", 1, env));
            env.getColumns().add(new ColumnModel("Done", 2, env));
        }

        EnvironmentModel saved = environmentRepository.save(env);
        log.info("Ambiente criado id={} por usuário id={}", saved.getId(), owner.getId());
        return EnvironmentResponseDTO.from(saved);
    }

    @Transactional
    public EnvironmentResponseDTO updateEnvironment(Integer envId, EnvironmentUpdateRequestDTO req, String userEmail) {
        EnvironmentModel env = requireMember(envId, userEmail);

        if (req.archived() != null && !req.archived().equals(env.getArchived())) {
            requireOwner(envId, userEmail);
            env.setArchived(req.archived());
        }
        if (req.name() != null) {
            env.setName(req.name().trim());
        }
        if (req.description() != null) {
            env.setDescription(req.description().trim());
        }
        if (req.position() != null) {
            env.setPosition(req.position());
        }
        if (req.type() != null) {
            env.setType(req.type());
        }

        EnvironmentModel saved = environmentRepository.save(env);
        log.info("Ambiente atualizado id={}", saved.getId());
        return EnvironmentResponseDTO.from(saved);
    }

    @Transactional
    public EnvironmentResponseDTO updateArchiveStatus(Integer envId, Boolean archived, String userEmail) {
        EnvironmentModel env = requireOwner(envId, userEmail);
        env.setArchived(archived);
        EnvironmentModel saved = environmentRepository.save(env);
        log.info("Status de arquivamento do ambiente id={} alterado para {}", envId, archived);
        return EnvironmentResponseDTO.from(saved);
    }

    @Transactional
    public void deleteEnvironment(Integer envId, String userEmail) {
        EnvironmentModel env = requireOwner(envId, userEmail);

        for (CardModel card : env.getCards()) {
            card.setColumn(null);
            card.setPriority(null);
            card.getUsers().clear();
        }
        cardRepository.saveAll(env.getCards());
        cardRepository.flush();

        for (UserModel member : List.copyOf(env.getUsers())) {
            env.removeUser(member);
        }
        environmentRepository.delete(env);
        log.info("Ambiente excluído id={}", envId);
    }

    @Transactional
    public UserEnvDTO addUser(Integer envId, Integer userId, String requesterEmail) {
        EnvironmentModel env = requireOwner(envId, requesterEmail);

        UserModel target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        boolean alreadyMember = env.getUsers().stream()
                .anyMatch(u -> u.getId().equals(target.getId()));
        if (alreadyMember) {
            throw new ApiException(HttpStatus.CONFLICT, "Esse usuário já faz parte do ambiente");
        }

        env.addUser(target);
        environmentRepository.save(env);
        log.info("Usuário id={} adicionado ao ambiente id={}", target.getId(), envId);
        return UserEnvDTO.from(target);
    }

    @Transactional
    public void removeUser(Integer envId, Integer userId, String requesterEmail) {
        EnvironmentModel env = requireOwner(envId, requesterEmail);

        if (env.getOwner().getId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O dono do ambiente não pode ser removido");
        }

        UserModel target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        boolean removed = env.removeUser(target);
        if (!removed) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Usuário não faz parte deste ambiente");
        }

        List<CardModel> cards = cardRepository.findByEnvironmentIdAndUsersContaining(envId, target);
        for (CardModel card : cards) {
            card.getUsers().removeIf(u -> u.getId().equals(target.getId()));
        }
        cardRepository.saveAll(cards);
        environmentRepository.save(env);
        log.info("Usuário id={} removido do ambiente id={}", target.getId(), envId);
    }

    public EnvironmentModel requireOwner(Integer envId, String userEmail) {
        UserModel user = userService.requireByEmail(userEmail);
        EnvironmentModel env = environmentRepository.findById(envId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ambiente não encontrado"));

        if (!env.getOwner().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Apenas o dono do ambiente pode realizar esta ação");
        }
        return env;
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
