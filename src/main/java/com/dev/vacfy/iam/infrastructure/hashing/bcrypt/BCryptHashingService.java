package com.dev.vacfy.iam.infrastructure.hashing.bcrypt;

import com.dev.vacfy.iam.application.internal.outboundservices.hashing.HashingService;
import org.springframework.security.crypto.password.PasswordEncoder;

public interface BCryptHashingService extends HashingService, PasswordEncoder {
}