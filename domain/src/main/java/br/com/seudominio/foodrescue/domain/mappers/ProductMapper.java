package br.com.seudominio.foodrescue.domain.mappers;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.dtos.ProductDTO;
import br.com.seudominio.foodrescue.domain.dtos.RegisterProductDTO;
import br.com.seudominio.foodrescue.domain.entities.Product;
import org.springframework.stereotype.Component;

/**
 * Mapper between {@link Product} and {@link ProductDTO}.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class ProductMapper implements DTOMapper<Product, ProductDTO> {

    /**
     * Converts a {@link ProductDTO} to a {@link Product}.
     * @param dto the DTO to convert
     * @return the corresponding entity
     */
    @Override
    public Product toEntity(ProductDTO dto) {
        if (dto == null) {
            return null;
        }

        Product product = new Product();
        product.setId(dto.id());
        product.setName(dto.name());
        product.setCategory(dto.category());
        product.setOriginalPrice(Money.of(dto.originalPrice()));
        product.setCurrentPrice(Money.of(dto.currentPrice()));
        product.setPhotoUrl(dto.photoUrl());
        product.setStockQuantity(dto.stockQuantity());
        product.setExpirationDate(dto.expirationDate());

        return product;
    }

    /**
     * Converts a {@link RegisterProductDTO} to a new {@link Product} entity.
     * The builder is used to guarantee that the current price is set to the original price by default.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity, or {@code null} if {@code dto} is {@code null}
     */
    public Product toNewEntity(RegisterProductDTO dto) {
        if (dto == null) {
            return null;
        }

        return Product.builder()
                .name(dto.name())
                .category(dto.category())
                .originalPrice(Money.of(dto.originalPrice()))
                .photoUrl(dto.photoUrl())
                .build();
    }

    /**
     * Converts a {@link Product} to a {@link ProductDTO}.
     *
     * @param product the entity to convert
     * @return the corresponding DTO, or {@code null} if {@code product} is {@code null}
     */
    @Override
    public ProductDTO toDto(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getOriginalPrice().amount(),
                product.getCurrentPrice().amount(),
                product.getPhotoUrl(),
                product.getStockQuantity(),
                product.getExpirationDate(),
                product.getEstablishment().getId()
        );
    }


}
