package com.dev.vacfy.iam.domain.model.aggregates;

import com.dev.vacfy.iam.domain.model.values.UserDni;
import com.dev.vacfy.iam.domain.model.values.UserId;
import com.dev.vacfy.iam.domain.model.values.UserPassword;
import com.dev.vacfy.iam.domain.model.values.UserRole;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "credentials")
@Getter
public class Credential {
    @EmbeddedId
    private UserId userId;

    @Embedded
    private UserDni userDni;

    @Embedded
    private UserPassword userPassword;

    /** null en cuentas creadas antes de los roles: cuenta como ENFERMERA. SUPERVISOR se asigna en la base de datos. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private UserRole role;

    public Credential() {}

    public Credential(String userDni, String userPassword) {
        this.userId = new UserId();
        this.userDni = new UserDni(userDni);
        this.userPassword = new UserPassword(userPassword);
        this.role = UserRole.ENFERMERA;
    }

    public UserRole getRole() {
        return role == null ? UserRole.ENFERMERA : role;
    }

    public Credential updatePassword(String userPassword) {
        this.userPassword = new UserPassword(userPassword);
        return this;
    }

    public Credential updateDni(String userDni) {
        this.userDni = new UserDni(userDni);
        return this;
    }
}
