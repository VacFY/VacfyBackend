package com.dev.vacfy.iam.infrastructure.persistence.redis.repositories;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface RedisRepository<T> {

    void save(String key, T value);

    void save(String key, T value, long timeout, TimeUnit unit);

    Optional<T> findByKey(String key, Class<T> clazz);

    boolean existsByKey(String key);

    boolean delete(String key);
}