package com.exe101.customer.service;

import com.exe101.common.IService;
import com.exe101.customer.dto.CustomerDTO;
import com.exe101.customer.entity.Customer;
import com.exe101.customer.exception.CustomerNotFound;
import com.exe101.customer.mapper.CustomerMapper;
import com.exe101.customer.repository.ICustomerRepository;
import com.exe101.user.entity.User;
import com.exe101.user.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService implements IService<Customer, CustomerDTO, Long> {

    private static final int MAX_SUGGEST_LIMIT = 20;

    private final ICustomerRepository customerRepository;
    private final IUserRepository userRepository;

    @Override
    public List<CustomerDTO> getAll() {
        return customerRepository.findAll().stream().map(CustomerMapper::toDTO).toList();
    }

    public List<CustomerDTO> getAllByShopId(Long shopId) {
        return customerRepository.findByShopIdOrderByIdDesc(shopId).stream()
                .map(CustomerMapper::toDTO)
                .toList();
    }

    public CustomerDTO verifyByPhone(Long shopId, String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }

        String normalizedPhone = phone.trim();
        return customerRepository.findByShopIdAndPhone(shopId, normalizedPhone)
                .map(CustomerMapper::toDTO)
                .orElseThrow(() -> new CustomerNotFound(
                        "CustomerPhoneNotFound",
                        "Không tìm thấy khách hàng với số điện thoại này"
                ));
    }

    public List<CustomerDTO> suggestByPhone(Long shopId, String phone, int limit) {
        if (phone == null || phone.isBlank()) {
            return List.of();
        }

        int normalizedLimit = Math.min(Math.max(limit, 1), MAX_SUGGEST_LIMIT);
        String normalizedPhone = phone.trim();

        return customerRepository.findByShopIdAndPhoneStartingWithOrderByIdDesc(
                        shopId,
                        normalizedPhone,
                        PageRequest.of(0, normalizedLimit)
                ).stream()
                .map(CustomerMapper::toDTO)
                .toList();
    }

    @Override
    public CustomerDTO getById(Long id) {
        return customerRepository.findById(id)
                .map(CustomerMapper::toDTO)
                .orElseThrow(() -> new CustomerNotFound("CustomerNotFound", "Không tìm thấy khách hàng"));
    }

    @Override
    public CustomerDTO create(CustomerDTO dto) {
        return CustomerMapper.toDTO(customerRepository.save(CustomerMapper.toEntity(dto)));
    }

    @Override
    public CustomerDTO update(Long id, CustomerDTO dto) {
        Customer entity = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFound("CustomerNotFound", "Không tìm thấy khách hàng"));
        CustomerMapper.updateEntity(entity, dto);
        return CustomerMapper.toDTO(customerRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFound("CustomerNotFound", "Không tìm thấy khách hàng");
        }
        customerRepository.deleteById(id);
    }

    @Transactional
    public Customer getOrCreateCustomerForUser(Long shopId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được để trống");
        }
        return customerRepository.findFirstByShopIdAndUserIdOrderByIdDesc(shopId, userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CustomerNotFound("UserNotFoundForCustomer", "Không tìm thấy người dùng với ID: " + userId));
                    Customer customer = new Customer();
                    customer.setShopId(shopId);
                    customer.setUserId(userId);
                    customer.setFullName(user.getFullName());
                    customer.setPhone(user.getPhone());
                    customer.setEmail(user.getEmail());
                    return customerRepository.save(customer);
                });
    }
}
