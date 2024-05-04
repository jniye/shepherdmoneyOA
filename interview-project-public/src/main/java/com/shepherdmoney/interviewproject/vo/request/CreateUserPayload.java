package com.shepherdmoney.interviewproject.vo.request;

import lombok.Data;
import lombok.NonNull;

@Data
public class CreateUserPayload {
    @NonNull
    private String name;

    @NonNull
    private String email;
}
