package com.example.demo.service;

import com.example.demo.dto.CardMoveRequestDTO;
import com.example.demo.dto.CardRequestDTO;
import com.example.demo.dto.CardResponseDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.ColumnModel;
import com.example.demo.model.ColumnRepository;
import com.example.demo.model.EnvironmentModel;
import com.example.demo.model.PriorityModel;
import com.example.demo.model.PriorityRepository;
import com.example.demo.model.UserModel;
import com.example.demo.model.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final ColumnRepository columnRepository;
    private final PriorityRepository priorityRepository;
    private final UserRepository userRepository;
    private final EnvironmentService environmentService;

    public CardService(CardRepository cardRepository,
                       ColumnRepository columnRepository,
                       PriorityRepository priorityRepository,
                       UserRepository userRepository,
                       EnvironmentService environmentService) {
        this.cardRepository = cardRepository;
        this.columnRepository = columnRepository;
        this.priorityRepository = priorityRepository;
        this.userRepository = userRepository;
        this.environmentService = environmentService;
    }

    @Transactional(readOnly = true)
    public List<CardResponseDTO> listByEnvironment(Integer envId, Boolean backlogOnly, Boolean archived, String userEmail) {
        environmentService.requireMember(envId, userEmail);

        if (Boolean.TRUE.equals(backlogOnly)) {
            return cardRepository
                    .findByEnvironmentIdAndColumnIsNullAndArchivedFalseOrderByPositionAscIdAsc(envId)
                    .stream()
                    .map(CardResponseDTO::from)
                    .toList();
        }

        List<CardModel> cards = cardRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId);
        if (archived != null) {
            cards = cards.stream().filter(c -> archived.equals(c.getArchived())).toList();
        }
        return cards.stream().map(CardResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CardResponseDTO> listArchivedByEnvironment(Integer envId, String userEmail) {
        environmentService.requireMember(envId, userEmail);
        return cardRepository
                .findByEnvironmentIdAndArchivedTrueOrderByPositionAscIdAsc(envId)
                .stream()
                .map(CardResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponseDTO getById(Integer cardId, String userEmail) {
        CardModel card = requireCardWithAccess(cardId, userEmail);
        return CardResponseDTO.from(card);
    }

    @Transactional
    public CardResponseDTO create(Integer envId, CardRequestDTO req, String userEmail) {
        EnvironmentModel env = environmentService.requireMember(envId, userEmail);
        String label = req != null ? req.resolvedLabel() : null;
        if (label == null || label.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Título do card é obrigatório");
        }

        CardModel card = new CardModel();
        card.setEnvironment(env);
        card.setName(label);
        card.setDescription(req.description() != null && !req.description().isBlank() ? req.description().trim() : null);

        ColumnModel column = resolveColumn(req != null ? req.column() : null, envId);
        card.setColumn(column);

        PriorityModel priority = resolvePriority(req.priority(), envId);
        card.setPriority(priority);

        float progress = req.progress() != null ? req.progress() : 0.0f;
        validateProgress(progress);
        card.setProgress(progress);

        card.setFinalDate(parseDeadlineSafely(req));
        card.setDone(req.done() != null ? req.done() : false);
        card.setArchived(req.archived() != null ? req.archived() : false);

        if (req.users() != null) {
            card.setUsers(resolveUsers(req.users(), env));
        }

        int pos;
        if (req.position() != null) {
            pos = req.position();
        } else if (column != null) {
            pos = cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(column.getId()).size();
        } else {
            pos = cardRepository.findByEnvironmentIdAndColumnIsNullAndArchivedFalseOrderByPositionAscIdAsc(envId).size();
        }
        card.setPosition(pos);

        CardModel saved = cardRepository.save(card);
        return CardResponseDTO.from(saved);
    }

    @Transactional
    public Object update(Integer cardId, CardRequestDTO req, String userEmail) {
        CardModel card = requireCardWithAccess(cardId, userEmail);
        EnvironmentModel env = card.getEnvironment();
        Integer envId = env.getId();

        boolean wasArchived = Boolean.TRUE.equals(card.getArchived());
        Integer previousColumnId = card.getColumn() != null ? card.getColumn().getId() : null;

        boolean isFullUpdate = req != null && req.resolvedLabel() != null;

        if (isFullUpdate) {
            card.setName(req.resolvedLabel());
            card.setDescription(req.description() != null && !req.description().isBlank() ? req.description().trim() : null);
            card.setColumn(resolveColumn(req.column(), envId));
            card.setPriority(resolvePriority(req.priority(), envId));
            if (req.progress() != null) {
                validateProgress(req.progress());
                card.setProgress(req.progress());
            }
            card.setFinalDate(parseDeadlineSafely(req));
            if (req.users() != null) {
                card.setUsers(resolveUsers(req.users(), env));
            }
            if (req.done() != null) {
                card.setDone(req.done());
            }
            if (req.archived() != null) {
                card.setArchived(req.archived());
            }
            if (req.position() != null) {
                card.setPosition(req.position());
            }
        } else if (req != null) {
            if (req.description() != null) {
                card.setDescription(req.description().isBlank() ? null : req.description().trim());
            }
            if (req.column() != null) {
                card.setColumn(resolveColumn(req.column(), envId));
            }
            if (req.priority() != null) {
                card.setPriority(resolvePriority(req.priority(), envId));
            }
            if (req.progress() != null) {
                validateProgress(req.progress());
                card.setProgress(req.progress());
            }
            if (req.deadline() != null || req.finalDate() != null) {
                card.setFinalDate(parseDeadlineSafely(req));
            }
            if (req.users() != null) {
                card.setUsers(resolveUsers(req.users(), env));
            }
            if (req.done() != null) {
                card.setDone(req.done());
            }
            if (req.archived() != null) {
                card.setArchived(req.archived());
            }
            if (req.position() != null) {
                card.setPosition(req.position());
            }
        }

        boolean isNowArchived = Boolean.TRUE.equals(card.getArchived());
        Integer newColumnId = card.getColumn() != null ? card.getColumn().getId() : null;

        if (!Objects.equals(previousColumnId, newColumnId) && (req == null || req.position() == null)) {
            if (newColumnId != null) {
                int endPos = cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(newColumnId).size();
                card.setPosition(endPos);
            }
        }

        CardModel saved = cardRepository.save(card);

        // Re-index columns if archiving/restoring or moving column
        if (wasArchived != isNowArchived || !Objects.equals(previousColumnId, newColumnId)) {
            if (previousColumnId != null) {
                renumberActiveColumn(previousColumnId);
            }
            if (newColumnId != null && !Objects.equals(previousColumnId, newColumnId)) {
                renumberActiveColumn(newColumnId);
            }
        }

        // Match frontend contract: CardCRUD.archive (PUT /cards/{id} changing archived) expects CardEnv[],
        // while CardCRUD.edit (PUT /cards/{id} without changing archived) expects CardEnv.
        if (wasArchived != isNowArchived) {
            return cardRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId)
                    .stream()
                    .map(CardResponseDTO::from)
                    .toList();
        }
        return CardResponseDTO.from(saved);
    }

    @Transactional
    public List<CardResponseDTO> move(Integer cardId, CardMoveRequestDTO req, String userEmail) {
        CardModel card = requireCardWithAccess(cardId, userEmail);
        Integer envId = card.getEnvironment().getId();

        ColumnModel sourceColumn = card.getColumn();
        ColumnModel targetColumn = (req != null && req.column() != null)
                ? resolveColumn(req.column(), envId)
                : sourceColumn;

        Integer sourceColId = sourceColumn != null ? sourceColumn.getId() : null;
        Integer targetColId = targetColumn != null ? targetColumn.getId() : null;

        // If moving across columns, renumber source column without the moved card
        if (!Objects.equals(sourceColId, targetColId) && sourceColId != null) {
            List<CardModel> sourceCards = new ArrayList<>(
                    cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(sourceColId)
            );
            sourceCards.removeIf(c -> c.getId().equals(cardId));
            for (int i = 0; i < sourceCards.size(); i++) {
                sourceCards.get(i).setPosition(i);
            }
            cardRepository.saveAll(sourceCards);
        }

        // Build target list without the moved card
        List<CardModel> targetCards;
        if (targetColId != null) {
            targetCards = new ArrayList<>(
                    cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(targetColId)
            );
        } else {
            targetCards = new ArrayList<>(
                    cardRepository.findByEnvironmentIdAndColumnIsNullAndArchivedFalseOrderByPositionAscIdAsc(envId)
            );
        }
        targetCards.removeIf(c -> c.getId().equals(cardId));

        card.setColumn(targetColumn);

        Integer beforeId = req != null ? req.before() : null;
        if (beforeId != null) {
            int insertIndex = -1;
            for (int i = 0; i < targetCards.size(); i++) {
                if (targetCards.get(i).getId().equals(beforeId)) {
                    insertIndex = i;
                    break;
                }
            }
            if (insertIndex >= 0) {
                targetCards.add(insertIndex, card);
            } else {
                targetCards.add(card);
            }
        } else {
            targetCards.add(card);
        }

        for (int i = 0; i < targetCards.size(); i++) {
            targetCards.get(i).setPosition(i);
        }
        cardRepository.saveAll(targetCards);
        cardRepository.flush();

        return cardRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId)
                .stream()
                .map(CardResponseDTO::from)
                .toList();
    }

    @Transactional
    public List<CardResponseDTO> archiveCard(Integer cardId, Boolean done, String userEmail) {
        CardModel card = requireCardWithAccess(cardId, userEmail);
        Integer envId = card.getEnvironment().getId();
        Integer colId = card.getColumn() != null ? card.getColumn().getId() : null;

        card.setArchived(true);
        if (done != null) {
            card.setDone(done);
        }
        cardRepository.save(card);
        cardRepository.flush();

        if (colId != null) {
            renumberActiveColumn(colId);
        }

        return cardRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId)
                .stream()
                .map(CardResponseDTO::from)
                .toList();
    }

    @Transactional
    public List<CardResponseDTO> restoreCard(Integer cardId, Integer targetColumnId, String userEmail) {
        CardModel card = requireCardWithAccess(cardId, userEmail);
        Integer envId = card.getEnvironment().getId();

        ColumnModel targetColumn;
        if (targetColumnId != null) {
            targetColumn = resolveColumn(targetColumnId, envId);
        } else if (card.getColumn() != null && card.getColumn().getEnvironment().getId().equals(envId)) {
            targetColumn = card.getColumn();
        } else {
            List<ColumnModel> cols = columnRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId);
            if (cols.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Crie uma coluna no Kanban antes de restaurar o card");
            }
            targetColumn = cols.get(0);
        }

        card.setColumn(targetColumn);
        card.setArchived(false);
        int pos = cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(targetColumn.getId()).size();
        card.setPosition(pos);

        cardRepository.save(card);
        cardRepository.flush();

        return cardRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId)
                .stream()
                .map(CardResponseDTO::from)
                .toList();
    }

    @Transactional
    public void delete(Integer cardId, Boolean permanent, String userEmail) {
        CardModel card = requireCardWithAccess(cardId, userEmail);

        if (Boolean.TRUE.equals(permanent) || Boolean.TRUE.equals(card.getArchived())) {
            deletePermanentInternal(card);
        } else {
            // Exclusão convencional: soft delete (arquivamento lógico)
            Integer colId = card.getColumn() != null ? card.getColumn().getId() : null;
            card.setArchived(true);
            cardRepository.save(card);
            cardRepository.flush();
            if (colId != null) {
                renumberActiveColumn(colId);
            }
        }
    }

    @Transactional
    public void deletePermanent(Integer cardId, String userEmail) {
        CardModel card = requireCardWithAccess(cardId, userEmail);
        deletePermanentInternal(card);
    }

    private void deletePermanentInternal(CardModel card) {
        Integer columnId = card.getColumn() != null ? card.getColumn().getId() : null;
        EnvironmentModel env = card.getEnvironment();
        env.getCards().removeIf(c -> c.getId().equals(card.getId()));
        cardRepository.delete(card);
        cardRepository.flush();

        if (columnId != null) {
            renumberActiveColumn(columnId);
        }
    }

    private void renumberActiveColumn(Integer columnId) {
        List<CardModel> active = cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(columnId);
        for (int i = 0; i < active.size(); i++) {
            active.get(i).setPosition(i);
        }
        cardRepository.saveAll(active);
    }

    private ColumnModel resolveColumn(Integer columnId, Integer envId) {
        if (columnId == null) {
            return null;
        }
        ColumnModel column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Coluna não encontrada"));
        if (!column.getEnvironment().getId().equals(envId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A coluna não pertence a este ambiente");
        }
        return column;
    }

    private PriorityModel resolvePriority(Integer priorityId, Integer envId) {
        if (priorityId == null) {
            return null;
        }
        PriorityModel priority = priorityRepository.findById(priorityId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prioridade não encontrada"));
        if (!priority.getEnvironment().getId().equals(envId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A prioridade não pertence a este ambiente");
        }
        return priority;
    }

    private List<UserModel> resolveUsers(List<Integer> userIds, EnvironmentModel env) {
        List<UserModel> result = new ArrayList<>();
        for (Integer uid : userIds) {
            if (uid == null) continue;
            UserModel user = userRepository.findById(uid)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuário não encontrado: " + uid));
            boolean isMember = env.getOwner().getId().equals(uid)
                    || env.getUsers().stream().anyMatch(u -> u.getId().equals(uid));
            if (!isMember) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Usuário não faz parte deste ambiente: " + uid);
            }
            if (result.stream().noneMatch(u -> u.getId().equals(uid))) {
                result.add(user);
            }
        }
        return result;
    }

    private void validateProgress(float progress) {
        if (progress < 0f || progress > 100f) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Progresso deve estar entre 0 e 100");
        }
    }

    private java.time.LocalDate parseDeadlineSafely(CardRequestDTO req) {
        try {
            return req.resolvedDeadline();
        } catch (DateTimeParseException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Formato de data inválido");
        }
    }

    private CardModel requireCardWithAccess(Integer cardId, String userEmail) {
        CardModel card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Card não encontrado"));
        environmentService.requireMember(card.getEnvironment().getId(), userEmail);
        return card;
    }
}
