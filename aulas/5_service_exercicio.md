# Aula 5: Exercício

O exercício tem duas partes. Na primeira, você implementa um caso de uso novo (UC-5, `Gerenciar Casa`) e isso obriga a refatorar tudo o que já existe no projeto, porque toda categoria passa a pertencer a uma Casa. Na segunda parte, você faz o que já era esperado desde o início da aula: criar o `TransacaoService`.

Consulte [documentacao/1_casos_uso.md](../documentacao/1_casos_uso.md) (UC 5) e [documentacao/diagrama_er.mmd](../documentacao/diagrama_er.mmd) antes de começar.

---

## Parte 1: UC-5 — Gerenciar Casa

O UC-5 descreve a entidade `Casa`: o espaço compartilhado ao qual categorias, orçamentos e lançamentos pertencem. O projeto ainda não tem `Usuario` nem autenticação, então esta parte do exercício se limita ao que já é possível construir com o que temos: a entidade `Casa` em si, e o relacionamento dela com `Categoria`. A parte de convite/associação de usuário (UC-11) fica de fora por enquanto — ela depende de um `UsuarioService` que este projeto ainda não tem.

### 1. Criar `Casa`

- [ ] Crie `model/Casa.java`, classe tradicional (não `record`), com `id`, `nome` e `codigoConvite`
- [ ] Crie `dto/CasaRequest.java` (`record` com `nome`, sem `id`)
- [ ] Crie `dto/CasaResponse.java` (`record` com `id`, `nome`, `codigoConvite`)

### 2. Criar `CasaService` e `CasaController`

- [ ] Crie `service/CasaService.java`, anotado com `@Service`, seguindo exatamente o padrão do `CategoriaService`: lista interna de `Casa`, `proximoId`, método `toResponse(...)` privado, e os métodos públicos `listar()`, `buscarPorId(int id)`, `criar(CasaRequest request)`, `atualizar(int id, CasaRequest request)`, `atualizarParcial(int id, CasaRequest request)` e `deletar(int id)` — todos recebendo e devolvendo só `dto`, nunca `model.Casa`
- [ ] Use `null` como retorno quando o id não existe, do mesmo jeito que `CategoriaService` faz (sem `Optional`)
- [ ] Crie `controller/CasaController.java`, recebendo `CasaService` no construtor (sem `@Autowired`), com os endpoints `GET /casas`, `GET /casas/{id}`, `POST /casas`, `PUT /casas/{id}`, `PATCH /casas/{id}` e `DELETE /casas/{id}`

### 3. Refatorar `Categoria` para pertencer a uma `Casa`

Toda categoria passa a existir dentro de uma Casa. Isso significa alterar as classes que já existiam:

- [ ] Adicione `int casaId` em `model/Categoria.java`
- [ ] Adicione `Integer casaId` em `dto/CategoriaRequest.java`
- [ ] Adicione `int casaId` e `String casaNome` em `dto/CategoriaResponse.java`
- [ ] No `CategoriaService`, receba `CasaService` no construtor e use os métodos públicos dele (que já devolvem `CasaResponse`) para montar o `casaNome`, do mesmo jeito que o `TransacaoService` vai fazer com `CategoriaService` na parte 2
- [ ] Atualize `criar`, `atualizar` e `atualizarParcial` do `CategoriaService` para receber e persistir o `casaId`

### 4. Testar

- [ ] `POST /casas` cria uma Casa e devolve `201`
- [ ] `POST /categorias` informando um `casaId` existente devolve a categoria com `casaNome` preenchido
- [ ] `POST /categorias` com um `casaId` inexistente devolve a categoria com `casaNome: null`, sem quebrar
- [ ] `GET /categorias` continua funcionando e agora traz `casaId`/`casaNome` em cada item
- [ ] Os testes de `PUT`, `PATCH` e `DELETE` de `Categoria` das aulas anteriores continuam passando, agora considerando o campo `casaId`

---

## Parte 2: `TransacaoService`

Crie o `TransacaoService`, seguindo o mesmo padrão do `CategoriaService`, e remova a lista de transações e o método de conversão de dentro do `TransacaoController`.

### 1. Criar o service

- [ ] Crie `service/TransacaoService.java`, anotado com `@Service`
- [ ] Mova `proximoId`, a `List<Transacao> transacoes` e a lógica de `toResponse(...)` do `TransacaoController` para dentro do service
- [ ] Os métodos públicos devem receber e devolver só `dto` (`TransacaoRequest`/`TransacaoResponse`), nunca `model.Transacao`, seguindo a regra de visibilidade da aula 5
- [ ] Implemente `listar()`, `buscarPorId(int id)`, `criar(...)`, `atualizar(...)`, `atualizarParcial(...)` e `deletar(int id)`, no mesmo formato usado em `CategoriaService`, devolvendo `null` quando o id não existe

### 2. Resolver o join sem duplicar dados

- [ ] No `TransacaoService`, receba `CategoriaService` como parâmetro do construtor, em vez de manter uma cópia local de `List<Categoria>`
- [ ] Use os métodos públicos do `CategoriaService` (que já devolvem `CategoriaResponse`) para montar o `categoriaNome`, em vez de acessar `model.Categoria` diretamente

### 3. Atualizar o controller

- [ ] Troque a `List<Transacao>` e o `toResponse(...)` do `TransacaoController` por um atributo `final TransacaoService transacaoService`, recebido no construtor
- [ ] Reescreva cada método do controller para chamar o service, igual ao que foi feito em `CategoriaController`
- [ ] Confira que o `TransacaoController` não importa mais `model.Transacao` nem `model.Categoria`

### 4. Testar

- [ ] `GET /transacoes` continua devolvendo o `categoriaNome` corretamente
- [ ] Criar uma categoria nova com `POST /categorias` (agora com `casaId`) e, em seguida, criar uma transação com esse `categoriaId` deve devolver o `categoriaNome` certo, sem duplicação de dados
- [ ] Todos os testes de `PUT`, `PATCH` e `DELETE` feitos nas aulas anteriores continuam passando
