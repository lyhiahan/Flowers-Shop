package com.finalproject.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration @Getter
public class BankTransferConfig {
    @Value("${bank-transfer.mode:manual}") private String mode;
    public boolean isMock() { return "mock".equalsIgnoreCase(mode); }
    @Value("${bank-transfer.bank-name:Techcombank}") private String bankName;
    @Value("${bank-transfer.account-name:BUI THANH PHUC}") private String accountName;
    @Value("${bank-transfer.account-number:3337110941}") private String accountNumber;
}
