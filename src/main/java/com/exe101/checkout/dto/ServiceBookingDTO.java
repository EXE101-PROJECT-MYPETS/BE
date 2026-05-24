package com.exe101.checkout.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ServiceBookingDTO {

    @NotNull(message = "serviceId không được để trống")
    private Long serviceId;

    @NotNull(message = "petId không được để trống")
    private Long petId;

    @NotNull(message = "bookingDate không được để trống")
    private LocalDate bookingDate;

    @NotNull(message = "bookingTime không được để trống")
    private LocalTime bookingTime;

    @Positive(message = "staffUserId phải lớn hơn 0")
    private Long staffUserId;

    private String note;

        public Long getServiceId() {
            return serviceId;
        }

        public void setServiceId(Long serviceId) {
            this.serviceId = serviceId;
        }

        public Long getPetId() {
            return petId;
        }

        public void setPetId(Long petId) {
            this.petId = petId;
        }

        public LocalDate getBookingDate() {
            return bookingDate;
        }

        public void setBookingDate(LocalDate bookingDate) {
            this.bookingDate = bookingDate;
        }

        public LocalTime getBookingTime() {
            return bookingTime;
        }

        public void setBookingTime(LocalTime bookingTime) {
            this.bookingTime = bookingTime;
        }

        public Long getStaffUserId() {
            return staffUserId;
        }

        public void setStaffUserId(Long staffUserId) {
            this.staffUserId = staffUserId;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
}