package com.exe101.email.repository;

import com.exe101.email.entity.EmailVerificationPurpose;
import com.exe101.email.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IEmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findFirstByEmailAndPurposeAndCodeAndVerifiedFalseAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
            String email,
            EmailVerificationPurpose purpose,
            String code
    );

    Optional<EmailVerificationToken> findFirstByEmailAndPurposeAndCodeAndUsedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
            String email,
            EmailVerificationPurpose purpose,
            String code
    );

    @Modifying
    @Query("""
            update EmailVerificationToken token
               set token.invalidatedAt = :invalidatedAt
             where token.email = :email
               and token.purpose = :purpose
               and token.verified = false
               and token.usedAt is null
               and token.invalidatedAt is null
               and token.expiresAt > :invalidatedAt
            """)
    int invalidateActiveTokens(
            @Param("email") String email,
            @Param("purpose") EmailVerificationPurpose purpose,
            @Param("invalidatedAt") OffsetDateTime invalidatedAt
    );
}
