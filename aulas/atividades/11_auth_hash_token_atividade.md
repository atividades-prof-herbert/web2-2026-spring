# Aula 11 (atividade): Swagger autenticado, migração das senhas antigas e autorização por recurso

Leia [11_auth_hash_token.md](../11_auth_hash_token.md) antes de começar. A atividade tem quatro
partes. As três primeiras são de implementação, no projeto `casalapp`. A quarta é de análise
escrita, e fecha a sequência de autenticação iniciada na aula 10.

A parte 3 é o centro da atividade: ela resolve o defeito que a seção 8 da aula apontou como o
mais grave.

---

## Parte 1: o botão Authorize no Swagger UI

Como a aula registrou no final da seção 6, o Swagger UI documenta as rotas protegidas mas não
oferece onde informar o token, de modo que testar qualquer uma delas por lá devolve `401`.

Falta declarar ao springdoc que a API usa um esquema de segurança. Isso se faz com duas
anotações, `@SecurityScheme` e `@SecurityRequirement`, pesquisando na documentação do springdoc
indicada na referência 1 desta atividade.

- [ ] Declare, em uma classe de configuração, um esquema de segurança do tipo `HTTP`, com
      esquema `bearer`, e dê a ele um nome.
- [ ] Faça as rotas protegidas exigirem esse esquema, de modo que o botão **Authorize** apareça
      no Swagger UI.
- [ ] Confira que `POST /auth/login` e `POST /usuarios` continuam funcionando no Swagger **sem**
      token informado.
- [ ] Teste `GET /auth/eu` pelo Swagger, informando o token obtido no login.

Responda: essa declaração muda o comportamento do servidor ou apenas a documentação? Se alguém
apagasse o `addPathPatterns` do `WebConfig` e mantivesse essas anotações, as rotas continuariam
protegidas? E o contrário?

---

## Parte 2: migrar o banco à mão

A seção 2.6 da aula mostrou que o `ddl-auto=update` criou a coluna `senha_hash` e deixou a
coluna `senha` intacta, com as senhas antigas legíveis.

- [ ] Antes de qualquer alteração, rode e registre o resultado:

```sql
SELECT id, nome, email, senha, senha_hash FROM usuario;
```

- [ ] Apague a coluna antiga e os usuários que ficaram sem hash, como a aula indicou.
- [ ] Cadastre dois usuários **com a mesma senha**, em casas diferentes, e compare os dois
      valores de `senha_hash`.

Responda: por que os dois hashes são diferentes, se a senha é a mesma? Que consequência isso
tem para o código que confere o login, isto é, por que o `matches` é necessário e uma
comparação com `equals` não serviria?

### Um erro provocado de propósito

Troque temporariamente o `atualizarParcial` do `UsuarioService` por uma versão que aplique
`passwordEncoder.encode` sempre, inclusive quando `request.senha()` vier `null`, como o
operador ternário da aula 10 fazia.

- [ ] Faça um `PATCH /usuarios/{id}` alterando **apenas** o nome do usuário.
- [ ] Em seguida, tente fazer login com a senha correta desse usuário.

Responda: o `PATCH` devolveu erro? O login devolveu erro? Em que momento o defeito se tornou
perceptível, e por que esse tipo de defeito é especialmente perigoso? Relacione com o que a
seção 2.7 da aula diz sobre aplicar hash em um hash. Desfaça a alteração ao terminar.

---

## Parte 3: autorização em nível de objeto

Esta parte corrige o primeiro defeito da seção 8 da aula. Hoje, um usuário autenticado lê e
altera recursos de **outras** casas, bastando trocar o id na URL.

### 1. Reproduzir a falha antes de corrigir

Cadastre duas casas, duas categorias (uma em cada casa), duas transações (uma em cada
categoria) e dois usuários (um em cada casa). Faça login com o usuário da casa B e, com o token
dele, registre o que acontece:

- [ ] `GET /transacoes` (a rota corrigida na seção 5.3 da aula)
- [ ] `GET /transacoes/{id}` usando o id da transação da casa A
- [ ] `PUT /transacoes/{id}` alterando a transação da casa A
- [ ] `DELETE /transacoes/{id}` apagando a transação da casa A
- [ ] `GET /categorias`
- [ ] `GET /usuarios`

Escreva, antes de programar, uma frase para cada rota dizendo o que ela **deveria** responder.

### 2. Fechar as rotas de `Transacao`

A regra é: um usuário só enxerga e só altera transações cuja categoria pertença à casa dele.

- [ ] Faça `buscarPorId`, `atualizar`, `atualizarParcial` e `deletar` receberem a informação de
      qual casa está pedindo, e recusarem o que não pertence a ela.
- [ ] Faça o `criar` recusar uma `categoriaId` que seja de outra casa.

Duas decisões de projeto precisam ser tomadas, e a justificativa delas vale mais do que o
código:

**Onde a verificação mora.** O `controller` já tem o usuário, via `@RequestAttribute`, e seria
mais rápido verificar ali. Decida entre verificar no `controller` ou passar a informação ao
`service` e verificar lá, e justifique usando o critério de responsabilidades da aula 5. Lembre
que a mesma regra precisa valer para qualquer outra porta de entrada que venha a existir.

**Qual status devolver.** Para um recurso que existe mas não é seu, há duas respostas
defensáveis:

| Status | Argumento a favor |
|---|---|
| `403 Forbidden` | É literalmente o caso: sei quem você é e você não pode. |
| `404 Not Found` | Não revela que aquele id existe. Para quem pergunta, o recurso simplesmente não está lá. |

- [ ] Escolha uma das duas, implemente de forma consistente em todas as rotas, e escreva por
      que escolheu. Relacione seu argumento com a discussão de enumeração de usuários da seção
      6.3 da aula 10.

### 3. Fechar as rotas de `Categoria` e `Usuario`

- [ ] `GET /categorias` deve listar apenas as categorias da casa de quem pediu, com uma consulta
      derivada no `CategoriaRepository`.
- [ ] As demais rotas de `Categoria` devem seguir a mesma regra das transações.
- [ ] `GET /usuarios` deve listar apenas os usuários da casa de quem pediu.

Responda: depois dessa mudança, o que exatamente um usuário autenticado consegue descobrir
sobre pessoas de outras casas? O problema de enumeração de usuários discutido na aula 10
continua existindo em algum lugar?

### 4. Conferir no frontend

- [ ] Abra a tela de transações com cada um dos dois usuários e confirme que cada um vê apenas o
      que é seu, inclusive na lista de categorias do formulário.
- [ ] Verifique se alguma tela quebrou por receber `403` ou `404` onde antes recebia `200`.

---

## Parte 4: análise do esquema de sessão

Responda por escrito. Não há código a entregar nesta parte.

### 5. Uma sessão por dispositivo

Faça login com o mesmo usuário duas vezes seguidas e consulte a tabela:

```sql
SELECT id, valor, usuario_id, criado_em, expira_em FROM token;
```

- [ ] Registre quantas linhas existem.
- [ ] Faça `POST /auth/logout` com **um** dos dois tokens e teste o outro em seguida.

Responda: o segundo token continuou valendo? Isso é o comportamento desejado? Descreva, em
palavras, o que precisaria mudar no `AuthService` para oferecer um "sair de todos os
dispositivos", e por que essa operação é importante quando alguém desconfia que sua senha
vazou. Que método de consulta derivada o `TokenRepository` precisaria ter?

### 6. O prazo de validade

O token da aula vale oito horas a partir do login, sem renovação.

Responda: o que acontece com um usuário que está usando o sistema ativamente quando as oito
horas terminam? Descreva duas estratégias alternativas, uma que prorrogue o prazo a cada
requisição e outra que use dois tokens, um de acesso curto e um de renovação longo. Qual das
duas protege melhor contra um token roubado, e por quê?

### 7. O que o `JWT` mudaria

Suponha que o token deixasse de ser opaco e passasse a ser um `JWT`, isto é, um token assinado
que contém dentro de si o id, o nome e a casa do usuário.

Responda, comparando com a implementação desta aula:

- [ ] Quais consultas ao banco desapareceriam de cada requisição?
- [ ] O que aconteceria com o `POST /auth/logout`?
- [ ] O que aconteceria se o usuário mudasse de casa no meio de uma sessão?
- [ ] Por que o conteúdo de um `JWT` pode ser lido por qualquer um, e ainda assim o token é
      considerado confiável?

### 8. Onde está a proteção

Apague temporariamente a chamada a `exigirLogin()` do início de `js/pages/transacoes.js`, limpe
o `localStorage` pelo console do navegador e abra a página.

- [ ] Registre o que aparece na tela e o que aparece na aba Network das ferramentas de
      desenvolvedor.

Responda: algum dado da casa de alguém apareceu? Que conclusão isso permite tirar sobre o papel
de uma verificação feita no navegador? Explique por que o inverso não vale, isto é, por que
apagar a linha correspondente do `addPathPatterns` no `WebConfig` teria consequência
completamente diferente. Desfaça a alteração ao terminar.

---

## Checklist de verificação

- [ ] O Swagger UI tem o botão **Authorize**, e com ele é possível testar `GET /auth/eu`.
- [ ] A coluna `senha` não existe mais na tabela `usuario`, e nenhuma senha legível está no
      banco.
- [ ] Dois usuários com a mesma senha têm valores diferentes em `senha_hash`.
- [ ] Um `PATCH` que não envia senha não invalida a senha do usuário.
- [ ] Com o token do usuário da casa B, nenhuma rota de `Transacao`, `Categoria` ou `Usuario`
      devolve dado da casa A, em nenhum dos métodos HTTP.
- [ ] A verificação de propriedade está em um só lugar por recurso, e o status escolhido é o
      mesmo em todas as rotas.
- [ ] Nenhuma rota devolve `500` nos testes feitos.
- [ ] As duas telas do frontend continuam funcionando para os dois usuários.
- [ ] As respostas das questões 1, 2, 3, 5, 6, 7 e 8 estão escritas e entregues junto com o
      código.

---

## Referências

1. springdoc-openapi, configuração de esquemas de segurança e do botão Authorize:
   https://springdoc.org/#how-do-i-add-authorization-header-in-requests
2. OpenAPI 3, objeto `Security Scheme`:
   https://spec.openapis.org/oas/v3.1.0#security-scheme-object
3. OWASP API Security Top 10, `API1:2023 Broken Object Level Authorization`:
   https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/
4. OWASP, Session Management Cheat Sheet, expiração e renovação de sessão:
   https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
5. RFC 7519, `JSON Web Token (JWT)`:
   https://www.rfc-editor.org/rfc/rfc7519.html
6. RFC 9110, HTTP Semantics, seção 15.5.4, `403 Forbidden`:
   https://www.rfc-editor.org/rfc/rfc9110.html#name-403-forbidden
