package com.orderly.api.messaging.application;

/**
 * Port that routes inbound WhatsApp messages to the chatbot engine.
 * Implemented by the ConversationService.
 */
public interface WhatsAppMessageDispatcher {

    /**
     * Dispatch an inbound text message from a customer.
     *
     * @param businessSlug      business identifier (from webhook URL)
     * @param evolutionInstance actual Evolution API instance name (e.g.
     *                          "orderly-7792a041-...")
     * @param remoteJid         sender's WhatsApp JID (e.g.
     *                          "573001234567@s.whatsapp.net")
     * @param text              message text
     */
    void dispatch(String businessSlug, String evolutionInstance, String remoteJid, String text);

    /**
     * React to a WhatsApp connection state change for a business.
     *
     * @param businessSlug business identifier
     * @param state        Evolution API state: "open", "connecting", "close"
     */
    void onConnectionUpdate(String businessSlug, String state);
}
