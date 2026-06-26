package com.exe101.service_shop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDetailReviewDTO {
    private Long id;
    private Integer star;
    private String content;
    private ServiceDetailReviewUserDTO user;
    private OffsetDateTime date;
    private String reply;
    private Long likeCount;
    private Long dislikeCount;
    private String userReaction;
}
