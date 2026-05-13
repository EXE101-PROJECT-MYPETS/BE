package com.exe101.userAddress.service;

import com.exe101.userAddress.dto.UserAddressDTO;
import com.exe101.userAddress.mapper.UserAddressMapper;
import com.exe101.userAddress.repository.IUserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final IUserAddressRepository userAddressRepository;

    public List<UserAddressDTO> getAllByUserId(Long userId) {
        return userAddressRepository.findByUserIdOrderByDefaultAddressDescIdDesc(userId).stream()
                .map(UserAddressMapper::toDTO)
                .toList();
    }
}
