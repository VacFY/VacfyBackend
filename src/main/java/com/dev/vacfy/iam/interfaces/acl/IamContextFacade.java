package com.dev.vacfy.iam.interfaces.acl;

import org.apache.commons.lang3.tuple.ImmutablePair;

public interface IamContextFacade {
    ImmutablePair<Boolean, Exception> changeCredentialDni(String userId, String userDni);
}
