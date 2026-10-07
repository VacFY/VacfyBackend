package com.dev.vacfy.user.interfaces.acl;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collection;
import java.util.Map;

public interface ProfileContextFacade {
    ImmutablePair<Boolean, Exception> createProfile(String profileId, String profileDni);

    /** DNI y nombre de varios usuarios, por id (los que no existen no aparecen). */
    Map<String, ProfileContactData> getContactData(Collection<String> userIds);
}
