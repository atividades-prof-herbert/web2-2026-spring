# Aula 10 (atividade): consultas derivadas, unicidade de email e análise do que ficou exposto

Leia [10_auth_login.md](../10_auth_login.md) antes de começar. A atividade tem três partes. As
duas primeiras são de implementação, no projeto `casalapp`. A terceira é de análise escrita, e
prepara o terreno das aulas 11, 12 e 13.

---

## Parte 1: os métodos mágicos do `repository`

Esta parte exercita a seção 3 da aula. Todo o trabalho acontece em
`repository/UsuarioRepository.java`, sem escrever uma linha de SQL.

### 1. Declarar consultas derivadas a partir de um enunciado

Declare no `UsuarioRepository` os métodos que respondem às três perguntas abaixo. Para cada
um, escolha o **tipo de retorno adequado** usando a tabela da seção 3.5 e escreva, em um
comentário acima do método, qual consulta você espera que o Spring Data derive.

- [ ] Todos os usuários de uma casa, a partir do id da casa.
- [ ] Quantos usuários existem em uma determinada casa.
- [ ] Todos os usuários cujo nome contenha um trecho digitado, ignorando maiúsculas e
      minúsculas.

Depois responda: por que o primeiro método **não** poderia devolver `Optional<Usuario>`?

### 2. Provocar o erro de propósito

Troque o nome de um dos métodos por uma versão com erro de digitação, por exemplo
`findByEmai`, e tente subir a aplicação.

- [ ] Rode `./mvnw compile` e registre o resultado.
- [ ] Rode `./mvnw spring-boot:run` e copie a mensagem de erro completa.

Responda: em que momento do ciclo de vida da aplicação o erro apareceu? Por que o compilador
Java não reclamou antes? A mensagem fala em `property` ou em `coluna`, e o que isso revela
sobre em qual nível o Spring Data trabalha?

Desfaça a alteração ao terminar.

### 3. A saída pela `@Query`

- [ ] Reescreva `findByEmail` usando `@Query` com JPQL, mantendo exatamente o mesmo
      comportamento e sem alterar o `AuthService`.

Responda: neste caso específico, a versão com `@Query` trouxe algum ganho? Em que situação ela
passaria a ser a melhor escolha? Volte ao final da seção 3.7 antes de responder.

---

## Parte 2: unicidade de email no cadastro

Na aula, a proteção contra email repetido foi implementada apenas no método `criar` do
`UsuarioService`. Os métodos `atualizar` (PUT) e `atualizarParcial` (PATCH) continuam
desprotegidos: eles deixam alterar o email de um usuário para um email que já pertence a
outro, e o resultado é a exceção do banco chegando ao cliente como `500`.

- [ ] Implemente a mesma proteção nos dois métodos, devolvendo `409 Conflict` com
      `ErroResponseDTO`.
- [ ] Garanta que atualizar o usuário 3 mantendo o email **dele mesmo** continue funcionando
      normalmente.

Atenção ao detalhe que o `criar` não tem: encontrar o email no banco não basta para
caracterizar conflito. O que mais a condição precisa verificar? Escreva a condição em uma
frase antes de escrevê-la em Java.

- [ ] Teste os três casos pelo Swagger UI: email novo, email do próprio usuário, email de
      outro usuário.

---

## Parte 3: análise do que esta aula deixou exposto

Responda por escrito. Não há código a entregar nesta parte.

### 4. Igualdade de senha dentro da consulta

Suponha que o `AuthService` fosse reescrito para usar uma consulta derivada
`findByEmailAndSenha`, fazendo o banco comparar a senha, em vez de buscar por email e comparar
em Java com `equals`.

A aplicação continuaria funcionando igual hoje? E na aula 11, quando a senha deixar de ser
armazenada em texto puro e passar a ser um hash, qual das duas versões continuará possível?
Justifique explicando o que o hash faz com a possibilidade de comparar por igualdade.

### 5. O que um estranho consegue sem senha nenhuma

A rota `GET /usuarios` devolve nome, email e casa de **todos** os usuários do sistema, para
quem quer que a chame, sem autenticação.

Descreva que informação um atacante obtém com essa única chamada, e explique como ela se
combina com o problema de enumeração de usuários discutido na seção 6.3 da aula. A mensagem
genérica do `401` ainda protege alguma coisa, enquanto essa rota existir?

### 6. Documentar a própria defesa

- [ ] Adicione `@Schema(description = ...)` nos campos de `LoginRequestDTO` e um
      `@Operation(summary = ...)` no `AuthController` explicando a regra da mensagem genérica
      de erro.
- [ ] Abra o Swagger UI e confira o resultado.

Responda: documentar publicamente que a mensagem de erro é genérica de propósito ajuda ou
prejudica a segurança? Relacione sua resposta com a ideia de que a segurança de um sistema não
deve depender do segredo do seu funcionamento, apenas do segredo das credenciais.

---

## Checklist de verificação

- [ ] `UsuarioRepository` tem os três métodos novos declarados, cada um com o tipo de retorno
      justificado em comentário, e nenhum deles tem implementação escrita à mão.
- [ ] A aplicação sobe sem erro depois de desfeito o erro de digitação proposital.
- [ ] `PUT /usuarios/{id}` e `PATCH /usuarios/{id}` devolvem `409` ao tentar usar o email de
      outro usuário, e `200` ao manter o email do próprio usuário.
- [ ] Nenhuma rota devolve `500` nos testes feitos.
- [ ] Nenhum DTO de resposta do projeto expõe o campo `senha`.
- [ ] As respostas das questões 2, 3, 4, 5 e 6 estão escritas e entregues junto com o código.
