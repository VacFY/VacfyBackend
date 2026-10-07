package com.dev.vacfy.user.domain.services;

import com.dev.vacfy.user.domain.model.commands.CreateProfileCommand;
import com.dev.vacfy.user.domain.model.commands.UpdateDniCommand;
import com.dev.vacfy.user.domain.model.commands.UpdateProfileCommand;
import org.apache.commons.lang3.tuple.ImmutablePair;

public interface ProfileCommandService {
    ImmutablePair<Boolean, Exception> handle(CreateProfileCommand createProfileCommand);
    void handle(UpdateDniCommand updateDniCommand);
    void handle(UpdateProfileCommand updateProfileCommand);
}
