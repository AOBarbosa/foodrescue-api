package br.com.seudominio.foodrescue.domain.mappers;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.dtos.SaleDTO;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper between {@link Sale} and {@link SaleDTO}.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class SaleMapper implements DTOMapper<Sale, SaleDTO> {

    /**
     * Converts a {@link SaleDTO} to a {@link Sale} entity.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity, or {@code null} if dto is {@code null}
     */
    @Override
    public Sale toEntity(SaleDTO dto) {
        if (dto == null) {
            return null;
        }

        Sale sale = new Sale();
        sale.setId(dto.id());
        sale.setQuantity(dto.quantity());
        if (dto.unitPrice() != null) {
            sale.setUnitPrice(Money.of(dto.unitPrice()));
        }
        sale.setSoldAt(dto.soldAt());
        return sale;
    }

    /**
     * Converts a {@link Sale} entity to a {@link SaleDTO}.
     *
     * @param sale the entity to convert
     * @return the corresponding DTO, or {@code null} if sale is {@code null}
     */
    @Override
    public SaleDTO toDto(Sale sale) {
        if (sale == null) {
            return null;
        }

        BigDecimal unitPriceAmount = sale.getUnitPrice() != null ? sale.getUnitPrice().amount() : null;
        BigDecimal totalPrice = (unitPriceAmount != null)
                ? unitPriceAmount.multiply(BigDecimal.valueOf(sale.getQuantity()))
                : null;

        return new SaleDTO(
                sale.getId(),
                sale.getProduct() != null ? sale.getProduct().getId() : null,
                sale.getQuantity(),
                unitPriceAmount,
                totalPrice,
                sale.getSoldAt(),
                sale.getCreationDate()
        );
    }
}
