package com.exe101.user.service;

import com.exe101.common.IService;
import com.exe101.user.dto.UserDTO;
import com.exe101.user.entity.User;
import com.exe101.user.exception.UserNotFound;
import com.exe101.user.mapper.UserMapper;
import com.exe101.user.repository.IUserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements IService<User, UserDTO, Long> {

    private final IUserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(IUserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public List<UserDTO> getAll() {
        return List.of();
    }

    @Override
    public UserDTO getById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new UserNotFound("UserNotFound", "User Not Found!"));
    }

    @Override
    public UserDTO create(UserDTO dto) {
        return null;
    }

    @Override
    public UserDTO update(Long aLong, UserDTO dto) {
        return null;
    }

    @Override
    public void delete(Long aLong) {

    }
}
