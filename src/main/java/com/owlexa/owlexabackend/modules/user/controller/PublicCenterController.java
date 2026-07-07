package com.owlexa.owlexabackend.modules.user.controller;

import com.owlexa.owlexabackend.common.filter.DomainResolverFilter;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public endpoint để guest xem thông tin center.
 * DomainResolverFilter đã set attribute "resolvedCenterId" từ subdomain.
 * Endpoint này không cần JWT.
 */
@RestController
@RequestMapping("/public/centers")
@RequiredArgsConstructor
public class PublicCenterController {

    private final CenterRepository centerRepository;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentCenter(
            @RequestAttribute(value = DomainResolverFilter.ATTR_RESOLVED_CENTER_ID, required = false) Long centerId
    ) {
        if (centerId == null) {
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "No center context for this request"
            ));
        }

        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new com.owlexa.owlexabackend.common.exception.ResourceNotFoundException(
                        "Center not found"
                ));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 200);
        body.put("data", Map.of(
                "id", center.getId(),
                "name", center.getName(),
                "subdomain", center.getSubdomain()
        ));

        return ResponseEntity.ok(body);
    }
}