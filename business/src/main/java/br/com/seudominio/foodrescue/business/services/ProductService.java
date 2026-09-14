package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.business.validation.validators.ProductBusinessValidator;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.dtos.ProductDTO;
import br.com.seudominio.foodrescue.domain.dtos.RegisterProductDTO;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.ProductMapper;
import br.com.seudominio.foodrescue.persistence.repositories.EstablishmentRepository;
import br.com.seudominio.foodrescue.persistence.repositories.ProductRepository;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that provides operations for {@link Product} entity.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Service
@Transactional
public class ProductService extends GenericService<Product, ProductDTO> {

    private final ProductRepository productRepository;
    private final EstablishmentRepository establishmentRepository;
    private final ProductMapper productMapper;
    private final ProductBusinessValidator productValidator;

    /**
     * Constructor.
     *
     * @param repository                the product repository
     * @param establishmentRepository   the establishment repository
     * @param mapper                    the product mapper
     * @param validator                 the bean validator
     * @param messageUtils              the message utils
     * @param productValidator          the business validator for the entity
     */
    public ProductService(
            ProductRepository repository,
            EstablishmentRepository establishmentRepository,
            ProductMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            ProductBusinessValidator productValidator) {
        super(repository, mapper, validator, messageUtils);
        this.productRepository = repository;
        this.establishmentRepository = establishmentRepository;
        this.productMapper = mapper;
        this.productValidator = productValidator;
    }

    /**
     * Registers a new product.
     *
     * @param dto the product registration data
     * @param establishmentId the establishment id
     * @return the registered product
     * @throws ValidationException if the entity violates a constraint
     *
     */
    public ProductDTO registerProduct(RegisterProductDTO dto, Long establishmentId) {
        Product product = productMapper.toNewEntity(dto);

        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new EntityNotFoundException(Establishment.class, establishmentId));
        product.setEstablishment(establishment);

        productValidator.validateOperation(product, BusinessOperation.CREATE);

        Product saved = productRepository.save(product);
        return productMapper.toDto(saved);
    }

    /**
     * Finds all active products for an authenticated establishment.
     * @param establishmentId the establishment id
     * @return the list of products
     */
    public List<ProductDTO> findAllForEstablishment(Long establishmentId) {
        if (!establishmentRepository.existsById(establishmentId)) {
            throw new EntityNotFoundException(Establishment.class, establishmentId);
        }

        return productRepository.findAllByEstablishmentIdAndActiveTrue(establishmentId)
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    /**
     * Finds a product for an authenticated establishment.
     *
     * @param id the product id
     * @param establishmentId the establishment id
     * @return the product
     */
    public ProductDTO findByIdForEstablishment(Long id, Long establishmentId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Product.class, id));

        if (!product.getEstablishment().getId().equals(establishmentId)) {
            throw new EntityNotFoundException(Product.class, id);
        }

        return productMapper.toDto(product);
    }
}
