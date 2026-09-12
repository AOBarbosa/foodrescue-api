package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.business.validation.validators.ConsumerBusinessValidator;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.dtos.ConsumerDTO;
import br.com.seudominio.foodrescue.domain.dtos.LoginRequest;
import br.com.seudominio.foodrescue.domain.entities.Consumer;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.mappers.ConsumerMapper;
import br.com.seudominio.foodrescue.persistence.repositories.ConsumerRepository;

import jakarta.validation.Validator;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that provides operations for {@link Consumer} entity.
 * Consumer registration/login has no dedicated UC of its own — it's covered
 * by the architectural foundation (issue #1), since UC09 requires it as a
 * precondition.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Service
@Transactional
public class ConsumerService extends GenericService<Consumer, ConsumerDTO> {

    private final ConsumerRepository consumerRepository;
    private final ConsumerMapper consumerMapper;
    private final ConsumerBusinessValidator consumerValidator;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor.
     *
     * @param repository        the consumer repository
     * @param mapper            the consumer mapper
     * @param validator         the bean validator
     * @param messageUtils      the message utils
     * @param consumerValidator the business validator for the entity
     * @param passwordEncoder   the password encoder
     */
    public ConsumerService(
            ConsumerRepository repository,
            ConsumerMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            ConsumerBusinessValidator consumerValidator,
            PasswordEncoder passwordEncoder) {
        super(repository, mapper, validator, messageUtils);
        this.consumerRepository = repository;
        this.consumerMapper = mapper;
        this.consumerValidator = consumerValidator;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new consumer: hashes the raw password, then validates
     * (bean constraints + email uniqueness) before persisting.
     *
     * @param dto the registration data, {@code password} carrying the raw password
     * @return the registered consumer, without its password hash
     * @throws ValidationException if the entity violates a constraint or the email is already registered
     */
    @Override
    public ConsumerDTO save(ConsumerDTO dto) {
        Consumer entity = consumerMapper.toEntity(dto);
        entity.setPasswordHash(passwordEncoder.encode(entity.getPasswordHash()));

        consumerValidator.validateOperation(entity, BusinessOperation.CREATE);

        Consumer saved = consumerRepository.save(entity);
        return consumerMapper.toDto(saved);
    }

    /**
     * Validates login credentials against a registered consumer.
     *
     * @param request the login credentials
     * @return the authenticated consumer, without its password hash
     * @throws BusinessRuleViolationException if the email is unknown or the password doesn't match
     */
    public ConsumerDTO login(LoginRequest request) {
        Consumer consumer = consumerRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessRuleViolationException("invalid email or password"));

        if (!passwordEncoder.matches(request.password(), consumer.getPasswordHash())) {
            throw new BusinessRuleViolationException("invalid email or password");
        }

        return consumerMapper.toDto(consumer);
    }
}
