-- ============================================================================
-- V2 - Carga inicial: 1 recepção, 2 instrutores e os planos básicos.
-- Senhas em BCrypt (custo 10):
--   recepcao@academia.com   -> Recepcao@123
--   instrutor@academia.com  -> Instrutor@123
--   instrutor2@academia.com -> Instrutor@123
-- Troque as senhas após o primeiro acesso em ambientes reais.
-- ============================================================================

-- Recepção
INSERT INTO usuario (nome, cpf, email, senha_hash, perfil, criado_em)
VALUES ('Recepção Academia', '11144477735', 'recepcao@academia.com',
        '$2a$10$SF8l2/pwUvB6oLcInrbWaOBo2vOcqsGT7r9Zrh9Jh7zrfvOXbJKka', 'RECEPCAO', NOW(6));
SET @recepcao_id = LAST_INSERT_ID();
INSERT INTO recepcao (usuario_id, id_func, turno, ramal, ativo)
VALUES (@recepcao_id, 'REC-001', 'MANHA', '100', TRUE);

-- Instrutores
INSERT INTO usuario (nome, cpf, email, senha_hash, perfil, criado_em)
VALUES ('Carlos Instrutor', '52998224725', 'instrutor@academia.com',
        '$2a$10$bWLtGg2yZerhhmmKU2OcvuTxqWwRjEso06kPiKr42TFnY/s1O3laS', 'INSTRUTOR', NOW(6));
SET @instrutor1_id = LAST_INSERT_ID();
INSERT INTO instrutor (usuario_id, cref, especialidade, turno, data_contratacao, ativo)
VALUES (@instrutor1_id, '000001-G/SP', 'Musculação', 'MANHA', CURRENT_DATE, TRUE);

INSERT INTO usuario (nome, cpf, email, senha_hash, perfil, criado_em)
VALUES ('Ana Instrutora', '39053344705', 'instrutor2@academia.com',
        '$2a$10$bWLtGg2yZerhhmmKU2OcvuTxqWwRjEso06kPiKr42TFnY/s1O3laS', 'INSTRUTOR', NOW(6));
SET @instrutor2_id = LAST_INSERT_ID();
INSERT INTO instrutor (usuario_id, cref, especialidade, turno, data_contratacao, ativo)
VALUES (@instrutor2_id, '000002-G/SP', 'Yoga e Pilates', 'NOITE', CURRENT_DATE, TRUE);

-- Planos básicos (duração de 1, 3 ou 12 meses)
INSERT INTO plano (nome, duracao_meses, valor_mensal, limite_aulas_semana)
VALUES ('Mensal Básico', 1, 99.90, 3);
SET @plano_mensal = LAST_INSERT_ID();
INSERT INTO plano_modalidades (plano_id, modalidade) VALUES
    (@plano_mensal, 'MUSCULACAO'),
    (@plano_mensal, 'SPINNING');

INSERT INTO plano (nome, duracao_meses, valor_mensal, limite_aulas_semana)
VALUES ('Trimestral Completo', 3, 89.90, 4);
SET @plano_trimestral = LAST_INSERT_ID();
INSERT INTO plano_modalidades (plano_id, modalidade) VALUES
    (@plano_trimestral, 'MUSCULACAO'),
    (@plano_trimestral, 'SPINNING'),
    (@plano_trimestral, 'FUNCIONAL'),
    (@plano_trimestral, 'YOGA');

INSERT INTO plano (nome, duracao_meses, valor_mensal, limite_aulas_semana)
VALUES ('Anual Premium', 12, 69.90, 7);
SET @plano_anual = LAST_INSERT_ID();
INSERT INTO plano_modalidades (plano_id, modalidade) VALUES
    (@plano_anual, 'MUSCULACAO'),
    (@plano_anual, 'SPINNING'),
    (@plano_anual, 'FUNCIONAL'),
    (@plano_anual, 'YOGA'),
    (@plano_anual, 'PILATES'),
    (@plano_anual, 'ZUMBA'),
    (@plano_anual, 'NATACAO'),
    (@plano_anual, 'LUTA');
