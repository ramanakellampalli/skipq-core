package com.skipq.core.subscription;

/** Status values admin can explicitly store. PAST_DUE is computed on read — never stored. */
public enum AdminSubscriptionStatus {
    ACTIVE,
    SUSPENDED
}
