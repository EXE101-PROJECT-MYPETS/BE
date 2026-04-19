package com.exe101.shopMember.repository;

import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.entity.ShopMember;
import com.exe101.shopMember.entity.ShopMemberId;
import com.exe101.shop.entity.ShopRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface IShopMemberRepository extends JpaRepository<ShopMember, ShopMemberId> {
    boolean existsByUserIdAndStatus(Long userId, MemberStatus status);

    boolean existsByShopIdAndUserIdAndRoleInAndStatus(
            Long shopId,
            Long userId,
            Collection<ShopRole> roles,
            MemberStatus status
    );
}
