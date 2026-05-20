package com.exe101.shopMember.repository;

import com.exe101.auth.dto.AuthenticatedShopDTO;
import com.exe101.shop.entity.ShopRole;
import com.exe101.shop.entity.ShopStatus;
import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.entity.ShopMember;
import com.exe101.shopMember.entity.ShopMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IShopMemberRepository extends JpaRepository<ShopMember, ShopMemberId> {
    boolean existsByUserIdAndStatus(Long userId, MemberStatus status);

    @Query("""
            SELECT COUNT(member) > 0
            FROM ShopMember member
            JOIN member.shop shop
            WHERE member.userId = :userId
              AND member.status = :memberStatus
              AND shop.status = :shopStatus
            """)
    boolean existsByUserIdAndMemberStatusAndShopStatus(
            @Param("userId") Long userId,
            @Param("memberStatus") MemberStatus memberStatus,
            @Param("shopStatus") ShopStatus shopStatus
    );

    @Query("""
            SELECT COUNT(member) > 0
            FROM ShopMember member
            JOIN member.shop shop
            WHERE member.userId = :userId
              AND shop.status = :shopStatus
            """)
    boolean existsByUserIdAndShopStatus(
            @Param("userId") Long userId,
            @Param("shopStatus") ShopStatus shopStatus
    );

    boolean existsByShopIdAndStatus(Long shopId, MemberStatus status);

    boolean existsByShopIdAndUserId(Long shopId, Long userId);

        @Query("SELECT COUNT(m) > 0 FROM ShopMember m WHERE m.shopId = :shopId AND m.userId = :userId AND m.status = :status")
        boolean existsByShopIdAndUserIdAndStatus(@Param("shopId") Long shopId, @Param("userId") Long userId, @Param("status") MemberStatus status);

    java.util.Optional<ShopMember> findByShopIdAndUserId(Long shopId, Long userId);

    List<ShopMember> findByShopId(Long shopId);

    List<ShopMember> findByShopIdAndRole(Long shopId, ShopRole role);

    @Query("""
            SELECT member
            FROM ShopMember member
            JOIN FETCH member.user user
            WHERE member.shopId IN :shopIds
              AND member.role = :role
            ORDER BY member.shopId ASC, member.createdAt ASC, member.userId ASC
            """)
    List<ShopMember> findByShopIdInAndRoleWithUser(
            @Param("shopIds") List<Long> shopIds,
            @Param("role") ShopRole role
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
              AND member.status = :status
            ORDER BY user.fullName ASC, member.userId ASC
            """)
    List<ShopMemberDTO> findByShopIdAndStatusForDisplay(
            @Param("shopId") Long shopId,
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
              AND (:role IS NULL OR member.role = :role)
              AND (:status IS NULL OR member.status = :status)
              AND (
                  :keyword IS NULL
                  OR LOWER(user.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                  OR LOWER(COALESCE(user.email, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                  OR LOWER(COALESCE(user.phone, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
              )
            ORDER BY member.createdAt ASC, member.userId ASC
            """)
    List<ShopMemberDTO> findByShopIdForDisplay(
            @Param("shopId") Long shopId,
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
              AND shop.status = :shopStatus
            ORDER BY shop.id ASC
            """)
    List<AuthenticatedShopDTO> findAuthenticatedShopsByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") MemberStatus status,
            @Param("shopStatus") ShopStatus shopStatus
    );
}
