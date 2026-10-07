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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/profile", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Profile", description = "Profile Endpoints")
public class ProfileController {
    private final ProfileCommandService profileCommandService;
    private final ProfileQueryService profileQueryService;

    public ProfileController(ProfileCommandService profileCommandService, ProfileQueryService profileQueryService) {
        this.profileCommandService = profileCommandService;
        this.profileQueryService = profileQueryService;
    }

    @GetMapping
    @Operation(summary = "Get your profile", description = "Get your profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully."),
            @ApiResponse(responseCode = "404", description = "Profile not found."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
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
    @Operation(summary = "Update DNI", description = "Update DNI")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DNI updated"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<Void> updateDni(@RequestBody UpdateDniResource updateDniResource, HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var updateDniCommand = UpdateDniCommandFromResourceAssembler.toCommandFromResource(profileId, updateDniResource);
        profileCommandService.handle(updateDniCommand);
        return ResponseEntity.ok().build();
    }

    @PutMapping
    @Operation(summary = "Update profile information", description = "Update profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<Void> updateProfile(@RequestBody UpdateProfileResource updateProfileResource, HttpServletRequest request) {
        String profileId = request.getAttribute("userId").toString();
        var updateProfileCommand = UpdateProfileCommandFromResourceAssembler.toCommandFromResource(profileId, updateProfileResource);
        profileCommandService.handle(updateProfileCommand);
        return ResponseEntity.ok().build();
    }
}
