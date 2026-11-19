package br.com.seudominio.perishrescue.domain.mappers;

import br.com.seudominio.perishrescue.domain.dtos.ResourceDTO;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import org.springframework.stereotype.Component;

/**
 * Maps any {@link PerishableResource} to the kernel's common view,
 * {@link ResourceDTO}, with the attributes every resource shares.
 *
 * <p>The kernel cannot create a resource from a DTO: {@code PerishableResource}
 * is abstract and only the instance knows its concrete type. Creation goes
 * through the instance's {@link ResourceTypeMapper}.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class ResourceMapper implements DTOMapper<PerishableResource, ResourceDTO> {

    /**
     * Not supported: {@code PerishableResource} is abstract.
     *
     * @param dto ignored
     * @return never returns
     * @throws UnsupportedOperationException always; use the instance's {@link ResourceTypeMapper}
     */
    @Override
    public PerishableResource toEntity(ResourceDTO dto) {
        throw new UnsupportedOperationException(
                "PerishableResource is abstract; create resources through the instance's ResourceTypeMapper");
    }

    /**
     * Converts any {@link PerishableResource} to the common {@link ResourceDTO}.
     *
     * @param resource the resource to convert
     * @return the corresponding DTO
     */
    @Override
    public ResourceDTO toDto(PerishableResource resource) {
        if (resource == null) {
            return null;
        }
        return new ResourceDTO(
                resource.getId(),
                resource.getName(),
                resource.getCategory(),
                resource.getOriginalPrice().amount(),
                resource.getCurrentPrice().amount(),
                resource.getPhotoUrl(),
                resource.getStockQuantity(),
                resource.getExpirationDate(),
                resource.getOfferer().getId(),
                resource.getModificationDate()
        );
    }
}
