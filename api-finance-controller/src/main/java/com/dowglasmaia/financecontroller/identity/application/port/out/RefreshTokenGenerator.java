package com.dowglasmaia.financecontroller.identity.application.port.out;

import java.time.Duration;

public interface RefreshTokenGenerator {

    /** Token de renovação aleatório e opaco, com entropia suficiente para ser guardado só como hash. */
    String newToken();

    /** Hash estável (sem sal) do token, usado para guardar e procurar a sessão. */
    String hash(String token);

    Duration lifetime();
}
