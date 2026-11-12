package com.gbr.AEP.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StatusProjeto")
class StatusProjetoTest {

    @Test
    @DisplayName("deve conter os quatro status do ciclo de vida")
    void deveConterQuatroStatus() {
        assertThat(StatusProjeto.values())
                .containsExactlyInAnyOrder(
                        StatusProjeto.PLANEJADO,
                        StatusProjeto.EM_ANDAMENTO,
                        StatusProjeto.CONCLUIDO,
                        StatusProjeto.CANCELADO);
    }

    @Test
    @DisplayName("deve resolver por nome")
    void deveResolverPorNome() {
        assertThat(StatusProjeto.valueOf("EM_ANDAMENTO"))
                .isEqualTo(StatusProjeto.EM_ANDAMENTO);
    }
}
