package com.shepherdmoney.interviewproject.vo.request;

import lombok.Data;
import lombok.NonNull;

@Data
public class AddCreditCardToUserPayload {
    @NonNull
    private int userId;

    private String cardIssuanceBank;
    @NonNull
    private String cardNumber;
}
