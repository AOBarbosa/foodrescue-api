package br.com.seudominio.perishrescue.business.pricing;

import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * What a {@link PricingPolicy} can base its discount on.
 *
 * @param resource    the resource being priced
 * @param beneficiary the beneficiary reserving it; {@code null} when the offer
 *                    itself is (re)priced
 * @param now         the current date and time
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record PricingContext(PerishableResource resource, Beneficiary beneficiary, LocalDateTime now) {

    /**
     * The beneficiary, when the price is for a specific reservation.
     *
     * @return the beneficiary, or empty when pricing the offer itself
     */
    public Optional<Beneficiary> optionalBeneficiary() {
        return Optional.ofNullable(beneficiary);
    }
}
