-- ============================================================================
-- V1 - Schema inicial (MySQL 8 / InnoDB / utf8mb4)
-- Espelha o mapeamento JPA: herança JOINED (usuario -> aluno | instrutor | recepcao).
-- ============================================================================

CREATE TABLE usuario (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    nome       VARCHAR(120) NOT NULL,
    cpf        VARCHAR(14)  NOT NULL,
    email      VARCHAR(160) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    perfil     VARCHAR(20)  NOT NULL,
    criado_em  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_cpf UNIQUE (cpf),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ALUNO', 'RECEPCAO', 'INSTRUTOR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE aluno (
    usuario_id         BIGINT       NOT NULL,
    data_nascimento    DATE         NULL,
    telefone           VARCHAR(20)  NULL,
    objetivo           VARCHAR(255) NULL,
    restricoes_medicas VARCHAR(500) NULL,
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_aluno_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE instrutor (
    usuario_id       BIGINT       NOT NULL,
    cref             VARCHAR(20)  NULL,
    especialidade    VARCHAR(100) NULL,
    turno            VARCHAR(10)  NULL,
    data_contratacao DATE         NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_instrutor_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT ck_instrutor_turno CHECK (turno IS NULL OR turno IN ('MANHA', 'TARDE', 'NOITE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE recepcao (
    usuario_id BIGINT      NOT NULL,
    id_func    VARCHAR(20) NULL,
    turno      VARCHAR(10) NULL,
    ramal      VARCHAR(10) NULL,
    ativo      BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_recepcao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT ck_recepcao_turno CHECK (turno IS NULL OR turno IN ('MANHA', 'TARDE', 'NOITE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE plano (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    nome                VARCHAR(80)    NOT NULL,
    duracao_meses       INT            NOT NULL,
    valor_mensal        DECIMAL(10, 2) NOT NULL,
    limite_aulas_semana INT            NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_plano_duracao CHECK (duracao_meses IN (1, 3, 12))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE plano_modalidades (
    plano_id   BIGINT      NOT NULL,
    modalidade VARCHAR(30) NOT NULL,
    PRIMARY KEY (plano_id, modalidade),
    CONSTRAINT fk_plano_modalidades_plano FOREIGN KEY (plano_id) REFERENCES plano (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE matricula (
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    aluno_id           BIGINT         NOT NULL,
    plano_id           BIGINT         NOT NULL,
    data_inicio        DATE           NOT NULL,
    data_fim           DATE           NOT NULL,
    status_matricula   VARCHAR(20)    NOT NULL,
    data_encerramento  DATE           NULL,
    multa_cancelamento DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    CONSTRAINT fk_matricula_aluno FOREIGN KEY (aluno_id) REFERENCES aluno (usuario_id),
    CONSTRAINT fk_matricula_plano FOREIGN KEY (plano_id) REFERENCES plano (id),
    CONSTRAINT ck_matricula_status CHECK (status_matricula IN ('ATIVA', 'CANCELADA', 'ENCERRADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_matricula_aluno_status ON matricula (aluno_id, status_matricula);

CREATE TABLE mensalidade (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    matricula_id     BIGINT         NOT NULL,
    numero           INT            NOT NULL,
    valor            DECIMAL(10, 2) NOT NULL,
    vencimento       DATE           NOT NULL,
    periodo_inicial  DATE           NOT NULL,
    periodo_fim      DATE           NOT NULL,
    status_pagamento VARCHAR(20)    NOT NULL,
    data_pagamento   DATE           NULL,
    valor_pago       DECIMAL(10, 2) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_mensalidade_matricula FOREIGN KEY (matricula_id) REFERENCES matricula (id) ON DELETE CASCADE,
    CONSTRAINT uk_mensalidade_matricula_numero UNIQUE (matricula_id, numero),
    CONSTRAINT ck_mensalidade_status CHECK (status_pagamento IN ('PENDENTE', 'PAGO', 'CANCELADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_mensalidade_status_vencimento ON mensalidade (status_pagamento, vencimento);

CREATE TABLE aula_coletiva (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    modalidade      VARCHAR(30) NOT NULL,
    instrutor_id    BIGINT      NOT NULL,
    dia_semana      VARCHAR(10) NOT NULL,
    horario         TIME(6)     NOT NULL,
    duracao_minutos INT         NOT NULL,
    capacidade      INT         NOT NULL,
    criada_em       DATE        NOT NULL,
    ativa           BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_aula_instrutor FOREIGN KEY (instrutor_id) REFERENCES instrutor (usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_aula_instrutor_dia ON aula_coletiva (instrutor_id, dia_semana);

CREATE TABLE inscricao (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    aula_id      BIGINT      NOT NULL,
    aluno_id     BIGINT      NOT NULL,
    data_aula    DATE        NOT NULL,
    status       VARCHAR(20) NOT NULL,
    presenca     VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    criada_em    DATETIME(6) NOT NULL,
    cancelada_em DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inscricao_aula FOREIGN KEY (aula_id) REFERENCES aula_coletiva (id),
    CONSTRAINT fk_inscricao_aluno FOREIGN KEY (aluno_id) REFERENCES aluno (usuario_id),
    CONSTRAINT ck_inscricao_status CHECK (status IN ('CONFIRMADA', 'LISTA_ESPERA', 'CANCELADA')),
    CONSTRAINT ck_inscricao_presenca CHECK (presenca IN ('PENDENTE', 'PRESENTE', 'FALTA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_inscricao_sessao ON inscricao (aula_id, data_aula, status);
CREATE INDEX idx_inscricao_aluno ON inscricao (aluno_id, data_aula);

CREATE TABLE ficha_treino (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    aluno_id    BIGINT       NOT NULL,
    instrutor_id BIGINT      NOT NULL,
    criada_em   DATETIME(6)  NOT NULL,
    is_atual    BOOLEAN      NOT NULL DEFAULT TRUE,
    observacoes VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ficha_aluno FOREIGN KEY (aluno_id) REFERENCES aluno (usuario_id),
    CONSTRAINT fk_ficha_instrutor FOREIGN KEY (instrutor_id) REFERENCES instrutor (usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ficha_aluno_atual ON ficha_treino (aluno_id, is_atual);

CREATE TABLE item_treino (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    ficha_treino_id BIGINT        NOT NULL,
    ordem           INT           NOT NULL,
    exercicio       VARCHAR(120)  NOT NULL,
    series          INT           NOT NULL,
    repeticoes      INT           NOT NULL,
    carga_kg        DECIMAL(6, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_item_treino_ficha FOREIGN KEY (ficha_treino_id) REFERENCES ficha_treino (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE checkin (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    aluno_id          BIGINT       NOT NULL,
    data_hora_entrada DATETIME(6)  NOT NULL,
    tipo_acesso       VARCHAR(20)  NOT NULL,
    acesso_liberado   BOOLEAN      NOT NULL,
    motivo_bloqueio   VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_checkin_aluno FOREIGN KEY (aluno_id) REFERENCES aluno (usuario_id),
    CONSTRAINT ck_checkin_tipo CHECK (tipo_acesso IN ('CATRACA', 'RECEPCAO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_checkin_aluno_data ON checkin (aluno_id, data_hora_entrada);
