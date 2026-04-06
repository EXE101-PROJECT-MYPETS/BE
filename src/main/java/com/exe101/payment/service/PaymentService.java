package com.exe101.payment.service;

import com.exe101.common.IService;
import com.exe101.payment.dto.PaymentIntentDTO;
import com.exe101.payment.entity.PaymentIntent;
import com.exe101.payment.exception.PaymentNotFound;
import com.exe101.payment.mapper.PaymentIntentMapper;
import com.exe101.payment.repository.IPaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService implements IService<PaymentIntent, PaymentIntentDTO, Long> {

    private final IPaymentIntentRepository paymentIntentRepository;

    @Override
    public List<PaymentIntentDTO> getAll() {
        return paymentIntentRepository.findAll().stream().map(PaymentIntentMapper::toDTO).toList();
    }

    @Override
    public PaymentIntentDTO getById(Long id) {
        return paymentIntentRepository.findById(id)
                .map(PaymentIntentMapper::toDTO)
                .orElseThrow(() -> new PaymentNotFound("PaymentNotFound", "Payment intent not found"));
    }

    @Override
    public PaymentIntentDTO create(PaymentIntentDTO dto) {
        return PaymentIntentMapper.toDTO(paymentIntentRepository.save(PaymentIntentMapper.toEntity(dto)));
    }

    @Override
    public PaymentIntentDTO update(Long id, PaymentIntentDTO dto) {
        PaymentIntent entity = paymentIntentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFound("PaymentNotFound", "Payment intent not found"));
        entity.setShopId(dto.getShopId());
        entity.setInvoiceId(dto.getInvoiceId());
        entity.setProvider(dto.getProvider());
        entity.setMethod(dto.getMethod());
        entity.setAmount(dto.getAmount());
        entity.setCurrency(dto.getCurrency());
        entity.setStatus(dto.getStatus());
        return PaymentIntentMapper.toDTO(paymentIntentRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!paymentIntentRepository.existsById(id)) {
            throw new PaymentNotFound("PaymentNotFound", "Payment intent not found");
        }
        paymentIntentRepository.deleteById(id);
    }
}
