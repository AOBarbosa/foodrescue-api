package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.validation.validators.BeneficiaryBusinessValidator;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.dtos.BeneficiaryDTO;
import br.com.seudominio.perishrescue.domain.dtos.LoginRequest;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.mappers.BeneficiaryMapper;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;

import jakarta.validation.Validator;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that provides operations for {@link Beneficiary} entity.
 * Beneficiary registration/login has no dedicated UC of its own — it's covered
 * by the architectural foundation (issue #1), since UC09 requires it as a
 * precondition.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Service
@Transactional
public class BeneficiaryService extends GenericService<Beneficiary, BeneficiaryDTO> {

    private final BeneficiaryRepository beneficiaryRepository;
    private final BeneficiaryMapper beneficiaryMapper;
    private final BeneficiaryBusinessValidator beneficiaryValidator;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor.
     *
     * @param repository        the beneficiary repository
     * @param mapper            the beneficiary mapper
     * @param validator         the bean validator
     * @param messageUtils      the message utils
     * @param beneficiaryValidator the business validator for the entity
     * @param passwordEncoder   the password encoder
     */
    public BeneficiaryService(
            BeneficiaryRepository repository,
            BeneficiaryMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            BeneficiaryBusinessValidator beneficiaryValidator,
            PasswordEncoder passwordEncoder) {
        super(repository, mapper, validator, messageUtils);
        this.beneficiaryRepository = repository;
        this.beneficiaryMapper = mapper;
        this.beneficiaryValidator = beneficiaryValidator;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new beneficiary: hashes the raw password, then validates
     * (bean constraints + email uniqueness) before persisting.
     *
     * @param dto the registration data, {@code password} carrying the raw password
     * @return the registered beneficiary, without its password hash
     * @throws ValidationException if the entity violates a constraint or the email is already registered
     */
    @Override
    public BeneficiaryDTO save(BeneficiaryDTO dto) {
        Beneficiary entity = beneficiaryMapper.toEntity(dto);
        entity.setPasswordHash(passwordEncoder.encode(entity.getPasswordHash()));

        beneficiaryValidator.validateOperation(entity, BusinessOperation.CREATE);

        Beneficiary saved = beneficiaryRepository.save(entity);
        return beneficiaryMapper.toDto(saved);
    }

    /**
     * Validates login credentials against a registered beneficiary.
     *
     * @param request the login credentials
     * @return the authenticated beneficiary, without its password hash
     * @throws BusinessRuleViolationException if the email is unknown or the password doesn't match
     */
    public BeneficiaryDTO login(LoginRequest request) {
        Beneficiary beneficiary = beneficiaryRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessRuleViolationException("invalid email or password"));

        if (!passwordEncoder.matches(request.password(), beneficiary.getPasswordHash())) {
            throw new BusinessRuleViolationException("invalid email or password");
        }

        return beneficiaryMapper.toDto(beneficiary);
    }
}
