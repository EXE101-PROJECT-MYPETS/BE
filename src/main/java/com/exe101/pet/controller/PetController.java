package com.exe101.pet.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.pet.dto.PetDTO;
import com.exe101.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @GetMapping
    public ResponseEntity<List<PetDTO>> getAll() {
        return ResponseEntity.ok(petService.getAll());
    }

    @GetMapping("/my")
    public ResponseEntity<List<PetDTO>> getMyPets(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(petService.getAllByUserId(userId));
    }

    @GetMapping("/customer")
    public ResponseEntity<List<PetDTO>> getAllByCustomer(Authentication authentication) {
        // Alias for backward compatibility with old mobile route.
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(petService.getAllByUserId(userId));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<PetDTO> getMyPetById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(petService.getByIdForUser(id, userId));
    }

    @PostMapping(value = {"", "/my"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetDTO> create(
            Authentication authentication,
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @ModelAttribute PetDTO dto
    ) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(petService.createForUser(userId, shopId, dto));
    }

    @PutMapping(value = {"/{id}", "/my/{id}"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PetDTO> update(
            Authentication authentication,
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @PathVariable Long id,
            @ModelAttribute PetDTO dto
    ) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(petService.updateForUser(id, userId, shopId, dto));
    }

    @DeleteMapping({"/{id}", "/my/{id}"})
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long userId = resolveUserId(authentication);
        petService.deleteForUser(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/my/{id}/shops/{shopId}")
    public ResponseEntity<Void> linkPetToShop(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long shopId
    ) {
        Long userId = resolveUserId(authentication);
        petService.linkPetToShop(userId, id, shopId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Không xác định được người dùng hiện tại");
        }
        return principal.getUser().getId();
    }
}
