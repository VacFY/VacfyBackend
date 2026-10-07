package com.dev.vacfy.iam.infrastructure.persistence.redis.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisRepositoryImpl<T> implements RedisRepository<T> {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisRepositoryImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String key, T value) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue);
        } catch (Exception e) {
            throw new RuntimeException("Error saving to Redis", e);
        }
    }

    @Override
    public void save(String key, T value, long timeout, TimeUnit unit) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, timeout, unit);
        } catch (Exception e) {
            throw new RuntimeException("Error saving with expiration in Redis", e);
        }
    }

    @Override
    public Optional<T> findByKey(String key, Class<T> clazz) {
        try {
            String jsonValue = redisTemplate.opsForValue().get(key);
            if (jsonValue == null) {
                return Optional.empty();
            }
            T object = objectMapper.readValue(jsonValue, clazz);
            return Optional.of(object);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving data from Redis", e);
        }
    }

    @Override
    public boolean existsByKey(String key) {
        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying the existence of the key in Redis", e);
        }
    }

    @Override
    public boolean delete(String key) {
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }
}