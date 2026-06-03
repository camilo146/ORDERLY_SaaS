package com.orderly.api.conversation.domain.model;

/**
 * All possible chatbot states for a conversation session.
 * Stored in Redis as part of ConversationSession.
 */
public enum ConversationState {
    IDLE,
    GREETING,
    BROWSING_CATEGORIES,
    BROWSING_PRODUCTS,
    ADDING_ITEM,
    ADDRESS_REQUEST,
    PAYMENT_REQUEST,
    /** Waiting for customer to send a payment proof photo (Nequi / Transfer). */
    PAYMENT_PROOF_UPLOAD,
    ORDER_CONFIRMATION,
    ORDER_CREATED,
    /** Customer is typing a complaint description. */
    COMPLAINT_DESCRIBE,
    /** Customer can optionally send a photo as evidence of their complaint. */
    COMPLAINT_PHOTO,
    HUMAN_HANDOFF
}
