package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.business.validation.validators.EstablishmentBusinessValidator;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.dtos.EstablishmentDTO;
import br.com.seudominio.foodrescue.domain.dtos.EstablishmentUpdateDTO;
import br.com.seudominio.foodrescue.domain.dtos.LoginRequest;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.EstablishmentMapper;
import br.com.seudominio.foodrescue.persistence.repositories.EstablishmentRepository;

import jakarta.validation.Validator;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that provides operations for {@link Establishment} entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Service
@Transactional
public class EstablishmentService extends GenericService<Establishment, EstablishmentDTO> {

    private final EstablishmentRepository establishmentRepository;
    private final EstablishmentMapper establishmentMapper;
    private final EstablishmentBusinessValidator establishmentValidator;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor.
     *
     * @param repository            the establishment repository
     * @param mapper                the establishment mapper
     * @param validator             the bean validator
     * @param messageUtils          the message utils
     * @param establishmentValidator the business validator for the entity
     * @param passwordEncoder       the password encoder
     */
    public EstablishmentService(
            EstablishmentRepository repository,
            EstablishmentMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            EstablishmentBusinessValidator establishmentValidator,
            PasswordEncoder passwordEncoder) {
        super(repository, mapper, validator, messageUtils);
        this.establishmentRepository = repository;
        this.establishmentMapper = mapper;
        this.establishmentValidator = establishmentValidator;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new establishment: hashes the raw password, then validates
     * (bean constraints + CNPJ format/uniqueness + email uniqueness) before
     * persisting.
     *
     * @param dto the registration data, {@code password} carrying the raw password
     * @return the registered establishment, without its password hash
     * @throws ValidationException if the entity violates a constraint, the CNPJ is invalid,
     *                             or the CNPJ/email is already registered
     */
    @Override
    public EstablishmentDTO save(EstablishmentDTO dto) {
        Establishment entity = establishmentMapper.toEntity(dto);
        entity.setPasswordHash(passwordEncoder.encode(entity.getPasswordHash()));

        establishmentValidator.validateOperation(entity, BusinessOperation.CREATE);

        Establishment saved = establishmentRepository.save(entity);
        return establishmentMapper.toDto(saved);
    }

    /**
     * Validates login credentials against a registered establishment.
     *
     * @param request the login credentials
     * @return the authenticated establishment, without its password hash
     * @throws BusinessRuleViolationException if the email is unknown or the password doesn't match
     */
    public EstablishmentDTO login(LoginRequest request) {
        Establishment establishment = establishmentRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessRuleViolationException("invalid email or password"));

        if (!passwordEncoder.matches(request.password(), establishment.getPasswordHash())) {
            throw new BusinessRuleViolationException("invalid email or password");
        }

        return establishmentMapper.toDto(establishment);
    }

    /**
     * Finds an active establishment by id.
     *
     * @param id the establishment's id
     * @return the establishment, without its password hash
     * @throws EntityNotFoundException if no active establishment has this id
     */
    public EstablishmentDTO getById(Long id) {
        return establishmentRepository.findById(id)
                .map(establishmentMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException(Establishment.class, id));
    }

    /**
     * Updates an establishment's own profile. A blank/absent {@code password}
     * in {@code dto} keeps the current password hash; a non-blank one is
     * hashed and replaces it.
     *
     * @param id  the establishment's id
     * @param dto the profile data to apply
     * @return the updated establishment, without its password hash
     * @throws EntityNotFoundException if no active establishment has this id
     * @throws ValidationException     if the entity violates a constraint, the CNPJ is invalid,
     *                                 or the CNPJ/email is already registered by another establishment
     */
    public EstablishmentDTO updateProfile(Long id, EstablishmentUpdateDTO dto) {
        Establishment entity = establishmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Establishment.class, id));

        establishmentMapper.applyUpdate(dto, entity);
        if (dto.password() != null && !dto.password().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(dto.password()));
        }

        establishmentValidator.validateOperation(entity, BusinessOperation.UPDATE);

        Establishment saved = establishmentRepository.save(entity);
        return establishmentMapper.toDto(saved);
    }
}
