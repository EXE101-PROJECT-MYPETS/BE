package com.exe101.userAddress.repository;

import com.exe101.userAddress.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IUserAddressRepository extends JpaRepository<UserAddress, Long> {

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    List<UserAddress> findByUserIdOrderByDefaultAddressDescIdDesc(Long userId);

    Optional<UserAddress> findFirstByUserIdOrderByDefaultAddressDescIdDesc(Long userId);

    Optional<UserAddress> findFirstByUserIdOrderByIdDesc(Long userId);

    boolean existsByUserId(Long userId);

    @Modifying
    @Query("""
            update UserAddress address
            set address.defaultAddress = false
            where address.userId = :userId
              and (:excludedId is null or address.id <> :excludedId)
            """)
    void clearDefaultAddress(
            @Param("userId") Long userId,
            @Param("excludedId") Long excludedId
    );
}
