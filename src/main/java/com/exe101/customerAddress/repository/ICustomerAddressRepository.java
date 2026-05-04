package com.exe101.customerAddress.repository;

import com.exe101.customerAddress.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ICustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerIdOrderByDefaultAddressDescIdDesc(Long customerId);

    List<CustomerAddress> findByCustomerIdInOrderByDefaultAddressDescIdDesc(List<Long> customerIds);

    Optional<CustomerAddress> findByIdAndCustomerId(Long id, Long customerId);

    Optional<CustomerAddress> findFirstByCustomerIdOrderByDefaultAddressDescIdDesc(Long customerId);

    Optional<CustomerAddress> findFirstByCustomerIdOrderByIdDesc(Long customerId);

    boolean existsByCustomerId(Long customerId);

    @Modifying
    @Query("""
            update CustomerAddress address
            set address.defaultAddress = false
            where address.customerId = :customerId
              and (:excludedId is null or address.id <> :excludedId)
            """)
    void clearDefaultAddress(
            @Param("customerId") Long customerId,
            @Param("excludedId") Long excludedId
    );
}
