package com.dowglasmaia.financecontroller.identity.infrastructure.persistence;

import com.dowglasmaia.financecontroller.identity.application.port.out.UserRepository;
import com.dowglasmaia.financecontroller.identity.domain.Email;
import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository users;

    JpaUserRepositoryAdapter(SpringDataUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(Email email) {
        return users.findByEmail(email.value()).map(IdentityPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId id) {
        return users.findById(id.value()).map(IdentityPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        return users.existsByEmail(email.value());
    }

    /** Flush imediato: a violação de unicidade surge dentro do caso de uso, não no commit. */
    @Override
    @Transactional
    public User save(User user) {
        return IdentityPersistenceMapper.toDomain(users.saveAndFlush(IdentityPersistenceMapper.toEntity(user)));
    }
}
