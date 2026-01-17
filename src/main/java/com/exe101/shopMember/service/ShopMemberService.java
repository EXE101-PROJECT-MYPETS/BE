package com.exe101.shopMember.service;

import com.exe101.common.IService;
import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.entity.ShopMember;
import com.exe101.shopMember.entity.ShopMemberId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShopMemberService implements IService<ShopMember, ShopMemberDTO, ShopMemberId> {
    @Override
    public List<ShopMemberDTO> getAll() {
        return List.of();
    }

    @Override
    public ShopMemberDTO getById(ShopMemberId shopMemberId) {
        return null;
    }

    @Override
    public ShopMemberDTO create(ShopMemberDTO dto) {
        return null;
    }

    @Override
    public ShopMemberDTO update(ShopMemberId shopMemberId, ShopMemberDTO dto) {
        return null;
    }

    @Override
    public void delete(ShopMemberId shopMemberId) {

    }
}
