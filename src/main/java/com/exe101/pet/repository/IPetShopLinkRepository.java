package com.exe101.pet.repository;

import com.exe101.pet.entity.PetShopLink;
import com.exe101.pet.entity.PetShopLink.PetShopLinkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPetShopLinkRepository extends JpaRepository<PetShopLink, PetShopLinkId> {
    boolean existsByPetIdAndShopId(Long petId, Long shopId);
}
