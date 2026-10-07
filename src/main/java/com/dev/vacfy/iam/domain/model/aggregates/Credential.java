package com.dev.vacfy.iam.domain.model.aggregates;

import com.dev.vacfy.iam.domain.model.values.UserDni;
import com.dev.vacfy.iam.domain.model.values.UserId;
import com.dev.vacfy.iam.domain.model.values.UserPassword;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    public Credential() {}

    public Credential(String userDni, String userPassword) {
        this.userId = new UserId();
        this.userDni = new UserDni(userDni);
        this.userPassword = new UserPassword(userPassword);
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
