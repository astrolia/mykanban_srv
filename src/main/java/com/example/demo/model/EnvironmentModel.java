package com.example.demo.model;

import com.example.demo.model.enums.EnvironmentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "environments")
@Getter
@Setter
@NoArgsConstructor
public class EnvironmentModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Nome do ambiente é obrigatório")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description = "";

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer position = 0;

    @NotNull
    @Column(nullable = false)
    private Boolean archived = false;

    @Enumerated(EnumType.STRING)
    private EnvironmentType type = EnvironmentType.DEFAULT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserModel owner;

    @ManyToMany
    @JoinTable(
            name = "environment_users",
            joinColumns = @JoinColumn(name = "environment_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<UserModel> users = new ArrayList<>();

    @OneToMany(mappedBy = "environment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<ColumnModel> columns = new ArrayList<>();

    @OneToMany(mappedBy = "environment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<CardModel> cards = new ArrayList<>();

    @OneToMany(mappedBy = "environment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PriorityModel> priorities = new ArrayList<>();

    public void addUser(UserModel user) {
        if (this.users.stream().noneMatch(u -> Objects.equals(u.getId(), user.getId()))) {
            this.users.add(user);
        }
        if (user.getEnvironments().stream()
                .noneMatch(e -> e == this || (e.getId() != null && Objects.equals(e.getId(), this.id)))) {
            user.getEnvironments().add(this);
        }
    }

    public boolean removeUser(UserModel user) {
        boolean removed = this.users.removeIf(u -> Objects.equals(u.getId(), user.getId()));
        user.getEnvironments().removeIf(e -> e == this || (e.getId() != null && Objects.equals(e.getId(), this.id)));
        return removed;
    }
}
