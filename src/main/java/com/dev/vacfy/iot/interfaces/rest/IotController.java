package com.dev.vacfy.iot.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/iot", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Iot", description = "Iot Endpoints")
public class IotController {
}
