package com.dev.vacfy.user.interfaces.rest.transform;

import com.dev.vacfy.user.domain.model.aggregates.Profile;
import com.dev.vacfy.user.interfaces.rest.resources.ProfileResource;

public class ProfileResourceFromEntityAssembler {
    public static ProfileResource toResourceFromEntity(Profile entity) {
        return new ProfileResource(entity.getProfileDni().profileDni(), entity.getProfileName().profileName(), entity.getProfileLastName().profileLastName(), entity.getProfileCompany().profileCompany());
    }
}
