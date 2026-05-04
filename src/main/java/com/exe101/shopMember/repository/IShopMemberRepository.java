package com.exe101.shopMember.repository;

import com.exe101.auth.dto.AuthenticatedShopDTO;
import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.entity.ShopMember;
import com.exe101.shopMember.entity.ShopMemberId;
import com.exe101.shop.entity.ShopRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface IShopMemberRepository extends JpaRepository<ShopMember, ShopMemberId> {
    boolean existsByUserIdAndStatus(Long userId, MemberStatus status);

    boolean existsByShopIdAndUserId(Long shopId, Long userId);

    boolean existsByShopIdAndUserIdAndRoleInAndStatus(
            Long shopId,
            Long userId,
            Collection<ShopRole> roles,
            MemberStatus status
    );

    long countByShopIdAndRoleAndStatus(Long shopId, ShopRole role, MemberStatus status);

    java.util.Optional<ShopMember> findByShopIdAndUserId(Long shopId, Long userId);

    @Query("""
            SELECT new com.exe101.shopMember.dto.ShopMemberDTO(
                member.shopId,
                member.userId,
                member.role,
                member.status,
                member.createdAt,
                user.fullName,
                user.email,
                user.phone,
                user.avatarUrlPreview
            )
            FROM ShopMember member
            JOIN member.user user
            WHERE member.shopId = :shopId
              AND member.role = :role
              AND member.status = :status
            ORDER BY user.fullName ASC, member.userId ASC
            """)
    List<ShopMemberDTO> findByShopIdAndRoleAndStatusForDisplay(
            @Param("shopId") Long shopId,
            @Param("role") ShopRole role,
            @Param("status") MemberStatus status
    );

    @Query("""
            SELECT new com.exe101.shopMember.dto.ShopMemberDTO(
                member.shopId,
                member.userId,
                member.role,
                member.status,
                member.createdAt,
                user.fullName,
                user.email,
                user.phone,
                user.avatarUrlPreview
            )
            FROM ShopMember member
            JOIN member.user user
            WHERE member.shopId = :shopId
              AND member.role <> :excludedRole
              AND (:role IS NULL OR member.role = :role)
              AND (:status IS NULL OR member.status = :status)
              AND (
                  :keyword IS NULL
                  OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(COALESCE(user.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(COALESCE(user.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY member.createdAt ASC, member.userId ASC
            """)
    List<ShopMemberDTO> findByShopIdForDisplay(
            @Param("shopId") Long shopId,
            @Param("excludedRole") ShopRole excludedRole,
            @Param("role") ShopRole role,
            @Param("status") MemberStatus status,
            @Param("keyword") String keyword
    );

    @Query("""
            SELECT new com.exe101.shopMember.dto.ShopMemberDTO(
                member.shopId,
                member.userId,
                member.role,
                member.status,
                member.createdAt,
                user.fullName,
                user.email,
                user.phone,
                user.avatarUrlPreview
            )
            FROM ShopMember member
            JOIN member.user user
            WHERE member.shopId = :shopId
              AND member.userId = :userId
            """)
    java.util.Optional<ShopMemberDTO> findDetailByShopIdAndUserId(
            @Param("shopId") Long shopId,
            @Param("userId") Long userId
    );

    @Query("""
            SELECT new com.exe101.auth.dto.AuthenticatedShopDTO(
                shop.id,
                shop.name,
                shop.addressText,
                shop.status,
                member.role,
                member.status
            )
            FROM ShopMember member
            JOIN member.shop shop
            WHERE member.userId = :userId
              AND member.status = :status
            ORDER BY shop.id ASC
            """)
    List<AuthenticatedShopDTO> findAuthenticatedShopsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") MemberStatus status
    );
}
