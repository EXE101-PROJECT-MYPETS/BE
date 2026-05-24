package com.exe101.checkout.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductOrderDTO {

    @NotNull(message = "productId không được để trống")
    private Long productId;

    @NotNull(message = "qty không được để trống")
    @Positive(message = "qty phải lớn hơn 0")
    private Integer qty;

    @PositiveOrZero(message = "unitPrice phải lớn hơn hoặc bằng 0")
    private Long unitPrice;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQty() {
            return qty;
        }

        public void setQty(Integer qty) {
            this.qty = qty;
        }

        public Long getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(Long unitPrice) {
            this.unitPrice = unitPrice;
        }
}