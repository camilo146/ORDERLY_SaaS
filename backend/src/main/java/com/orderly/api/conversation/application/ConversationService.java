package com.orderly.api.conversation.application;

import com.orderly.api.business.domain.model.Business;
import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.complaint.domain.model.Complaint;
import com.orderly.api.complaint.domain.port.ComplaintRepository;
import com.orderly.api.conversation.domain.model.ConversationSession;
import com.orderly.api.conversation.domain.model.ConversationState;
import com.orderly.api.conversation.infrastructure.redis.ConversationSessionStore;
import com.orderly.api.messaging.application.EvolutionApiService;
import com.orderly.api.messaging.application.WhatsAppMessageDispatcher;
import com.orderly.api.order.application.CreateOrderCommand;
import com.orderly.api.order.application.OrderManagementUseCase;
import com.orderly.api.order.domain.model.Order;
import com.orderly.api.product.domain.model.Product;
import com.orderly.api.product.domain.port.ProductRepository;
import com.orderly.api.shared.service.BusinessHoursService;
import com.orderly.api.shared.service.ServiceStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.orderly.api.business.domain.event.BusinessWhatsAppConnectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Chatbot engine — WhatsApp conversation state machine.
 *
 * States: IDLE → GREETING → BROWSING_CATEGORIES → BROWSING_PRODUCTS →
 * ADDING_ITEM → ADDRESS_REQUEST → PAYMENT_REQUEST →
 * [PAYMENT_PROOF_UPLOAD →] ORDER_CONFIRMATION → ORDER_CREATED
 * COMPLAINT_DESCRIBE → COMPLAINT_PHOTO → (complaint saved)
 * HUMAN_HANDOFF
 */
@Service
public class ConversationService implements WhatsAppMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    /**
     * Special prefix injected by the webhook controller when the customer sends
     * an image. Format: "[MEDIA:url_here]"
     */
    static final String MEDIA_PREFIX = "[MEDIA:";

    private final ConversationSessionStore sessionStore;
    private final BusinessRepository businessRepository;
    private final ProductRepository productRepository;
    private final EvolutionApiService evolutionApi;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderManagementUseCase orderManagementUseCase;
    private final ComplaintRepository complaintRepository;
    private final ServiceStatusService serviceStatusService;
    private final BusinessHoursService businessHoursService;

    public ConversationService(
            ConversationSessionStore sessionStore,
            BusinessRepository businessRepository,
            ProductRepository productRepository,
            EvolutionApiService evolutionApi,
            ApplicationEventPublisher eventPublisher,
            OrderManagementUseCase orderManagementUseCase,
            ComplaintRepository complaintRepository,
            ServiceStatusService serviceStatusService,
            BusinessHoursService businessHoursService) {
        this.sessionStore = sessionStore;
        this.businessRepository = businessRepository;
        this.productRepository = productRepository;
        this.evolutionApi = evolutionApi;
        this.eventPublisher = eventPublisher;
        this.orderManagementUseCase = orderManagementUseCase;
        this.complaintRepository = complaintRepository;
        this.serviceStatusService = serviceStatusService;
        this.businessHoursService = businessHoursService;
    }

    @Override
    public void dispatch(String businessSlug, String evolutionInstance, String remoteJid, String text) {
        Optional<Business> businessOpt = businessRepository.findBySlug(businessSlug);
        if (businessOpt.isEmpty()) {
            log.warn("Received message for unknown slug: {}", businessSlug);
            return;
        }
        Business business = businessOpt.get();
        String businessId = business.id().toString();
        String customerPhone = normalizePhone(remoteJid);

        // Check if the service is disabled (out of service)
        if (!serviceStatusService.isEnabled(business.id())) {
            try {
                evolutionApi.sendTextMessage(evolutionInstance, remoteJid,
                        "😔 Lo sentimos, en este momento estamos *fuera de servicio* temporalmente.\n\n"
                        + "Estamos trabajando para volver pronto. ¡Gracias por tu paciencia! 🙏");
            } catch (Exception ignored) {}
            return;
        }

        ConversationSession session = sessionStore.find(businessId, customerPhone)
                .orElseGet(() -> ConversationSession.start(business.id(), customerPhone, evolutionInstance));

        // Delivery photo detection: if customer sends a photo and has an OUT_FOR_DELIVERY order, auto-deliver
        String incomingText = text.trim();
        String mediaUrl = extractMediaUrl(incomingText);
        if (mediaUrl != null) {
            try {
                List<Order> activeDeliveries = orderManagementUseCase.findByCustomerWhatsappAndStatus(
                        business.id(), customerPhone, "OUT_FOR_DELIVERY");
                if (!activeDeliveries.isEmpty()) {
                    orderManagementUseCase.updateStatus(business.id(), activeDeliveries.get(0).id(), "DELIVERED");
                    session.setState(ConversationState.GREETING);
                    sessionStore.save(businessId, customerPhone, session);
                    evolutionApi.sendTextMessage(evolutionInstance, remoteJid,
                            "✅ *¡Pedido entregado exitosamente!* 🎉\n\n"
                            + "Gracias por tu compra. ¡Esperamos verte pronto! 😊\n\n"
                            + "Escribe *0* para hacer un nuevo pedido.");
                    return;
                }
            } catch (Exception e) {
                log.warn("Error checking delivery orders for {}: {}", customerPhone, e.getMessage());
            }
        }

        String response = processMessage(session, business, incomingText, remoteJid, evolutionInstance);

        sessionStore.save(businessId, customerPhone, session);

        if (response != null) {
            try {
                evolutionApi.sendTextMessage(evolutionInstance, remoteJid, response);
            } catch (Exception e) {
                log.error("Failed to send message via Evolution API: {}", e.getMessage());
            }
        }
    }

    @Override
    public void onConnectionUpdate(String businessSlug, String state) {
        if ("open".equals(state)) {
            log.info("WhatsApp CONNECTED for slug={}", businessSlug);
            businessRepository.findBySlug(businessSlug).ifPresent(business -> {
                String instanceName = "orderly-" + businessSlug;
                eventPublisher.publishEvent(
                        new BusinessWhatsAppConnectedEvent(business.id(), businessSlug, instanceName));
            });
        } else if ("close".equals(state)) {
            log.warn("WhatsApp DISCONNECTED for slug={}", businessSlug);
        }
    }

    // -------------------------------------------------------------------------
    // State machine
    // -------------------------------------------------------------------------

    private String processMessage(
            ConversationSession session,
            Business business,
            String text,
            String remoteJid,
            String instanceName) {

        if (session.isHumanActive()) {
            return null;
        }

        String lower = text.toLowerCase();

        // Global: retomar bot
        if (lower.equals("#bot")) {
            session.setState(ConversationState.GREETING);
            session.setHumanActive(false);
            clearPendingSelection(session);
            return mainMenu(business);
        }

        // Global: cancelar / menú principal
        if (lower.contains("cancelar") || lower.contains("salir") || lower.equals("0")) {
            session.setState(ConversationState.GREETING);
            session.clearCart();
            session.setPendingComplaintDescription(null);
            clearPendingSelection(session);
            return mainMenu(business);
        }

        return switch (session.getState()) {
            case IDLE, GREETING -> handleGreeting(session, business, text, lower, remoteJid, instanceName);
            case BROWSING_CATEGORIES ->
                handleBrowsingCategories(session, business, text, lower, remoteJid, instanceName);
            case BROWSING_PRODUCTS -> handleBrowsingProducts(session, business, text, lower);
            case ADDING_ITEM -> handleAddingItem(session, business, text, lower);
            case ADDRESS_REQUEST -> handleAddressRequest(session, business, text);
            case PAYMENT_REQUEST -> handlePaymentRequest(session, business, text);
            case PAYMENT_PROOF_UPLOAD -> handlePaymentProofUpload(session, business, text, lower, remoteJid);
            case ORDER_CONFIRMATION -> handleOrderConfirmation(session, business, lower);
            case ORDER_CREATED -> mainMenu(business);
            case COMPLAINT_DESCRIBE -> handleComplaintDescribe(session, business, text, lower);
            case COMPLAINT_PHOTO -> handleComplaintPhoto(session, business, text, lower);
            case HUMAN_HANDOFF -> null;
        };
    }

    private String handleGreeting(
            ConversationSession session,
            Business business,
            String text,
            String lower,
            String remoteJid,
            String instanceName) {

        // Opción 4 → quejas o reclamos
        if (lower.contains("4") || lower.contains("queja") || lower.contains("reclamo")
                || lower.contains("reclamaci")) {
            session.setState(ConversationState.COMPLAINT_DESCRIBE);
            return String.format(
                    "Lamentamos que hayas tenido una mala experiencia 😔\n\n"
                            + "Por favor cuéntanos qué ocurrió. Describe tu queja o reclamo con el mayor detalle posible:",
                    business.name());
        }

        // Opción 1 → catálogo / pedido
        if (lower.contains("1") || lower.contains("pedido") || lower.contains("domicilio")
                || lower.contains("menu") || lower.contains("menú") || lower.contains("producto")) {
            // Check business hours
            if (!businessHoursService.isOpenNow(business.id(), business.timezone())) {
                String hoursMsg = businessHoursService.buildHoursMessage(business.id());
                return String.format(
                        "⏰ En este momento no estamos recibiendo pedidos.\n\n"
                        + "*Nuestros horarios de atención:*\n%s\n\n"
                        + "¡Vuelve en nuestro horario y con gusto te atendemos! 😊\n"
                        + "Escribe *0* para ver el menú.", hoursMsg);
            }
            session.setState(ConversationState.BROWSING_PRODUCTS);
            sendCatalogImages(business, instanceName, remoteJid);
            return buildProductMenu(business);
        }

        // Opción 2 → horarios
        if (lower.contains("2") || lower.contains("horario")) {
            session.setState(ConversationState.GREETING);
            String hoursMsg = businessHoursService.buildHoursMessage(business.id());
            return String.format("🕐 *Horarios de atención de %s*\n\n%s\n\n"
                    + "¿Necesitas algo más? Escribe *0* para ver el menú principal.",
                    business.name(), hoursMsg.isEmpty() ? "Atendemos todos los días." : hoursMsg);
        }

        // Opción 3 → contacto
        if (lower.contains("3") || lower.contains("contact") || lower.contains("comunicar")) {
            session.setState(ConversationState.GREETING);
            return String.format("""
                    📬 *Contáctanos — %s*

                    Puedes escribirnos directamente por este mismo WhatsApp y un agente te responderá pronto.

                    También puedes volver al menú escribiendo *0*.
                    """, business.name());
        }

        session.setState(ConversationState.GREETING);
        return welcomeGreeting(business);
    }

    private String handleBrowsingCategories(ConversationSession session, Business business,
            String text, String lower, String remoteJid, String instanceName) {
        session.setState(ConversationState.BROWSING_PRODUCTS);
        sendCatalogImages(business, instanceName, remoteJid);
        return buildProductMenu(business);
    }

    private String handleBrowsingProducts(ConversationSession session, Business business,
            String text, String lower) {
        List<Product> products = productRepository.findAllByBusinessId(business.id());
        if (products.isEmpty()) {
            return "😕 No hay productos disponibles. Escribe *0* para volver al inicio.";
        }
        try {
            int choice = Integer.parseInt(text.trim());
            if (choice >= 1 && choice <= products.size()) {
                Product selected = products.get(choice - 1);
                session.setState(ConversationState.ADDING_ITEM);
                session.setPendingProductId(selected.id());
                session.setPendingProductName(selected.name());
                session.setPendingProductUnitPrice(selected.price());
                return String.format("Has elegido *%s* — $%,.0f\n¿Cuántas unidades quieres? (responde solo el número)",
                        selected.name(), selected.price());
            }
        } catch (NumberFormatException ignored) {
        }
        return "Por favor responde con el *número* del producto que deseas, o escribe *0* para cancelar.";
    }

    private String handleAddingItem(ConversationSession session, Business business, String text, String lower) {
        if (session.getPendingProductId() == null || session.getPendingProductUnitPrice() == null
                || session.getPendingProductName() == null) {
            session.setState(ConversationState.BROWSING_PRODUCTS);
            return "Primero elige un producto del menú para continuar.";
        }
        try {
            int qty = Integer.parseInt(text.trim());
            if (qty < 1)
                return "Por favor ingresa una cantidad válida (mínimo 1).";

            session.addToCart(new ConversationSession.CartItem(
                    session.getPendingProductId(),
                    session.getPendingProductName(),
                    qty,
                    session.getPendingProductUnitPrice()));
            clearPendingSelection(session);
            session.setState(ConversationState.ADDRESS_REQUEST);
            return "¡Perfecto! ¿Cuál es tu dirección de entrega?\n(O escribe *recoger* si lo retiras en tienda)";
        } catch (NumberFormatException ignored) {
            return "Por favor responde con el número de unidades.";
        }
    }

    private String handleAddressRequest(ConversationSession session, Business business, String text) {
        session.setDeliveryAddress(text);
        session.setState(ConversationState.PAYMENT_REQUEST);
        return buildPaymentMenu(business);
    }

    private String handlePaymentRequest(ConversationSession session, Business business, String text) {
        String method = switch (text.trim()) {
            case "1" -> "CASH";
            case "2" -> "NEQUI";
            case "3" -> "TRANSFER";
            default -> null;
        };
        if (method == null) {
            return "Por favor responde *1*, *2* o *3* para seleccionar el método de pago.";
        }
        session.setSelectedPaymentMethod(method);

        if ("CASH".equals(method)) {
            // Efectivo: ir directo a confirmación
            session.setState(ConversationState.ORDER_CONFIRMATION);
            return buildOrderSummary(session);
        } else {
            // Pago digital: pedir foto del comprobante
            session.setState(ConversationState.PAYMENT_PROOF_UPLOAD);
            String accountInfo = buildPaymentAccountInfo(business, method);
            return String.format("""
                    %s

                    ✅ Una vez realizado el pago, por favor *envía la foto del comprobante* aquí como imagen.
                    """, accountInfo);
        }
    }

    private String handlePaymentProofUpload(
            ConversationSession session,
            Business business,
            String text,
            String lower,
            String remoteJid) {

        String mediaUrl = extractMediaUrl(text);
        boolean hasMedia = mediaUrl != null;

        if (!hasMedia) {
            // Si no es imagen, recordarle que debe enviar la foto
            return "📷 Por favor envía la *foto del comprobante de pago* como imagen para continuar.";
        }

        // Guardar URL del comprobante en la sesión para usarla al crear el pedido
        session.setPendingOrderId(mediaUrl); // usamos este campo temporalmente para la URL
        session.setState(ConversationState.ORDER_CONFIRMATION);
        return buildOrderSummary(session) + "\n\n✅ Comprobante recibido. " +
                "¿Confirmas el pedido? Responde *SÍ* o *NO*.";
    }

    private String handleOrderConfirmation(ConversationSession session, Business business, String lower) {
        if (lower.startsWith("s") || lower.equals("si") || lower.equals("sí")) {
            // Construir comando de creación del pedido
            List<CreateOrderCommand.OrderItemInput> items = session.getCart().stream()
                    .map(item -> new CreateOrderCommand.OrderItemInput(
                            item.getProductId(), item.getQuantity(), null))
                    .toList();

            if (items.isEmpty()) {
                session.setState(ConversationState.GREETING);
                return "❌ No hay productos en tu carrito. Escribe *0* para volver al menú.";
            }

            String deliveryType = "DOMICILIO";
            if (session.getDeliveryAddress() != null
                    && session.getDeliveryAddress().toLowerCase().contains("recoger")) {
                deliveryType = "RECOGIDA";
            }

            String customerName = session.getCustomerName() != null
                    ? session.getCustomerName()
                    : session.getCustomerPhone();

            String paymentMethod = session.getSelectedPaymentMethod() != null
                    ? session.getSelectedPaymentMethod()
                    : "CASH";

            try {
                var order = orderManagementUseCase.create(new CreateOrderCommand(
                        business.id(),
                        customerName,
                        session.getCustomerPhone(),
                        deliveryType,
                        session.getDeliveryAddress(),
                        items,
                        paymentMethod));

                // Si había comprobante de pago, asociarlo al pedido
                String proofUrl = session.getPendingOrderId(); // URL temporal
                if (proofUrl != null && !proofUrl.isBlank() && proofUrl.startsWith("http")) {
                    try {
                        orderManagementUseCase.setPaymentProof(business.id(), order.id(), proofUrl);
                    } catch (Exception e) {
                        log.warn("No se pudo asociar comprobante al pedido {}: {}", order.id(), e.getMessage());
                    }
                }

                session.setState(ConversationState.ORDER_CREATED);
                session.clearCart();
                session.setSelectedPaymentMethod(null);
                session.setPendingOrderId(null);
                clearPendingSelection(session);

                return String.format("""
                        ✅ *¡Pedido confirmado!* 🎉

                        Tu pedido #%s ha sido recibido y ya lo estamos preparando.
                        Te avisaremos cuando esté en camino 🛵

                        Escribe *0* en cualquier momento para hacer un nuevo pedido.
                        """, order.id().toString().substring(0, 8).toUpperCase());

            } catch (Exception e) {
                log.error("Error al crear pedido para business={}: {}", business.id(), e.getMessage(), e);
                session.setState(ConversationState.GREETING);
                return "❌ Ocurrió un error al procesar tu pedido. Por favor intenta de nuevo o escribe *0*.";
            }

        } else {
            session.setState(ConversationState.GREETING);
            session.clearCart();
            session.setSelectedPaymentMethod(null);
            session.setPendingOrderId(null);
            clearPendingSelection(session);
            return "❌ Pedido cancelado.\n\n" + mainMenu(business);
        }
    }

    private String handleComplaintDescribe(
            ConversationSession session,
            Business business,
            String text,
            String lower) {

        if (text.isBlank() || text.length() < 10) {
            return "Por favor describe tu queja con más detalle (mínimo 10 caracteres).";
        }
        session.setPendingComplaintDescription(text);
        session.setState(ConversationState.COMPLAINT_PHOTO);
        return """
                Gracias por contarnos 🙏

                ¿Tienes alguna foto o evidencia que quieras adjuntar?
                📷 Envía la imagen ahora, o escribe *sin foto* para continuar sin adjuntar evidencia.
                """;
    }

    private String handleComplaintPhoto(
            ConversationSession session,
            Business business,
            String text,
            String lower) {

        String evidenceUrl = null;
        String mediaUrl = extractMediaUrl(text);

        if (mediaUrl != null) {
            evidenceUrl = mediaUrl;
        } else if (!lower.contains("sin foto") && !lower.contains("sin imagen") && !lower.contains("no")) {
            return "📷 Envía la imagen como adjunto, o escribe *sin foto* para continuar.";
        }

        String description = session.getPendingComplaintDescription();
        if (description == null || description.isBlank()) {
            session.setState(ConversationState.COMPLAINT_DESCRIBE);
            return "Por favor describe tu queja primero.";
        }

        try {
            complaintRepository.save(Complaint.create(
                    business.id(),
                    session.getCustomerPhone(),
                    description,
                    evidenceUrl));
        } catch (Exception e) {
            log.error("Error guardando queja para business={}: {}", business.id(), e.getMessage(), e);
        }

        session.setPendingComplaintDescription(null);
        session.setState(ConversationState.GREETING);

        return String.format("""
                ✅ *Tu queja ha sido registrada.*

                Nuestro equipo de *%s* la revisará a la brevedad y te contactará si es necesario.

                Lamentamos los inconvenientes causados 🙏
                Escribe *0* para volver al menú principal.
                """, business.name());
    }

    // ── Message helpers ───────────────────────────────────────────────────────

    private String welcomeGreeting(Business business) {
        String botName = business.botName();
        String emoji = business.botEmoji();
        return String.format("""
                ¡Hola! Soy *%s* %s, el asistente virtual de *%s* 😊

                Estoy aquí para ayudarte. ¿Qué deseas hacer?

                1️⃣  Hacer un pedido / Ver productos
                2️⃣  Horarios de atención
                3️⃣  Contáctanos
                4️⃣  Quejas o reclamos 📋

                👆 *Responde con el número de tu opción.*
                """, botName, emoji, business.name());
    }

    private String mainMenu(Business business) {
        String botName = business.botName();
        String emoji = business.botEmoji();
        return String.format("""
                %s *%s* al servicio 😊 ¿En qué te ayudo?

                1️⃣  Hacer un pedido / Ver productos
                2️⃣  Horarios de atención
                3️⃣  Contáctanos
                4️⃣  Quejas o reclamos 📋

                👆 *Responde con el número de tu opción.*
                """, emoji, botName);
    }

    private String buildPaymentMenu(Business business) {
        String nequi = business.paymentNequi() != null ? business.paymentNequi() : "No configurado";
        String bankName = business.paymentBankName() != null ? business.paymentBankName() : "No configurado";
        String bankAccount = business.paymentBankAccount() != null ? business.paymentBankAccount() : "---";
        String bankHolder = business.paymentBankHolder() != null ? business.paymentBankHolder() : "---";

        return String.format("""
                💳 *¿Cómo deseas pagar?*

                1️⃣  Efectivo contra entrega
                2️⃣  Nequi / Daviplata → *%s*
                3️⃣  Transferencia bancaria
                     Banco: %s
                     Cuenta: %s
                     Titular: %s

                👆 *Responde con el número de tu opción.*
                """, nequi, bankName, bankAccount, bankHolder);
    }

    private String buildPaymentAccountInfo(Business business, String method) {
        if ("NEQUI".equals(method)) {
            String nequi = business.paymentNequi() != null ? business.paymentNequi() : "No configurado";
            return String.format("""
                    📱 *Pago por Nequi / Daviplata*

                    Número: *%s*

                    Realiza la transferencia y envía aquí la foto del comprobante.
                    """, nequi);
        } else {
            String bankName = business.paymentBankName() != null ? business.paymentBankName() : "No configurado";
            String bankAccount = business.paymentBankAccount() != null ? business.paymentBankAccount() : "---";
            String bankHolder = business.paymentBankHolder() != null ? business.paymentBankHolder() : "---";
            return String.format("""
                    🏦 *Transferencia bancaria*

                    Banco: *%s*
                    Cuenta: *%s*
                    Titular: *%s*

                    Realiza la transferencia y envía aquí la foto del comprobante.
                    """, bankName, bankAccount, bankHolder);
        }
    }

    private String buildOrderSummary(ConversationSession session) {
        StringBuilder sb = new StringBuilder("📋 *Resumen de tu pedido:*\n\n");
        session.getCart().forEach(item -> sb.append(String.format("• %s × %d — $%,.0f%n",
                item.getProductName(), item.getQuantity(),
                item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))));
        sb.append(String.format("%nDirección: %s", session.getDeliveryAddress()));
        String pm = session.getSelectedPaymentMethod();
        if (pm != null) {
            String pmLabel = switch (pm) {
                case "CASH" -> "Efectivo contra entrega";
                case "NEQUI" -> "Nequi / Daviplata";
                case "TRANSFER" -> "Transferencia bancaria";
                default -> pm;
            };
            sb.append(String.format("%nPago: %s", pmLabel));
        }
        sb.append("\n\n¿Confirmas? Responde *SÍ* para enviar o *NO* para cancelar.");
        return sb.toString();
    }

    private String buildProductMenu(Business business) {
        List<Product> products = productRepository.findAllByBusinessId(business.id());
        if (products.isEmpty()) {
            return "😕 Aún no hay productos disponibles. Vuelve pronto.";
        }
        StringBuilder sb = new StringBuilder("🛒 *Nuestros productos:*\n\n");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            sb.append(String.format("%d. %s — $%,.0f%n", i + 1, p.name(), p.price()));
        }
        sb.append("\n👆 Responde con el *número* del producto que deseas.");
        return sb.toString();
    }

    private void sendCatalogImages(Business business, String instanceName, String remoteJid) {
        List<Product> products = productRepository.findAllByBusinessId(business.id());
        products.stream()
                .filter(product -> product.imageUrl() != null && !product.imageUrl().isBlank())
                .limit(8)
                .forEach(product -> {
                    try {
                        evolutionApi.sendImageMessage(
                                instanceName,
                                remoteJid,
                                product.imageUrl(),
                                String.format("%s - $%,.0f", product.name(), product.price()));
                    } catch (Exception e) {
                        log.warn("No se pudo enviar imagen de producto {}: {}", product.id(), e.getMessage());
                    }
                });
    }

    private void clearPendingSelection(ConversationSession session) {
        session.setPendingProductId(null);
        session.setPendingProductName(null);
        session.setPendingProductUnitPrice(null);
    }

    /**
     * Extracts the media URL from the special "[MEDIA:url]" prefix injected
     * by the webhook controller when the customer sends an image.
     */
    private String extractMediaUrl(String text) {
        if (text != null && text.startsWith(MEDIA_PREFIX) && text.endsWith("]")) {
            return text.substring(MEDIA_PREFIX.length(), text.length() - 1).trim();
        }
        return null;
    }

    private String normalizePhone(String remoteJid) {
        if (remoteJid == null)
            return "";
        int idx = remoteJid.indexOf('@');
        return idx > 0 ? remoteJid.substring(0, idx) : remoteJid;
    }
}
