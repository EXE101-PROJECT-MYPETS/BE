package com.exe101.customer.service;

import com.exe101.common.IService;
import com.exe101.customer.dto.CustomerDTO;
import com.exe101.customer.entity.Customer;
import com.exe101.customer.exception.CustomerNotFound;
import com.exe101.customer.mapper.CustomerMapper;
import com.exe101.customer.repository.ICustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService implements IService<Customer, CustomerDTO, Long> {

    private final ICustomerRepository customerRepository;

    @Override
    public List<CustomerDTO> getAll() {
        return customerRepository.findAll().stream().map(CustomerMapper::toDTO).toList();
    }

    @Override
    public CustomerDTO getById(Long id) {
        return customerRepository.findById(id)
                .map(CustomerMapper::toDTO)
                .orElseThrow(() -> new CustomerNotFound("CustomerNotFound", "Customer not found"));
    }

    @Override
    public CustomerDTO create(CustomerDTO dto) {
        return CustomerMapper.toDTO(customerRepository.save(CustomerMapper.toEntity(dto)));
    }

    @Override
    public CustomerDTO update(Long id, CustomerDTO dto) {
        Customer entity = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFound("CustomerNotFound", "Customer not found"));
        CustomerMapper.updateEntity(entity, dto);
        return CustomerMapper.toDTO(customerRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFound("CustomerNotFound", "Customer not found");
        }
        customerRepository.deleteById(id);
    }
}
