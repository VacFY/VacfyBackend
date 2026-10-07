package com.dev.vacfy.iam.interfaces.rest;

import com.dev.vacfy.iam.domain.services.CredentialCommandService;
import com.dev.vacfy.iam.infrastructure.cookies.CookieService;
import com.dev.vacfy.iam.infrastructure.tokens.opaque.OpaqueTokenService;
import com.dev.vacfy.iam.interfaces.rest.resources.UpdatePasswordResource;
import com.dev.vacfy.iam.interfaces.rest.resources.SignInResource;
import com.dev.vacfy.iam.interfaces.rest.resources.SignUpResource;
import com.dev.vacfy.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import com.dev.vacfy.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.dev.vacfy.iam.interfaces.rest.transform.UpdatePasswordCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Registro, inicio y cierre de sesión (cookie HttpOnly)")
public class AuthenticationController {
    private final CredentialCommandService credentialCommandService;
    private final OpaqueTokenService opaqueTokenService;
    private final CookieService cookieService;

    public AuthenticationController(CredentialCommandService credentialCommandService, OpaqueTokenService opaqueTokenService, CookieService cookieService) {
        this.credentialCommandService = credentialCommandService;
        this.opaqueTokenService = opaqueTokenService;
        this.cookieService = cookieService;
    }

    @PostMapping("/sign-up")
    @SecurityRequirements
    @Operation(summary = "Crear cuenta e iniciar sesión",
            description = """
                    Crea la cuenta y deja la sesión iniciada: responde **201 sin cuerpo** con la cookie `access-token`.
                    También crea el perfil, con nombre, apellido y empresa en "Undefined" (complétalo con PUT /api/v1/profile).

                    El backend no valida la contraseña: el front debe exigir que no esté vacía (máximo 72 bytes).""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"userDni\": \"12345678\", \"userPassword\": \"miClave123\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cuenta creada; la cookie de sesión viene en Set-Cookie"),
            @ApiResponse(responseCode = "400", description = "JSON mal formado"),
            @ApiResponse(responseCode = "500", description = "DNI ya registrado o vacío (todavía sin mensaje)")
    })
    public ResponseEntity<Void> signUp(@RequestBody SignUpResource signUpResource, HttpServletResponse response) {
        var signUpCommand = SignUpCommandFromResourceAssembler.toCommandFromResource(signUpResource);
        var token = credentialCommandService.handle(signUpCommand);
        if (token.isEmpty()) return ResponseEntity.badRequest().build();
        cookieService.setTokenCookie(response, token.get());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión",
            description = """
                    Responde **200 sin cuerpo** y deja la cookie `access-token` (HttpOnly, 12 h). Desde Swagger, después \
                    de esto ya puedes probar los endpoints con candado.

                    Desde el front, todas las peticiones deben ir con credenciales: \
                    `fetch(url, { credentials: 'include' })` o `axios` con `withCredentials: true`.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"userDni\": \"12345678\", \"userPassword\": \"miClave123\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión iniciada; la cookie viene en Set-Cookie"),
            @ApiResponse(responseCode = "400", description = "JSON mal formado"),
            @ApiResponse(responseCode = "500", description = "DNI o contraseña incorrectos (todavía sin mensaje)")
    })
    public ResponseEntity<Void> signIn(@RequestBody SignInResource signInResource, HttpServletResponse response) {
        var signInCommand = SignInCommandFromResourceAssembler.toCommandFromResource(signInResource);
        var token = credentialCommandService.handle(signInCommand);
        if (token.isEmpty()) return ResponseEntity.badRequest().build();
        cookieService.setTokenCookie(response, token.get());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Cambiar contraseña",
            description = "Cambia la contraseña del usuario con sesión; pide la actual. La sesión sigue abierta. "
                    + "No es una recuperación por correo.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"currentPassword\": \"miClave123\", \"newPassword\": \"otraClave456\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contraseña cambiada"),
            @ApiResponse(responseCode = "401", description = "No hay sesión"),
            @ApiResponse(responseCode = "500", description = "La contraseña actual no coincide (todavía sin mensaje)")
    })
    public ResponseEntity<Void> updatePassword(@RequestBody UpdatePasswordResource updatePasswordResource, HttpServletRequest request) {
        String userId = request.getAttribute("userId").toString();
        var updatePasswordCommand = UpdatePasswordCommandFromResourceAssembler.toCommandFromResource(userId, updatePasswordResource);
        credentialCommandService.handle(updatePasswordCommand);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sign-out")
    @Operation(summary = "Cerrar sesión", description = "Cierra la sesión en el servidor y borra la cookie.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sesión cerrada"),
            @ApiResponse(responseCode = "401", description = "No había sesión")
    })
    public ResponseEntity<Void> signOut(HttpServletRequest request, HttpServletResponse response) {
        String token = request.getAttribute("opaqueToken").toString();
        if (token.isEmpty()) return ResponseEntity.badRequest().build();
        opaqueTokenService.revokeToken(token);
        cookieService.clearTokenCookie(response);
        return ResponseEntity.ok().build();
    }
}