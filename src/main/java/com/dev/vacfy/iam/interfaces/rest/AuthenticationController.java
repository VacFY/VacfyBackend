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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "Authentication", description = "Authentication Endpoints")
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
    @Operation(summary = "Sign up a new user", description = "Sign up a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully."),
            @ApiResponse(responseCode = "400", description = "Bad request.")
    })
    public ResponseEntity<Void> signUp(@RequestBody SignUpResource signUpResource, HttpServletResponse response) {
        var signUpCommand = SignUpCommandFromResourceAssembler.toCommandFromResource(signUpResource);
        var token = credentialCommandService.handle(signUpCommand);
        if (token.isEmpty()) return ResponseEntity.badRequest().build();
        cookieService.setTokenCookie(response, token.get());
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Sign in with a user", description = "Sign in with a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User signed in successfully."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    public ResponseEntity<Void> signIn(@RequestBody SignInResource signInResource, HttpServletResponse response) {
        var signInCommand = SignInCommandFromResourceAssembler.toCommandFromResource(signInResource);
        var token = credentialCommandService.handle(signInCommand);
        if (token.isEmpty()) return ResponseEntity.badRequest().build();
        cookieService.setTokenCookie(response, token.get());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Resets password", description = "Resets the user's password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The password was reset."),
            @ApiResponse(responseCode = "404", description = "User not found.")
    })
    public ResponseEntity<Void> updatePassword(@RequestBody UpdatePasswordResource updatePasswordResource, HttpServletRequest request) {
        String userId = request.getAttribute("userId").toString();
        var updatePasswordCommand = UpdatePasswordCommandFromResourceAssembler.toCommandFromResource(userId, updatePasswordResource);
        credentialCommandService.handle(updatePasswordCommand);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sign-out")
    @Operation(summary = "Sign Out", description = "Sign Out of an account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User sign out in successfully."),
            @ApiResponse(responseCode = "400", description = "Bad request.")
    })
    public ResponseEntity<Void> signOut(HttpServletRequest request, HttpServletResponse response) {
        String token = request.getAttribute("opaqueToken").toString();
        if (token.isEmpty()) return ResponseEntity.badRequest().build();
        opaqueTokenService.revokeToken(token);
        cookieService.clearTokenCookie(response);
        return ResponseEntity.ok().build();
    }
}