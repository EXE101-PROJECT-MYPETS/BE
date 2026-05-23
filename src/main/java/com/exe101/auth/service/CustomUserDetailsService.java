package com.exe101.auth.service;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.user.entity.User;
import com.exe101.user.repository.IUserRepository;
import com.exe101.userCredential.entity.CredentialProvider;
import com.exe101.userCredential.entity.UserCredential;
import com.exe101.userCredential.repository.IUserCredentialRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final IUserRepository userRepository;
    private final IUserCredentialRepository credentialRepository;

    public CustomUserDetailsService(IUserRepository userRepository, IUserCredentialRepository credentialRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));

        UserCredential cred = credentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy thông tin đăng nhập"));

        if (cred.getProvider() != CredentialProvider.LOCAL && (cred.getPasswordHash() == null || cred.getPasswordHash().isEmpty())) {
            throw new UsernameNotFoundException("Vui lòng đăng nhập bằng phương thức " + cred.getProvider());
        }

        return new UserPrincipal(user, cred);
    }
}
