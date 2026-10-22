package com.finalproject.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration @Getter
public class BankTransferConfig {
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment environment;
    @Value("${bank-transfer.mode:manual}") private String mode;
    public boolean isMock() {
        return "mock".equalsIgnoreCase(mode) && (environment == null || !environment.matchesProfiles("prod"));
    }

    @jakarta.annotation.PostConstruct
    public void validate() {
        if (environment != null && environment.matchesProfiles("prod") && "mock".equalsIgnoreCase(mode))
            throw new IllegalStateException("Production không được bật chuyển khoản mô phỏng.");
    }
    @Value("${bank-transfer.bank-name:Techcombank}") private String bankName;
    @Value("${bank-transfer.account-name:BUI THANH PHUC}") private String accountName;
    @Value("${bank-transfer.account-number:3337110941}") private String accountNumber;
}
