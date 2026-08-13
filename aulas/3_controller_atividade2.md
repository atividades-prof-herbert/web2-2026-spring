---

## Exercício

Adicione os endpoints `POST`, `PUT` e `PATCH` ao `TransacaoController` criado na aula anterior.

Uma transação tem: `id`, `descricao`, `valor` (double) e `tipo` (`"receita"` ou `"despesa"`).

### Guia passo a passo

**1. Preparar a lista e o contador**

- [ ] No `TransacaoController`, troque `List.of(...)` por `new ArrayList<>(List.of(...))`
- [ ] Adicione o campo `private int proximoId = 4;`
- [ ] Adicione o import de `java.util.ArrayList`

**2. Criar o DTO de request**

- [ ] Crie `TransacaoRequest.java` no pacote `br.edu.ifpr.casalapp.dto`
- [ ] Declare como `public record` com os campos: `String descricao`, `Double valor`, `String tipo` (sem `id`; use `Double` com D maiúsculo para que o campo possa ser `null` no PATCH)

**3. Implementar `POST /transacoes`**

- [ ] Crie o método `criarTransacao(@RequestBody TransacaoRequest request)` com retorno `ResponseEntity<TransacaoResponse>`
- [ ] Anote com `@PostMapping("/transacoes")`
- [ ] Crie o `TransacaoResponse` usando `proximoId` e os dados do request, incremente `proximoId` e adicione à lista
- [ ] Retorne `ResponseEntity.status(201).body(nova)`

**4. Implementar `PUT /transacoes/{id}`**

- [ ] Crie o método `atualizarTransacao(@PathVariable int id, @RequestBody TransacaoRequest request)` com retorno `ResponseEntity<TransacaoResponse>`
- [ ] Anote com `@PutMapping("/transacoes/{id}")`
- [ ] Use `for` com índice, encontre o id, crie novo objeto com todos os campos do request, use `set(i, ...)` e retorne `ResponseEntity.ok(atualizada)`
- [ ] Se não encontrar, retorne `ResponseEntity.notFound().build()`

**5. Implementar `PATCH /transacoes/{id}`**

- [ ] Crie o método `atualizarParcialTransacao(@PathVariable int id, @RequestBody TransacaoRequest request)` com retorno `ResponseEntity<TransacaoResponse>`
- [ ] Anote com `@PatchMapping("/transacoes/{id}")`
- [ ] Use `for` com índice e guarde o objeto existente em uma variável
- [ ] Para cada campo, use `campo != null ? campo : existente.campo()` para decidir o valor
- [ ] Crie novo objeto, use `set(i, ...)` e retorne `ResponseEntity.ok(atualizada)`

**6. Testar**

- [ ] `curl -X POST` com todos os campos cria uma transação e retorna status `201` com `id` no body
- [ ] `curl -X PUT` com todos os campos atualiza e retorna status `200`
- [ ] `curl -X PUT` sem um campo faz esse campo ficar `null` na resposta
- [ ] `curl -X PATCH` enviando só `descricao` altera apenas a descrição e preserva os demais campos
- [ ] Qualquer verbo com id inexistente retorna `404`
