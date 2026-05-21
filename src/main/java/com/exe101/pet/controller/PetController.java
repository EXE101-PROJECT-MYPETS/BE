package com.exe101.pet.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.pet.dto.PetDTO;
import com.exe101.pet.exception.PetAccessDenied;
import com.exe101.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @GetMapping
    public ResponseEntity<List<PetDTO>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(petService.getAllByUserId(getCurrentUserId(principal)));
    }

    @GetMapping("/user")
    public ResponseEntity<List<PetDTO>> getAllByUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(petService.getAllByUserId(getCurrentUserId(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetDTO> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(petService.getByIdForUser(id, getCurrentUserId(principal)));
    }

    @PostMapping
    public ResponseEntity<PetDTO> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PetDTO dto
    ) {
        return ResponseEntity.ok(petService.createForUser(dto, getCurrentUserId(principal)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetDTO> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PetDTO dto
    ) {
        return ResponseEntity.ok(petService.updateForUser(id, dto, getCurrentUserId(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        petService.deleteForUser(id, getCurrentUserId(principal));
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new PetAccessDenied("AuthenticatedUserRequired", "Cần đăng nhập để thực hiện chức năng này");
        }
        return principal.getUser().getId();
    }
}
