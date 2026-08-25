# Aula 09: Bean Validation, documentação com Swagger e camadas do frontend

## _Dores_ ao construir uma API.

1. **Nada impedia um request malformado de chegar ao `service`.** Um `POST /casas` sem `nome`, ou um `POST /transacoes` com `valor` negativo, passava direto pelo `controller` e só ia dar problema (ou silenciosamente salvar um dado inválido) lá na frente.
2. **Descobrir o formato de cada endpoint exigia ler o código.** Sem um front fixo na tela, quem fosse integrar com a API precisava abrir cada `Controller` e cada `DTO` para saber que campos existiam, quais eram obrigatórios, o que cada rota devolvia.

Esta aula resolve os dois com duas ferramentas: **Bean Validation** (`jakarta.validation`) para o primeiro, **springdoc-openapi / Swagger** para o segundo. Fecha com uma explicação de como `casalapp-frontend/` foi organizado em camadas, espelhando a mesma separação `controller`/`service`/`model` que o backend já usa.

---

## 1. As anotações do `jakarta.validation`

`jakarta.validation` (Bean Validation) é, como a JPA, uma **especificação**: um conjunto de anotações que descrevem restrições sobre os campos de uma classe. Quem implementa e executa essas checagens é o **Hibernate Validator**, trazido pela dependência:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

No projeto, essas anotações foram nos **componentes do record** de cada DTO de request:

```java
public record CasaRequestDTO(
        @NotBlank(message = "nome é obrigatório") String nome) {
}
```

```java
public record CategoriaRequestDTO(
        @NotBlank(message = "nome é obrigatório") String nome,
        String icone,
        @NotNull(message = "casaId é obrigatório")
        @Schema(description = "id da casa à qual esta categoria pertence") Integer casaId) {
}
```

```java
public record TransacaoRequestDTO(
        @NotBlank(message = "descricao é obrigatória") String descricao,
        @NotNull(message = "valor é obrigatório")
        @Positive(message = "valor deve ser maior que zero") Double valor,
        @NotNull(message = "categoriaId é obrigatório")
        @Schema(description = "id da categoria existente à qual esta transação pertence") Integer categoriaId) {
}
```

| Anotação | O que exige |
|---|---|
| `@NotBlank` | String não pode ser `null`, nem vazia (`""`), nem só espaços em branco. Usada em `nome` e `descricao`. |
| `@NotNull` | Valor não pode ser `null` — mas aceita, por exemplo, `0` ou `""`. Usada em `valor` e nos ids de FK (`casaId`, `categoriaId`), onde `@NotBlank` não se aplica (não são `String`). |
| `@Positive` | Número precisa ser maior que zero. Usada em `valor`, para barrar uma transação de `R$ -50,00` ou `R$ 0,00`. |

Repare que `icone` (em `CategoriaRequestDTO`) não tem anotação nenhuma: é um campo opcional de verdade, então não faz sentido restringi-lo.

`Integer` em vez de `int` para `casaId`/`categoriaId` não é acidente: `int` primitivo nunca é `null` (o valor padrão seria `0`, um id válido em tese), então `@NotNull` só consegue barrar ausência de valor porque o tipo é o *wrapper* `Integer`, que aceita `null`.

---

## 2. `@Valid`: onde a validação é acionada

Anotar o DTO não basta — a anotação só descreve a regra, alguém precisa mandar o Hibernate Validator checá-la. Isso é feito com `@Valid` no parâmetro do `controller`:

```java
@PostMapping("/casas")
public ResponseEntity<CasaResponseDTO> criarCasa(@Valid @RequestBody CasaRequestDTO request) {
    CasaResponseDTO nova = casaService.criar(request);
    return ResponseEntity.status(201).body(nova);
}
```

Quando `@Valid` está presente, o Spring valida o `request` **antes** do corpo do método rodar. Se alguma restrição falhar, o método `criarCasa` nunca chega a ser executado — o Spring já responde com `400 Bad Request`:

```json
{"timestamp":"2026-08-21T16:42:31.408Z","status":400,"error":"Bad Request","path":"/casas"}
```

### 2.1 Por que `@Valid` está em POST e PUT, mas não no PATCH

Os três `Controller`s têm `@Valid` em `criar` (POST) e `atualizar` (PUT), mas **não** em `atualizarParcial` (PATCH):

```java
@PatchMapping("/categorias/{id}")
public ResponseEntity<CategoriaResponseDTO> atualizarParcialCategoria(
        @PathVariable int id,
        @RequestBody CategoriaRequestDTO request) {
    // sem @Valid
```

Isso não é esquecimento — é consequência de como `atualizarParcial`:

```java
public CategoriaResponseDTO atualizarParcial(int id, CategoriaRequestDTO request) {
    ...
    String novoNome = request.nome() != null ? request.nome() : existente.getNome();
    ...
}
```

O contrato do PATCH é justamente permitir que `nome` (ou `casaId`) venha `null`, significando "não mude este campo". Se `@Valid` fosse aplicado ali, um PATCH que só quer trocar o `icone` de uma categoria (deixando `nome` de fora do JSON) seria rejeitado com `400`, quebrando a própria funcionalidade que o método foi escrito para oferecer. `CasaRequestDTO`, `CategoriaRequestDTO` e `TransacaoRequestDTO` são reaproveitados nos três verbos (POST/PUT/PATCH) — a mesma anotação `@NotNull` significa coisas diferentes dependendo de quem está chamando, e é por isso que a validação automática só faz sentido ligada nos dois verbos onde o campo é, de fato, sempre obrigatório.

---

## 3. Documentando a API com Swagger / OpenAPI

**OpenAPI** é uma especificação (um formato de arquivo JSON/YAML) para descrever uma API REST: quais rotas existem, quais parâmetros e corpos cada uma aceita, o que cada uma devolve. **Swagger UI** é uma página HTML que lê esse arquivo e desenha uma interface navegável e testável a partir dele — parecido com o que o Postman faz manualmente, mas gerado automaticamente a partir do código.

A dependência usada é o **springdoc-openapi**, que faz a ponte entre Spring MVC e essa especificação:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
</dependency>
```

Sem nenhuma configuração adicional, essa dependência já expõe duas rotas:

| Rota | O que é |
|---|---|
| `/v3/api-docs` | O JSON da especificação OpenAPI, gerado lendo reflexivamente os `@RestController`, `@RequestMapping` e os records de DTO do projeto. |
| `/swagger-ui/index.html` | A interface visual, que consome esse JSON e mostra cada rota, agrupada por controller, com formulário pra testar. |

O springdoc já sabe, sozinho, que `POST /casas` recebe um `CasaRequestDTO` e devolve um `CasaResponseDTO` (lê isso da assinatura do método), e já marca `nome` como obrigatório na documentação **porque leu o `@NotBlank`** — a mesma anotação de validação serve, de graça, para documentação. É por isso que a aula não precisou anotar cada campo com um `@Schema` redundante (anotar `nome` como "o nome" não ensina nada a quem lê a doc); as únicas duas anotações `@Schema` do projeto ficam nos ids de FK, justamente os campos onde o nome sozinho (`casaId`) não deixa óbvio que ali se espera o id de uma casa **já existente**, e não um objeto novo:

```java
@Schema(description = "id da casa à qual esta categoria pertence") Integer casaId
```

Por fim, `@Operation(summary = "...")` nos métodos do `controller` documenta a **ação**, não o formato de dado — algo que nenhuma anotação de campo consegue expressar:

```java
@Operation(summary = "Cria uma categoria vinculada a uma casa existente")
@PostMapping("/categorias")
public ResponseEntity<CategoriaResponseDTO> criarCategoria(@Valid @RequestBody CategoriaRequestDTO request) {
```

Essa frase é o que aparece no Swagger UI ao lado da rota, e é onde cabe explicar uma regra de negócio (aqui, que a categoria depende de uma casa que precisa existir antes) que não está representada em nenhum tipo Java.

---

## 4. As camadas do `casalapp-frontend/`

O front foi separado em camadas seguindo o mesmo raciocínio de `controller`/`service`/`model` do backend: cada arquivo tem **uma** responsabilidade, e uma camada nunca faz o trabalho da outra.

```
casalapp-frontend/
  index.html, casas.html, categorias.html, transacoes.html
  css/
    styles.css
  js/
    config.js
    api/
      casaApi.js
      categoriaApi.js
      transacaoApi.js
    pages/
      casas.js
      categorias.js
      transacoes.js
```

| Camada | Responsabilidade | O que ela **não** faz |
|---|---|---|
| `*.html` | Estrutura da tela: formulário, tabela, elementos com `id` que o JavaScript vai procurar. Usa Bootstrap (via CDN) só para estilo, sem nenhuma lógica. | Não tem `<script>` inline nem lógica de negócio — só marcação e os `<script src="...">` que carregam as camadas JS, na ordem certa (`config.js` → `api/*` → `pages/*`). |
| `css/styles.css` | Pequenos ajustes visuais em cima do Bootstrap. | Não define nenhum comportamento. |
| `js/config.js` | Um único ponto com a URL base do backend (`API_BASE_URL`). | Trocar de ambiente (outra porta, outro host) significa mudar **uma linha**, em **um arquivo** — nenhuma outra camada guarda essa URL espalhada. |
| `js/api/*.js` | Equivalente ao `repository/` do backend: cada arquivo só sabe conversar HTTP com um recurso (`fetch`, `JSON.stringify`, `await resposta.json()`). Uma função por operação (`listarCasas`, `criarCasa`, `atualizarCasa`, `deletarCasa`). | Não toca em nenhum elemento do DOM, não sabe que existe uma tela ou um formulário. Se o back mudasse de REST para outro protocolo, só esta camada mudaria. |
| `js/pages/*.js` | Equivalente ao `controller` do backend: pega os elementos da tela (`document.getElementById`), reage a eventos (`submit`, `click`), chama a função certa da camada `api/`, e redesenha a tabela com o resultado. | Não monta a URL do backend, nem faz `fetch` diretamente — sempre delega para `api/*`. |

O paralelo direto com o backend:

| Backend | Frontend | Papel |
|---|---|---|
| `controller/` | `js/pages/*.js` | Recebe a interação (request HTTP / evento de DOM), decide o que chamar, devolve uma resposta (JSON / tela redesenhada). |
| `service/` + `repository/` | `js/api/*.js` | Sabe *como* buscar e persistir o dado — no backend, via `JpaRepository`; no front, via `fetch`. |
| `dto/` | O JSON trocado no `body` do `fetch` | O "contrato" de dado, sem lógica, que atravessa a borda entre as duas camadas. |

A mesma regra que já valia para `controller`/`service`/`model` vale aqui: **se uma função de `pages/` estiver fazendo `fetch` diretamente**, ou se uma função de `api/` estiver lendo `document.getElementById`, a camada errada está assumindo responsabilidade da outra — sinal de que a separação vazou.
