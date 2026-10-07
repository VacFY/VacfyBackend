package com.dev.vacfy.user.domain.model.aggregates;

import com.dev.vacfy.user.domain.model.values.*;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "profiles")
@Getter
public class Profile {
    @EmbeddedId
    private ProfileId profileId;

    @Embedded
    private ProfileDni profileDni;

    @Embedded
    private ProfileName profileName;

    @Embedded
    private ProfileLastName profileLastName;

    @Embedded
    private ProfileCompany profileCompany;

    public Profile() {}

    public Profile(String profileId, String profileDni) {
        this.profileId = new ProfileId(UUID.fromString(profileId));
        this.profileDni = new ProfileDni(profileDni);
        this.profileName = new ProfileName("Undefined");
        this.profileLastName = new ProfileLastName("Undefined");
        this.profileCompany = new ProfileCompany("Undefined");
    }

    public Profile updateProfileDni(String profileDni) {
        this.profileDni = new ProfileDni(profileDni);
        return this;
    }

    public Profile updateProfile(String profileName, String profileLastName, String profileCompany) {
        this.profileName = new ProfileName(profileName);
        this.profileLastName = new ProfileLastName(profileLastName);
        this.profileCompany = new ProfileCompany(profileCompany);
        return this;
    }
}
