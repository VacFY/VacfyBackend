package com.dev.vacfy.user.interfaces.acl;

import org.apache.commons.lang3.tuple.ImmutablePair;

public interface ProfileContextFacade {
    ImmutablePair<Boolean, Exception> createProfile(String profileId, String profileDni);
}
