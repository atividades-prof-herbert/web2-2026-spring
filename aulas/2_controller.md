# Aula 2 — Controllers no Spring Boot

## Revisão: Anotações da Aula 1

### `@SpringBootApplication`

Colocada na classe principal do projeto, é um atalho para três anotações combinadas:

| Anotação interna | O que faz |
|---|---|
| `@EnableAutoConfiguration` | Configura o Spring automaticamente com base nas dependências do `pom.xml`. Como `Spring Web` está presente, o Tomcat sobe automaticamente. |
| `@ComponentScan` | Varre o pacote atual e seus subpacotes procurando classes anotadas com `@Component`, `@Service`, `@Repository`, `@RestController`, etc., e as registra no contexto do Spring. |
| `@SpringBootConfiguration` | Marca a classe como fonte de configurações do Spring. |

```java
@SpringBootApplication
public class CasalappApplication {
    public static void main(String[] args) {
        SpringApplication.run(CasalappApplication.class, args);
    }
}
```

---

### `@RestController`

Colocada na classe do controller, é um atalho para duas anotações:

| Anotação interna | O que faz |
|---|---|
| `@Controller` | Registra a classe como componente Spring que trata requisições web. |
| `@ResponseBody` | O valor retornado pelo método vai direto no corpo da resposta HTTP (JSON ou texto), não como nome de página HTML. |

---

### Anotações de mapeamento de rota

Colocadas nos **métodos** do controller, associam um método a uma URL e a um verbo HTTP:

| Anotação | Verbo HTTP | Uso convencional |
|---|---|---|
| `@GetMapping("/rota")` | GET | Leitura de recursos |
| `@PostMapping("/rota")` | POST | Criação de recursos |
| `@PutMapping("/rota")` | PUT | Atualização completa |
| `@PatchMapping("/rota")` | PATCH | Atualização parcial |
| `@DeleteMapping("/rota")` | DELETE | Remoção de recursos |
| `@RequestMapping(value="/rota", method=...)` | Qualquer | Forma genérica, mais verbosa |

`@RequestMapping` pode ser usado também na **classe** para definir um prefixo que se aplica a todos os métodos:

```java
@RestController
@RequestMapping("/api/v1")
public class HelloController {

    @GetMapping("/hello")   // rota final: /api/v1/hello
    public String hello() {
        return "Hello, Spring Boot!";
    }
}
```

---

## 1. O que acontece quando uma requisição chega

Antes de escrever código, vale entender o caminho que uma requisição HTTP percorre dentro do Spring:

```
Cliente (navegador, curl, Postman)
    │
    │  GET /hello
    ▼
Tomcat (servidor embutido, porta 8080)
    │
    ▼
DispatcherServlet (componente central do Spring MVC)
    │  procura qual método está mapeado para GET /hello
    ▼
HelloController.hello()
    │  executa o método e pega o valor retornado
    ▼
HttpMessageConverter (converte String, objeto, List... para JSON ou texto)
    │
    ▼
Resposta HTTP para o cliente
```

O `DispatcherServlet` é o "porteiro" do Spring: toda requisição passa por ele antes de chegar ao seu código.

---

## 2. HelloController — retornando texto simples

O `HelloController` é o exemplo mais direto de um controller:

```java
package br.edu.ifpr.casalapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}
```

**O que acontece linha a linha:**

- `@RestController` — registra a classe no contexto do Spring e instrui que o retorno dos métodos vai direto no corpo da resposta HTTP.
- `@GetMapping("/hello")` — associa o método `hello()` ao verbo `GET` e ao caminho `/hello`.
- `return "Hello, Spring Boot!"` — o `HttpMessageConverter` serializa a `String` e a coloca no corpo da resposta com `Content-Type: text/plain`.

---

## 3. CategoriaController — retornando JSON

O `CategoriaController` mostra como retornar uma lista de objetos. O Spring converte automaticamente para JSON:

```java
package br.edu.ifpr.casalapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CategoriaController {

    record Categoria(int id, String nome, String icone) {}

    @GetMapping("/categorias")
    public List<Categoria> listarCategorias() {
        return List.of(
            new Categoria(1, "Alimentação", "prato"),
            new Categoria(2, "Transporte", "carro"),
            new Categoria(3, "Saúde", "coração")
        );
    }
}
```

**Pontos importantes:**

### `record Categoria`

`record` é um recurso do Java 16+ que cria uma classe de dados imutável com menos código:

```java
record Categoria(int id, String nome, String icone) {}
// equivale a uma classe com campos final, construtor, getters, equals e toString gerados
```

### Serialização automática para JSON

Quando o método retorna `List<Categoria>`, o Spring usa a biblioteca **Jackson** (incluída automaticamente com `Spring Web`) para converter a lista em JSON:

```json
[
  { "id": 1, "nome": "Alimentação", "icone": "prato" },
  { "id": 2, "nome": "Transporte",  "icone": "carro" },
  { "id": 3, "nome": "Saúde",       "icone": "coração" }
]
```

O `Content-Type` da resposta será `application/json` automaticamente.


---

## 4. Onde criar os controllers

Por convenção, controllers ficam em um subpacote `controller` dentro do pacote raiz da aplicação:

```
br/edu/ifpr/casalapp/
├── CasalappApplication.java
└── controller/
    ├── HelloController.java
    └── CategoriaController.java
```

Isso funciona porque `@SpringBootApplication` (na classe principal) ativa o `@ComponentScan`, que varre automaticamente todos os subpacotes — incluindo `controller/` — e registra as classes anotadas com `@RestController`.

---

## 5. DTO — Data Transfer Object


### O que é um DTO?

Um DTO é uma classe (ou record) criada **só para carregar dados entre camadas**. Neste caso, entre o controller e o cliente HTTP. Ele:

- Define exatamente quais campos aparecem no JSON de resposta
- Isola o formato da API da estrutura interna da aplicação
- Permite evolução independente: mudar a entidade não quebra a API, e mudar a API não afeta a entidade

Por convenção, DTOs de resposta levam o sufixo `Response`.

### Criando o `CategoriaResponse`

Crie o arquivo em um subpacote `dto`:

```java
package br.edu.ifpr.casalapp.dto;

public record CategoriaResponse(int id, String nome, String icone) {}
```

### Atualizando o controller para usar o DTO

```java
package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.CategoriaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CategoriaController {

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return List.of(
            new CategoriaResponse(1, "Alimentação", "prato"),
            new CategoriaResponse(2, "Transporte", "carro"),
            new CategoriaResponse(3, "Saúde", "coração")
        );
    }
}
```

O que mudou:
- O `record Categoria` saiu do controller e virou `CategoriaResponse` no pacote `dto`
- O tipo de retorno do método passou a ser `List<CategoriaResponse>`

### Estrutura de pacotes atualizada

```
br/edu/ifpr/casalapp/
├── CasalappApplication.java
├── controller/
│   ├── HelloController.java
│   └── CategoriaController.java
└── dto/
    └── CategoriaResponse.java
```

---

## 6. Parâmetros de path — `@PathVariable`

Até agora os endpoints recebem requisições sem nenhuma variação na URL. Mas é muito comum precisar identificar **um recurso específico** pelo seu id:

```
GET /categorias/2   →  retorna só o Transporte de id 2
DELETE /categorias/2   →  remove a categoria de id 2
```

A parte variável da URL (o `2`) é chamada de **path variable** (variável de caminho). No Spring, ela é declarada com chaves na anotação e capturada com `@PathVariable` no parâmetro do método:

```java
@GetMapping("/categorias/{id}")
public ResponseEntity<CategoriaResponse> buscarCategoria(@PathVariable int id) {
    ...
}
```

O nome dentro de `{}` deve ser igual ao nome do parâmetro. O Spring converte automaticamente o valor de `String` para o tipo do parâmetro (`int`, `long`, `String`, etc.).

---

## 7. `ResponseEntity`

Até agora os métodos retornavam o objeto diretamente e o Spring assumia status `200 OK`. Com `ResponseEntity<T>` você controla:

- o **status HTTP** da resposta (`200`, `404`, `204`, etc.)
- os **headers**
- o **body**

Isso é essencial quando a resposta pode variar: recurso encontrado → `200 OK` com body; não encontrado → `404 Not Found` sem body.

Antes de implementar, vale definir o que cada cenário deve retornar:

| Método | Cenário | Body | Status Code | `ResponseEntity` |
|---|---|---|---|---|
| `GET /categorias/{id}` | id encontrado | objeto JSON | `200 OK` | `ResponseEntity.ok(categoria)` |
| `GET /categorias/{id}` | id não existe | nenhum | `404 Not Found` | `ResponseEntity.notFound().build()` |
| `DELETE /categorias/{id}` | id encontrado | nenhum | `204 No Content` | `ResponseEntity.noContent().build()` |
| `DELETE /categorias/{id}` | id não existe | nenhum | `404 Not Found` | `ResponseEntity.notFound().build()` |

`ResponseEntity` usa o padrão **builder**: você encadeia chamadas que constroem a resposta passo a passo, e finaliza com `.build()` para criar o objeto.

### Métodos

`ResponseEntity.ok(corpo)` — cria uma resposta com status `200 OK` e o objeto como body. É o único que aceita o body direto, sem `.build()`.

```java
return ResponseEntity.ok(categoria);
// equivale a:
// return ResponseEntity.status(200).body(categoria);
```

`ResponseEntity.notFound()` — define o status como `404 Not Found`. Termina com `.build()` porque não há body.

```java
return ResponseEntity.notFound().build();
// equivale a:
// return ResponseEntity.status(404).build();
```

`ResponseEntity.noContent()` — define o status como `204 No Content`. Mesma lógica: sem body, `.build()` finaliza.

```java
return ResponseEntity.noContent().build();
// equivale a:
// return ResponseEntity.status(204).build();
```

**Por que `.build()` em vez de `.body(null)`?** `.build()` torna explícito que a ausência de body foi intencional. `.body(null)` seria ambíguo — difícil saber se é intenção ou esquecimento.

---

## 8. GET `/categorias/{id}` — buscando por id

```java
@GetMapping("/categorias/{id}")
public ResponseEntity<CategoriaResponse> buscarCategoria(@PathVariable int id) {
    for (CategoriaResponse categoria : categorias) {
        if (categoria.id() == id) {
            return ResponseEntity.ok(categoria);
        }
    }
    return ResponseEntity.notFound().build();
}
```

- O `for-each` percorre a lista um item por vez
- Se o `id` bate, retorna imediatamente `200 OK` com o objeto no body
- Se percorrer toda a lista sem achar, retorna `404 Not Found`

**Testando:**

```bash
curl -i http://localhost:8080/categorias/2
# HTTP/1.1 200 OK
# { "id": 2, "nome": "Transporte", "icone": "carro" }

curl -i http://localhost:8080/categorias/99
# HTTP/1.1 404 Not Found
```

---

## 9. DELETE `/categorias/{id}` — removendo por id

O `DELETE` segue a mesma lógica do path variable. A diferença está no verbo HTTP e no retorno:

- Recurso encontrado → `204 No Content` (sucesso, sem body)
- Recurso não encontrado → `404 Not Found`

`204 No Content` é a convenção REST para "operação realizada com sucesso, mas não há nada a retornar".

**`ResponseEntity<Void>`** — o `<Void>` formaliza que essa resposta nunca terá body. Quem lê a assinatura do método já sabe que não há dado de retorno.

```java
@DeleteMapping("/categorias/{id}")
public ResponseEntity<Void> deletarCategoria(@PathVariable int id) {
    for (CategoriaResponse categoria : categorias) {
        if (categoria.id() == id) {
            return ResponseEntity.noContent().build();
        }
    }
    return ResponseEntity.notFound().build();
}
```

**Testando:**

```bash
curl -i -X DELETE http://localhost:8080/categorias/1
# HTTP/1.1 204 No Content

curl -i -X DELETE http://localhost:8080/categorias/99
# HTTP/1.1 404 Not Found
```

---

## Resumo

| Elemento | Onde | Para que serve |
|---|---|---|
| `@RestController` | classe | registra o controller e habilita serialização automática no body |
| `@GetMapping` | método | mapeia o método para `GET /rota` |
| `@DeleteMapping` | método | mapeia o método para `DELETE /rota` |
| `@PathVariable` | parâmetro | extrai um segmento variável da URL e injeta no método |
| `ResponseEntity<T>` | tipo de retorno | controla status HTTP, headers e body da resposta |
| `ResponseEntity.ok(body)` | corpo do método | retorna `200 OK` com o objeto no body |
| `ResponseEntity.notFound().build()` | corpo do método | retorna `404 Not Found` sem body |
| `ResponseEntity.noContent().build()` | corpo do método | retorna `204 No Content` sem body |
| `record` | pacote `dto` | define um modelo de dados compacto e imutável |
| Jackson | automático | converte objetos Java em JSON na resposta |

---

## Exercício

Crie um endpoint para **transações financeiras**, seguindo o mesmo padrão do `CategoriaController`.

Uma transação tem: `id`, `descricao`, `valor` (double) e `tipo` (`"receita"` ou `"despesa"`).

### Guia passo a passo

**1. Criar o DTO**

- [ ] Crie o arquivo `TransacaoResponse.java` dentro do pacote `br.edu.ifpr.casalapp.dto`
- [ ] Declare-o como `public record` com os campos: `int id`, `String descricao`, `double valor`, `String tipo`

**2. Criar o Controller**

- [ ] Crie o arquivo `TransacaoController.java` dentro do pacote `br.edu.ifpr.casalapp.controller`
- [ ] Anote a classe com `@RestController`
- [ ] Declare um campo `private final List<TransacaoResponse> transacoes` com ao menos 3 transações fixas usando `List.of(...)`

**3. Implementar `GET /transacoes`**

- [ ] Crie o método `listarTransacoes()` com retorno `List<TransacaoResponse>`
- [ ] Anote com `@GetMapping("/transacoes")`
- [ ] Retorne a lista de transações

**4. Implementar `GET /transacoes/{id}`**

- [ ] Crie o método `buscarTransacao(@PathVariable int id)` com retorno `ResponseEntity<TransacaoResponse>`
- [ ] Anote com `@GetMapping("/transacoes/{id}")`
- [ ] Use `for-each` para percorrer a lista e retornar `ResponseEntity.ok(transacao)` se encontrar
- [ ] Retorne `ResponseEntity.notFound().build()` caso não encontre

**5. Implementar `DELETE /transacoes/{id}`**

- [ ] Crie o método `deletarTransacao(@PathVariable int id)` com retorno `ResponseEntity<Void>`
- [ ] Anote com `@DeleteMapping("/transacoes/{id}")`
- [ ] Use `for-each` para verificar se o id existe e retorne `ResponseEntity.noContent().build()` se encontrar
- [ ] Retorne `ResponseEntity.notFound().build()` caso não encontre

**6. Testar**

- [ ] Suba a aplicação com `./mvnw spring-boot:run`
- [ ] `curl http://localhost:8080/transacoes` retorna a lista em JSON
- [ ] `curl http://localhost:8080/transacoes/1` retorna a primeira transação com status `200`
- [ ] `curl http://localhost:8080/transacoes/99` retorna status `404`
- [ ] `curl -X DELETE http://localhost:8080/transacoes/1` retorna status `204`
- [ ] `curl -X DELETE http://localhost:8080/transacoes/99` retorna status `404`