package com.shepherdmoney.interviewproject.vo.request;

import java.time.LocalDate;

import lombok.Data;
import lombok.NonNull;

@Data
public class UpdateBalancePayload {
    @NonNull
    private String creditCardNumber;
    @NonNull
    private LocalDate balanceDate;
    @NonNull
    private double balanceAmount;
}
