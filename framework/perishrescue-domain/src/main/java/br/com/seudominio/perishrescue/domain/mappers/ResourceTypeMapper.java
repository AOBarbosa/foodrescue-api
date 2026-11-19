package br.com.seudominio.perishrescue.domain.mappers;

import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

/**
 * Maps an instance's concrete resource type to and from its REST payloads.
 *
 * <p>Part of <b>hot spot 1</b>: the kernel owns the registration and catalog
 * flow but does not know the instance's attributes (allergens, batch number,
 * species...). The instance provides one implementation per concrete
 * {@link PerishableResource} subclass and hands it to its
 * {@code AbstractResourceController} subclass.</p>
 *
 * @param <R> the concrete resource type (e.g. {@code FoodItem})
 * @param <Q> the registration request payload
 * @param <D> the response payload
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public interface ResourceTypeMapper<R extends PerishableResource, Q, D> {

    /**
     * The concrete resource type this mapper handles. The kernel uses it to
     * tell this instance's resources apart from other subclasses.
     *
     * @return the concrete resource class
     */
    Class<R> resourceType();

    /**
     * Builds a new, not yet persisted resource from a registration request.
     * The kernel sets the owning offerer, validates and saves it afterwards.
     *
     * @param request the registration payload
     * @return the new resource
     */
    R toNewEntity(Q request);

    /**
     * Converts a resource of this type to its response payload.
     *
     * @param resource the resource
     * @return the response payload
     */
    D toDto(R resource);
}
