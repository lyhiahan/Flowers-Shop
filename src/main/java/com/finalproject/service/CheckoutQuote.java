package com.finalproject.service;

import java.math.BigDecimal;

/** Amounts recomputed on the server, in VND. */
public record CheckoutQuote(BigDecimal subtotal, BigDecimal shippingFee,
                            BigDecimal discount, BigDecimal total, Long storeId) { }
