# Aula 3 — POST, PUT e PATCH com DTO no Spring Boot

## Revisão: Aula 2

Na aula anterior construímos endpoints `GET` e `DELETE` com `@PathVariable` e `ResponseEntity`. O "banco de dados" era uma `List.of(...)`. Para receber dados e persistir na memória, precisamos de uma lista mutável e de endpoints que leiam o corpo da requisição.

---

## 1. ArrayList como banco de dados fake

`List.of(...)` cria uma lista imutável. Para simular um banco de dados, trocamos por `new ArrayList<>()`:

```java
private int proximoId = 4;

private final List<CategoriaResponse> categorias = new ArrayList<>(List.of(
    new CategoriaResponse(1, "Alimentação", "prato"),
    new CategoriaResponse(2, "Transporte", "carro"),
    new CategoriaResponse(3, "Saúde", "coração")
));
```

- `new ArrayList<>(List.of(...))` cria uma lista mutável já com os itens iniciais.
- `categorias` é `final`, portanto a referência não muda, mas o conteúdo pode.
- `proximoId` controla qual id será usado na próxima inserção. Começa em `4` pois os dados iniciais já ocupam `1`, `2` e `3`.

---

## 2. DTO de Request — recebendo dados do cliente

Até agora usávamos apenas DTOs de resposta (sufixo `Response`). Para POST, PUT e PATCH, o cliente envia dados no corpo da requisição. Esses dados precisam de um DTO de entrada, por convenção com sufixo `Request`.

### Por que separar Request de Response?

| Aspecto | `CategoriaRequest` | `CategoriaResponse` |
|---|---|---|
| Direção | cliente para servidor | servidor para cliente |
| Contém `id`? | Não. Quem gera o id é o servidor. | Sim |

### Criando o `CategoriaRequest`

```java
package br.edu.ifpr.casalapp.dto;

public record CategoriaRequest(String nome, String icone) {}
```

`id` não está presente porque o servidor é responsável por gerar o identificador.

### Estrutura de pacotes

```
br/edu/ifpr/casalapp/
├── CasalappApplication.java
├── controller/
│   └── CategoriaController.java
└── dto/
    ├── CategoriaRequest.java
    └── CategoriaResponse.java
```

---

## 3. `@RequestBody`

`@RequestBody` instrui o Spring a ler o corpo da requisição HTTP e desserializar o JSON para um objeto Java:

```java
@PostMapping("/categorias")
public ResponseEntity<CategoriaResponse> criarCategoria(@RequestBody CategoriaRequest request) {
    ...
}
```

O Jackson (biblioteca incluída com `Spring Web`) faz a conversão automaticamente. O nome dos campos no JSON deve ser igual ao nome dos campos do record.

**Exemplo de corpo da requisição:**

```json
{ "nome": "Lazer", "icone": "estrela" }
```

---

## 4. POST `/categorias` — criando um recurso

### O que precisa acontecer?

Ao receber uma requisição `POST`, o servidor precisa:

1. **Receber o recurso:** o cliente envia os dados no corpo da requisição (`CategoriaRequest`).
2. **Inserir no banco de dados:** adicionar o novo objeto na lista (`ArrayList`).
3. **Retornar confirmação:** devolver o objeto criado, agora com o `id` gerado pelo servidor.

### Status de retorno

A convenção REST para criação bem-sucedida é `201 Created`. Diferente do `200 OK` (que indica que uma leitura ou atualização deu certo), o `201` comunica ao cliente que um **novo recurso foi criado**.

O body da resposta inclui o `CategoriaResponse` com o `id` gerado. Isso é importante porque o cliente não sabia o id antes de fazer a requisição.

| Cenário | Body | Status |
|---|---|---|
| criação bem-sucedida | objeto criado (com `id`) | `201 Created` |

### Implementação

```java
@PostMapping("/categorias")
public ResponseEntity<CategoriaResponse> criarCategoria(@RequestBody CategoriaRequest request) {
    CategoriaResponse nova = new CategoriaResponse(proximoId, request.nome(), request.icone());
    proximoId++;
    categorias.add(nova);
    return ResponseEntity.status(201).body(nova);
}
```

**O que acontece passo a passo:**

1. `new CategoriaResponse(proximoId, request.nome(), request.icone())` monta o objeto completo usando o id atual e os dados que vieram do cliente.
2. `proximoId++` incrementa o contador para a próxima inserção.
3. `categorias.add(nova)` insere o objeto no banco de dados fake.
4. `ResponseEntity.status(201).body(nova)` retorna `201 Created` com o objeto no body, incluindo o `id` gerado.

**Testando:**

```bash
curl -i -X POST http://localhost:8080/categorias \
     -H "Content-Type: application/json" \
     -d '{"nome": "Lazer", "icone": "estrela"}'
# HTTP/1.1 201 Created
# { "id": 4, "nome": "Lazer", "icone": "estrela" }
```

---

## 5. PUT `/categorias/{id}` — atualizando um recurso por completo

### O que precisa acontecer?

O `PUT` substitui **todos os campos** de um recurso existente. A semântica do `PUT` é: "substitua o recurso inteiro pelo que estou enviando agora". Por isso o cliente deve enviar **todos os campos** no body. Os campos não enviados serão perdidos.

Para isso, o servidor precisa de **duas informações**:

- **Qual objeto modificar:** vem pelo `id` na URL (`/categorias/2`). Sem o id na URL não haveria como saber se a intenção é atualizar Transporte, Saúde ou qualquer outro.
- **O que colocar no lugar:** vem no corpo da requisição (`@RequestBody`), com os novos valores dos campos.

Essa separação é intencional: a URL identifica o recurso, o body carrega os dados novos.

1. **Buscar no banco de dados pelo id:** percorrer a lista até encontrar o item cujo `id` bate com o `id` da URL.
2. **Atualizar todos os campos:** substituir o objeto inteiro pelo novo. Todos os campos vêm do request.
3. **Retornar o objeto atualizado:** devolver o `CategoriaResponse` com os dados novos.

| Cenário | Body | Status |
|---|---|---|
| id encontrado | objeto atualizado | `200 OK` |
| id não existe | nenhum | `404 Not Found` |

### Implementação

Como `record` é imutável, não é possível alterar os campos de um objeto existente. A solução é criar um novo objeto com os dados atualizados e **substituir** o item na lista pelo índice:

```java
@PutMapping("/categorias/{id}")
public ResponseEntity<CategoriaResponse> atualizarCategoria(
        @PathVariable int id,
        @RequestBody CategoriaRequest request) {

    for (int i = 0; i < categorias.size(); i++) {
        if (categorias.get(i).id() == id) {
            CategoriaResponse atualizada = new CategoriaResponse(id, request.nome(), request.icone());
            categorias.set(i, atualizada);
            return ResponseEntity.ok(atualizada);
        }
    }
    return ResponseEntity.notFound().build();
}
```

**O que acontece passo a passo:**

1. `for (int i = 0; i < categorias.size(); i++)` percorre a lista com índice, necessário para usar o `set` depois.
2. `categorias.get(i).id() == id` compara o id do item atual com o id que veio na URL.
3. `new CategoriaResponse(id, request.nome(), request.icone())` monta o objeto com todos os campos vindos do request. O `id` é mantido pois veio da URL.
4. `categorias.set(i, atualizada)` substitui o item na posição `i` no banco de dados fake.
5. `ResponseEntity.ok(atualizada)` retorna `200 OK` com o objeto atualizado.
6. Se o `for` terminar sem encontrar o id, retorna `404 Not Found`.

**Por que `for` com índice em vez de `for-each`?**

O `for-each` não expõe a posição do elemento na lista. `categorias.set(i, novo)` exige o índice, por isso usamos o `for` clássico aqui.

**Testando:**

```bash
curl -i -X PUT http://localhost:8080/categorias/2 \
     -H "Content-Type: application/json" \
     -d '{"nome": "Mobilidade", "icone": "bicicleta"}'
# HTTP/1.1 200 OK
# { "id": 2, "nome": "Mobilidade", "icone": "bicicleta" }

curl -i -X PUT http://localhost:8080/categorias/99 \
     -H "Content-Type: application/json" \
     -d '{"nome": "Teste", "icone": "icone"}'
# HTTP/1.1 404 Not Found
```

---

## 6. PATCH `/categorias/{id}` — atualizando campos específicos

### O id continua vindo pela URL

Assim como no `PUT`, o `PATCH` também recebe o `id` pela URL. O raciocínio é o mesmo: o cliente precisa informar **qual** objeto quer modificar antes de dizer **o quê** quer mudar. A URL identifica o recurso; o body traz apenas os campos que devem ser alterados.

### PUT vs PATCH

| | PUT | PATCH |
|---|---|---|
| O que atualiza? | **todos** os campos | apenas os campos enviados |
| Campo não enviado | fica `null` e é apagado | mantém o valor atual |
| Uso | substituição completa | atualização parcial |

Com `PUT`, se o cliente esquecer de enviar o campo `icone`, ele vai a `null`. Com `PATCH`, campos não enviados são ignorados e o valor anterior é preservado.

### Como identificar que um campo não foi enviado?

Quando o Jackson desserializa um JSON que não contém um determinado campo, ele atribui `null` ao campo correspondente no record. A lógica é simples: se o campo veio como `null`, mantemos o valor atual; se veio com valor, substituímos.

### Implementação

```java
@PatchMapping("/categorias/{id}")
public ResponseEntity<CategoriaResponse> atualizarParcialCategoria(
        @PathVariable int id,
        @RequestBody CategoriaRequest request) {

    for (int i = 0; i < categorias.size(); i++) {
        if (categorias.get(i).id() == id) {
            CategoriaResponse existente = categorias.get(i);

            String novoNome = request.nome() != null ? request.nome() : existente.nome();
            String novoIcone = request.icone() != null ? request.icone() : existente.icone();

            CategoriaResponse atualizada = new CategoriaResponse(id, novoNome, novoIcone);
            categorias.set(i, atualizada);
            return ResponseEntity.ok(atualizada);
        }
    }
    return ResponseEntity.notFound().build();
}
```

**O que muda em relação ao PUT:**

1. `CategoriaResponse existente = categorias.get(i)` guarda o objeto atual antes de substituir.
2. `request.nome() != null ? request.nome() : existente.nome()` verifica campo a campo: se o campo veio no request, usa o novo valor; se veio como `null`, mantém o valor atual.
3. O restante é igual ao `PUT`: cria novo objeto, substitui na lista e retorna `200 OK`.

**Testando — atualizando só o nome:**

```bash
curl -i -X PATCH http://localhost:8080/categorias/2 \
     -H "Content-Type: application/json" \
     -d '{"nome": "Mobilidade"}'
# HTTP/1.1 200 OK
# { "id": 2, "nome": "Mobilidade", "icone": "carro" }
# o icone foi preservado
```

**Comparando com PUT no mesmo cenário:**

```bash
curl -i -X PUT http://localhost:8080/categorias/2 \
     -H "Content-Type: application/json" \
     -d '{"nome": "Mobilidade"}'
# HTTP/1.1 200 OK
# { "id": 2, "nome": "Mobilidade", "icone": null }
# o icone foi apagado porque não foi enviado
```

---

## Resumo

| Elemento | Para que serve |
|---|---|
| `@PostMapping` | Mapeia o método para `POST /rota`. Usado para criação de recurso. |
| `@PutMapping` | Mapeia o método para `PUT /rota`. Substitui o recurso por completo. |
| `@PatchMapping` | Mapeia o método para `PATCH /rota`. Atualiza apenas os campos enviados. |
| `@RequestBody` | Desserializa o JSON do corpo da requisição para um objeto Java. |
| `CategoriaRequest` | DTO de entrada com os campos que o cliente envia (sem `id`). |
| `CategoriaResponse` | DTO de saída com os campos que o servidor devolve (com `id`). |
| `proximoId` | Contador para gerar ids sequenciais. |
| `categorias.add(nova)` | Insere um item no banco de dados fake. |
| `categorias.set(i, novo)` | Substitui o item na posição `i`. |
| `ResponseEntity.status(201).body(nova)` | Retorna `201 Created` com o objeto no body. |
| `campo != null ? campo : existente.campo()` | Lógica do PATCH: usa o novo valor se enviado, ou mantém o atual. |

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
