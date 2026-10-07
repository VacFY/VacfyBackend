package com.dev.vacfy.user.interfaces.acl;

/**
 * Datos de contacto de un usuario para otros módulos.
 *
 * @param fullName nombre y apellido, o null si el perfil todavía dice "Undefined"
 */
public record ProfileContactData(String userId, String dni, String fullName) { }
