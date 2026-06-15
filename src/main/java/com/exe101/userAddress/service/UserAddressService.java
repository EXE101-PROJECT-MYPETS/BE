package com.exe101.userAddress.service;

import com.exe101.userAddress.dto.UserAddressDTO;
import com.exe101.userAddress.entity.UserAddress;
import com.exe101.userAddress.exception.UserAddressNotFound;
import com.exe101.userAddress.mapper.UserAddressMapper;
import com.exe101.userAddress.repository.IUserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public UserAddressDTO getById(Long userId, Long id) {
        return UserAddressMapper.toDTO(findByIdAndUserId(id, userId));
    }

    @Transactional
    public UserAddressDTO create(Long userId, UserAddressDTO dto) {
        UserAddress entity = UserAddressMapper.toEntity(dto);
        entity.setUserId(userId);

        if (Boolean.TRUE.equals(entity.getDefaultAddress()) || !userAddressRepository.existsByUserId(userId)) {
            userAddressRepository.clearDefaultAddress(userId, null);
            entity.setDefaultAddress(true);
        }

        return UserAddressMapper.toDTO(userAddressRepository.save(entity));
    }

    @Transactional
    public UserAddressDTO update(Long userId, Long id, UserAddressDTO dto) {
        UserAddress entity = findByIdAndUserId(id, userId);

        if (Boolean.TRUE.equals(dto.getDefaultAddress())) {
            userAddressRepository.clearDefaultAddress(userId, id);
        }

        UserAddressMapper.updateEntity(entity, dto);
        entity.setUserId(userId);
        return UserAddressMapper.toDTO(userAddressRepository.save(entity));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        UserAddress entity = findByIdAndUserId(id, userId);
        boolean wasDefault = Boolean.TRUE.equals(entity.getDefaultAddress());

        userAddressRepository.delete(entity);

        if (wasDefault) {
            userAddressRepository.findFirstByUserIdOrderByIdDesc(userId)
                    .ifPresent(nextDefault -> {
                        nextDefault.setDefaultAddress(true);
                        userAddressRepository.save(nextDefault);
                    });
        }
    }

    private UserAddress findByIdAndUserId(Long id, Long userId) {
        return userAddressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new UserAddressNotFound(
                        "UserAddressNotFound",
                        "Không tìm thấy địa chỉ người dùng"
                ));
    }
}
