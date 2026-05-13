package com.exe101.userAddress.repository;

import com.exe101.userAddress.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IUserAddressRepository extends JpaRepository<UserAddress, Long> {

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    List<UserAddress> findByUserIdOrderByDefaultAddressDescIdDesc(Long userId);

    Optional<UserAddress> findFirstByUserIdOrderByDefaultAddressDescIdDesc(Long userId);
}
