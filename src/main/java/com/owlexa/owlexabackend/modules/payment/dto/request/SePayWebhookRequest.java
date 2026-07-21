package com.owlexa.owlexabackend.modules.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SePayWebhookRequest {

    @JsonProperty("gateway")
    private String gateway;

    @JsonProperty("transactionDate")
    private String transactionDate;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("subAccount")
    private String subAccount;

    @JsonProperty("code")
    private String code;

    @JsonProperty("content")
    private String content;

    @JsonProperty("transferType")
    private String transferType;

    @JsonProperty("description")
    private String description;

    @JsonProperty("transferAmount")
    private Long transferAmount;

    @JsonProperty("referenceCode")
    private String referenceCode;

    @JsonProperty("accumulated")
    private Long accumulated;

    @JsonProperty("id")
    private Long id;
}
