package com.dowglasmaia.financecontroller.identity.application.usecase;

import com.dowglasmaia.financecontroller.identity.application.port.out.UserRepository;
import com.dowglasmaia.financecontroller.identity.domain.User;
import com.dowglasmaia.financecontroller.identity.domain.UserId;
import com.dowglasmaia.financecontroller.shared.error.DomainException;
import com.dowglasmaia.financecontroller.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentUserUseCase {

    private final UserRepository users;

    GetCurrentUserUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public User execute(UserId id) {
        return users.findById(id).orElseThrow(() -> new DomainException(ErrorCode.UNAUTHENTICATED));
    }
}
