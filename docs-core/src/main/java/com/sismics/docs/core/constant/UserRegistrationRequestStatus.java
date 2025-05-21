package com.sismics.docs.core.constant;

/**
 * User registration request status.
 *
 * @author Scott-Einstein
 */
public enum UserRegistrationRequestStatus {
    /**
     * The request is pending admin approval.
     */
    PENDING,

    /**
     * The request has been approved.
     */
    APPROVED,

    /**
     * Request is rejected.
     */
    REJECTED
}
