package br.com.seudominio.perishrescue.business.event;

import br.com.seudominio.perishrescue.domain.enums.OfferStatus;

/**
 * Published by the kernel whenever an offer moves in the lifecycle state
 * machine. Instances react to it (notify, log, integrate) without changing
 * the kernel's flow.
 *
 * @param offerId    the offer
 * @param resourceId the offered resource
 * @param from       the previous status
 * @param to         the new status
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record OfferStatusChangedEvent(Long offerId, Long resourceId, OfferStatus from, OfferStatus to) {
}
