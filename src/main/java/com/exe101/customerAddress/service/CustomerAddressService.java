package com.exe101.customerAddress.service;

import com.exe101.customer.entity.Customer;
import com.exe101.customer.exception.CustomerNotFound;
import com.exe101.customer.repository.ICustomerRepository;
import com.exe101.customerAddress.dto.CustomerAddressDTO;
import com.exe101.customerAddress.entity.CustomerAddress;
import com.exe101.customerAddress.exception.CustomerAddressAccessDenied;
import com.exe101.customerAddress.exception.CustomerAddressNotFound;
import com.exe101.customerAddress.mapper.CustomerAddressMapper;
import com.exe101.customerAddress.repository.ICustomerAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final ICustomerAddressRepository customerAddressRepository;
    private final ICustomerRepository customerRepository;

    public List<CustomerAddressDTO> getAllByCustomerId(Long shopId, Long customerId) {
        assertCustomerBelongsToShop(shopId, customerId);
        return customerAddressRepository.findByCustomerIdOrderByDefaultAddressDescIdDesc(customerId).stream()
                .map(CustomerAddressMapper::toDTO)
                .toList();
    }

    public CustomerAddressDTO getById(Long shopId, Long id) {
        CustomerAddress address = findAddressInShop(shopId, id);
        return CustomerAddressMapper.toDTO(address);
    }

    @Transactional
    public CustomerAddressDTO create(Long shopId, CustomerAddressDTO dto) {
        assertCustomerBelongsToShop(shopId, dto.getCustomerId());

        CustomerAddress entity = CustomerAddressMapper.toEntity(dto);
        if (Boolean.TRUE.equals(entity.getDefaultAddress())
                || !customerAddressRepository.existsByCustomerId(entity.getCustomerId())) {
            customerAddressRepository.clearDefaultAddress(entity.getCustomerId(), null);
            entity.setDefaultAddress(true);
        }

        return CustomerAddressMapper.toDTO(customerAddressRepository.save(entity));
    }

    @Transactional
    public CustomerAddressDTO update(Long shopId, Long id, CustomerAddressDTO dto) {
        CustomerAddress entity = findAddressInShop(shopId, id);
        if (!Objects.equals(entity.getCustomerId(), dto.getCustomerId())) {
            throw new CustomerAddressAccessDenied(
                    "CustomerAddressCustomerMismatch",
                    "Không được chuyển địa chỉ sang khách hàng khác"
            );
        }

        boolean shouldBeDefault = Boolean.TRUE.equals(dto.getDefaultAddress());
        if (shouldBeDefault) {
            customerAddressRepository.clearDefaultAddress(entity.getCustomerId(), id);
        }

        CustomerAddressMapper.updateEntity(entity, dto);
        return CustomerAddressMapper.toDTO(customerAddressRepository.save(entity));
    }

    @Transactional
    public void delete(Long shopId, Long id) {
        CustomerAddress entity = findAddressInShop(shopId, id);
        boolean wasDefault = Boolean.TRUE.equals(entity.getDefaultAddress());
        Long customerId = entity.getCustomerId();

        customerAddressRepository.delete(entity);

        if (wasDefault) {
            customerAddressRepository.findFirstByCustomerIdOrderByIdDesc(customerId)
                    .ifPresent(nextDefault -> {
                        nextDefault.setDefaultAddress(true);
                        customerAddressRepository.save(nextDefault);
                    });
        }
    }

    private CustomerAddress findAddressInShop(Long shopId, Long id) {
        CustomerAddress address = customerAddressRepository.findById(id)
                .orElseThrow(() -> new CustomerAddressNotFound(
                        "CustomerAddressNotFound",
                        "Không tìm thấy địa chỉ nhận hàng"
                ));
        assertCustomerBelongsToShop(shopId, address.getCustomerId());
        return address;
    }

    private Customer assertCustomerBelongsToShop(Long shopId, Long customerId) {
        return customerRepository.findByShopIdAndId(shopId, customerId)
                .orElseThrow(() -> new CustomerNotFound(
                        "CustomerNotFound",
                        "Không tìm thấy khách hàng trong shop"
                ));
    }
}
