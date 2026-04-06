package com.exe101.invoice.service;

import com.exe101.common.IService;
import com.exe101.invoice.dto.InvoiceDTO;
import com.exe101.invoice.entity.Invoice;
import com.exe101.invoice.exception.InvoiceNotFound;
import com.exe101.invoice.mapper.InvoiceMapper;
import com.exe101.invoice.repository.IInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService implements IService<Invoice, InvoiceDTO, Long> {

    private final IInvoiceRepository invoiceRepository;

    @Override
    public List<InvoiceDTO> getAll() {
        return invoiceRepository.findAll().stream().map(InvoiceMapper::toDTO).toList();
    }

    @Override
    public InvoiceDTO getById(Long id) {
        return invoiceRepository.findById(id)
                .map(InvoiceMapper::toDTO)
                .orElseThrow(() -> new InvoiceNotFound("InvoiceNotFound", "Invoice not found"));
    }

    @Override
    public InvoiceDTO create(InvoiceDTO dto) {
        return InvoiceMapper.toDTO(invoiceRepository.save(InvoiceMapper.toEntity(dto)));
    }

    @Override
    public InvoiceDTO update(Long id, InvoiceDTO dto) {
        Invoice entity = invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFound("InvoiceNotFound", "Invoice not found"));
        InvoiceMapper.updateEntity(entity, dto);
        return InvoiceMapper.toDTO(invoiceRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!invoiceRepository.existsById(id)) {
            throw new InvoiceNotFound("InvoiceNotFound", "Invoice not found");
        }
        invoiceRepository.deleteById(id);
    }
}
