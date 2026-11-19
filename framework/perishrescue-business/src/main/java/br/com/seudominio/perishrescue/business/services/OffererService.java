package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.validation.validators.OffererBusinessValidator;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.dtos.OffererDTO;
import br.com.seudominio.perishrescue.domain.dtos.OffererUpdateDTO;
import br.com.seudominio.perishrescue.domain.dtos.LoginRequest;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.OffererMapper;
import br.com.seudominio.perishrescue.persistence.repositories.OffererRepository;

import jakarta.validation.Validator;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that provides operations for {@link Offerer} entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Service
@Transactional
public class OffererService extends GenericService<Offerer, OffererDTO> {

    private final OffererRepository offererRepository;
    private final OffererMapper offererMapper;
    private final OffererBusinessValidator offererValidator;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor.
     *
     * @param repository            the offerer repository
     * @param mapper                the offerer mapper
     * @param validator             the bean validator
     * @param messageUtils          the message utils
     * @param offererValidator the business validator for the entity
     * @param passwordEncoder       the password encoder
     */
    public OffererService(
            OffererRepository repository,
            OffererMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            OffererBusinessValidator offererValidator,
            PasswordEncoder passwordEncoder) {
        super(repository, mapper, validator, messageUtils);
        this.offererRepository = repository;
        this.offererMapper = mapper;
        this.offererValidator = offererValidator;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new offerer: hashes the raw password, then validates
     * (bean constraints + CNPJ format/uniqueness + email uniqueness) before
     * persisting.
     *
     * @param dto the registration data, {@code password} carrying the raw password
     * @return the registered offerer, without its password hash
     * @throws ValidationException if the entity violates a constraint, the CNPJ is invalid,
     *                             or the CNPJ/email is already registered
     */
    @Override
    public OffererDTO save(OffererDTO dto) {
        Offerer entity = offererMapper.toEntity(dto);
        entity.setPasswordHash(passwordEncoder.encode(entity.getPasswordHash()));

        offererValidator.validateOperation(entity, BusinessOperation.CREATE);

        Offerer saved = offererRepository.save(entity);
        return offererMapper.toDto(saved);
    }

    /**
     * Validates login credentials against a registered offerer.
     *
     * @param request the login credentials
     * @return the authenticated offerer, without its password hash
     * @throws BusinessRuleViolationException if the email is unknown or the password doesn't match
     */
    public OffererDTO login(LoginRequest request) {
        Offerer offerer = offererRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessRuleViolationException("invalid email or password"));

        if (!passwordEncoder.matches(request.password(), offerer.getPasswordHash())) {
            throw new BusinessRuleViolationException("invalid email or password");
        }

        return offererMapper.toDto(offerer);
    }

    /**
     * Finds an active offerer by id.
     *
     * @param id the offerer's id
     * @return the offerer, without its password hash
     * @throws EntityNotFoundException if no active offerer has this id
     */
    public OffererDTO getById(Long id) {
        return offererRepository.findById(id)
                .map(offererMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException(Offerer.class, id));
    }

    /**
     * Updates an offerer's own profile. A blank/absent {@code password}
     * in {@code dto} keeps the current password hash; a non-blank one is
     * hashed and replaces it.
     *
     * @param id  the offerer's id
     * @param dto the profile data to apply
     * @return the updated offerer, without its password hash
     * @throws EntityNotFoundException if no active offerer has this id
     * @throws ValidationException     if the entity violates a constraint, the CNPJ is invalid,
     *                                 or the CNPJ/email is already registered by another offerer
     */
    public OffererDTO updateProfile(Long id, OffererUpdateDTO dto) {
        Offerer entity = offererRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Offerer.class, id));

        offererMapper.applyUpdate(dto, entity);
        if (dto.password() != null && !dto.password().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(dto.password()));
        }

        offererValidator.validateOperation(entity, BusinessOperation.UPDATE);

        Offerer saved = offererRepository.save(entity);
        return offererMapper.toDto(saved);
    }
}
