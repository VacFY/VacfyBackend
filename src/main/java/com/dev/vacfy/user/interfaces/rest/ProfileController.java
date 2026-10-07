package com.dev.vacfy.user.interfaces.rest;

import com.dev.vacfy.user.domain.model.queries.GetProfileByIdQuery;
import com.dev.vacfy.user.domain.services.ProfileCommandService;
import com.dev.vacfy.user.domain.services.ProfileQueryService;
import com.dev.vacfy.user.interfaces.rest.resources.ProfileResource;
import com.dev.vacfy.user.interfaces.rest.resources.UpdateDniResource;
import com.dev.vacfy.user.interfaces.rest.resources.UpdateProfileResource;
import com.dev.vacfy.user.interfaces.rest.transform.ProfileResourceFromEntityAssembler;
import com.dev.vacfy.user.interfaces.rest.transform.UpdateDniCommandFromResourceAssembler;
import com.dev.vacfy.user.interfaces.rest.transform.UpdateProfileCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/profile", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Profile", description = "Datos del usuario con sesión")
public class ProfileController {
    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;

    public ProfileController(ProfileCommandService profileCommandService, ProfileQueryService profileQueryService) {
        this.profileCommandService = profileCommandService;
        this.profileQueryService = profileQueryService;
    }

    @GetMapping
    @Operation(summary = "Ver mi perfil",
            description = "Sirve también para saber si hay sesión: 200 = hay sesión, 401 = no. "
                    + "Un usuario recién registrado tiene \"Undefined\" en nombre, apellido y empresa: trátalo como vacío.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil del usuario"),
            @ApiResponse(responseCode = "404", description = "No tiene perfil", content = @Content),
            @ApiResponse(responseCode = "401", description = "No hay sesión", content = @Content)
    })
    public ResponseEntity<ProfileResource> getProfile(HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var getProfileByIdQuery = new GetProfileByIdQuery(profileId);
        var profile = profileQueryService.handle(getProfileByIdQuery);
        if (profile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var profileResource = ProfileResourceFromEntityAssembler.toResourceFromEntity(profile.get());
        return ResponseEntity.ok(profileResource);
    }

    @PatchMapping("/dni")
    @Operation(summary = "Cambiar mi DNI", description = "Cambia el DNI del perfil y también el que se usa para iniciar sesión.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"profileDni\": \"87654321\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DNI cambiado"),
            @ApiResponse(responseCode = "401", description = "No hay sesión"),
            @ApiResponse(responseCode = "500", description = "DNI vacío o ya usado por otra cuenta (todavía sin mensaje)")
    })
    public ResponseEntity<Void> updateDni(@RequestBody UpdateDniResource updateDniResource, HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var updateDniCommand = UpdateDniCommandFromResourceAssembler.toCommandFromResource(profileId, updateDniResource);
        profileCommandService.handle(updateDniCommand);
        return ResponseEntity.ok().build();
    }

    @PutMapping
    @Operation(summary = "Editar mi perfil",
            description = "Reemplaza nombre, apellido y empresa. Los tres son obligatorios y no pueden ir vacíos: "
                    + "para cambiar uno, envía los otros con su valor actual.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples =
            @ExampleObject(value = "{\"profileName\": \"Ana\", \"profileLastName\": \"Quispe\", \"profileCompany\": \"Posta Santa Rosa\"}")))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "401", description = "No hay sesión"),
            @ApiResponse(responseCode = "500", description = "Falta un campo o está vacío (todavía sin mensaje)")
    })
    public ResponseEntity<Void> updateProfile(@RequestBody UpdateProfileResource updateProfileResource, HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var updateProfileCommand = UpdateProfileCommandFromResourceAssembler.toCommandFromResource(profileId, updateProfileResource);
        profileCommandService.handle(updateProfileCommand);
        return ResponseEntity.ok().build();
    }
}
