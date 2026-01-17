package com.exe101.userCredential.repository;

import com.exe101.userCredential.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IUserCredentialRepository extends JpaRepository<UserCredential, Long> {
}
