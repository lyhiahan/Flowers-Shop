package com.finalproject.service;

import com.finalproject.entity.Store;
import com.finalproject.entity.Transaction;
import com.finalproject.repository.StoreRepository;
import com.finalproject.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final StoreAccessService access;
    private final StoreRepository stores;
    private final TransactionRepository transactions;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public void requestWithdrawal(BigDecimal amount, String bank, String account, String holder) {
        requestWithdrawal(null, amount, bank, account, holder);
    }

    @Transactional
    public void requestWithdrawal(Store targetStore, BigDecimal amount, String bank, String account, String holder) {
        requestWithdrawal(targetStore, amount, bank, account, holder, null);
    }

    @Transactional
    public void requestWithdrawal(Store targetStore, BigDecimal amount, String bank, String account, String holder, String requestId) {
        Store assigned = targetStore != null ? targetStore : access.assignedStore();
        access.requireOwner(assigned);
        if (amount == null || amount.compareTo(BigDecimal.valueOf(50000)) < 0 || amount.scale() > 2 || amount.precision() - amount.scale() > 16) {
            throw new IllegalArgumentException("Số tiền rút tối thiểu là 50.000 VNĐ và tối đa 2 chữ số thập phân.");
        }
        if (bank == null || bank.isBlank() || account == null || account.isBlank() || holder == null || holder.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin ngân hàng.");
        }
        if (bank.trim().length() > 100 || holder.trim().length() > 100 || !account.trim().matches("[0-9]{6,30}")) {
            throw new IllegalArgumentException("Tên ngân hàng/chủ tài khoản tối đa 100 ký tự; số tài khoản gồm 6–30 chữ số.");
        }
        String description = "Rút về " + bank.trim() + " - " + account.trim() + " (" + holder.trim() + ")";
        Store store = stores.findLockedById(assigned.getId()).orElseThrow();
        entityManager.refresh(store, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        String key = requestId == null || requestId.isBlank() ? null : store.getId() + ":" + java.util.UUID.fromString(requestId);
        if (key != null) {
            var previous = transactions.findByRequestKey(key);
            if (previous.isPresent()) {
                if (previous.get().getAmount().compareTo(amount) != 0 || !"WITHDRAWAL".equals(previous.get().getType()) || !description.equals(previous.get().getDescription())) throw new IllegalArgumentException("Mã yêu cầu đã được dùng cho giao dịch khác.");
                return;
            }
        }
        if (store.getEWallet() == null || store.getEWallet().compareTo(amount) < 0) {
            throw new IllegalStateException("Số dư ví không đủ.");
        }
        store.setEWallet(store.getEWallet().subtract(amount));
        stores.save(store);
        transactions.save(Transaction.builder().store(store).amount(amount).isUp(false).type("WITHDRAWAL").requestKey(key)
                .description(description)
                .status("PENDING").build());
    }

    @Transactional
    public void resolveWithdrawal(Long id, boolean paid) {
        if (access.currentUser().getRole() != com.finalproject.enums.Role.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ quản trị viên được xử lý rút tiền.");
        }
        Transaction tx = transactions.findLockedById(id).orElseThrow();
        if (!"WITHDRAWAL".equals(tx.getType()) || !"PENDING".equals(tx.getStatus())) {
            throw new IllegalStateException("Yêu cầu đã được xử lý hoặc không phải yêu cầu rút tiền.");
        }
        if (!paid) {
            Store store = stores.findLockedById(tx.getStore().getId()).orElseThrow();
            entityManager.refresh(store, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            store.setEWallet(store.getEWallet().add(tx.getAmount()));
            stores.save(store);
        }
        tx.setStatus(paid ? "COMPLETED" : "REJECTED");
        transactions.save(tx);
    }
}
