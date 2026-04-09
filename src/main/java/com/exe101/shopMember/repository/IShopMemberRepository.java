package com.exe101.shopMember.repository;

import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.entity.ShopMember;
import com.exe101.shopMember.entity.ShopMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IShopMemberRepository extends JpaRepository<ShopMember, ShopMemberId> {
    boolean existsByUserIdAndStatus(Long userId, MemberStatus status);
}
