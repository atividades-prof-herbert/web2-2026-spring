# Aula 4: Relacionamento entre recursos, model/ e INNER JOIN com loops

## Revisão: Aula 3

Na aula anterior implementamos POST, PUT e PATCH usando DTOs de request e response, com um ArrayList como banco de dados fake. Até aqui, cada controller tinha apenas um recurso isolado (Categoria), sem relação com outro.

Nesta aula:

1. Criamos o `TransacaoController`, cujo recurso se relaciona com Categoria através de um `categoriaId`.
2. Introduzimos o pacote `model/`, separando o "objeto de banco de dados" do DTO de resposta.
3. Fizemos um INNER JOIN manual, com loops básicos, para que o `GET /transacoes` devolva o nome da categoria junto com a transação.
4. Refatoramos o `CategoriaController` para seguir o mesmo padrão.

---

## 1. Por que separar model/ de dto/?

Até a aula 3, `CategoriaResponse` era usado tanto para armazenar o dado na lista (banco fake) quanto para devolver a resposta ao cliente. Isso funciona enquanto o recurso é simples, mas cria um problema quando existe relacionamento: a "linha do banco" (`categoriaId`) é diferente do que o cliente quer ver na resposta (`categoriaNome`).

A solução é separar as responsabilidades em três tipos de classe:

| Pacote | Papel | Contém id? | Sabe sobre outros recursos? |
|---|---|---|---|
| `model/` | representa a linha do banco fake (o que é armazenado) | sim | não, só guarda o `categoriaId`, um número |
| `dto/...Request` | o que o cliente envia | não | não |
| `dto/...Response` | o que o servidor devolve | sim | pode incluir dados combinados de outros recursos (ex: `categoriaNome`) |

### Estrutura de pacotes

```
br/edu/ifpr/casalapp/
├── CasalappApplication.java
├── controller/
│   ├── CategoriaController.java
│   └── TransacaoController.java
├── model/
│   ├── Categoria.java
│   └── Transacao.java
└── dto/
    ├── CategoriaRequest.java
    ├── CategoriaResponse.java
    ├── TransacaoRequest.java
    └── TransacaoResponse.java
```

---

## 2. model/Categoria.java e model/Transacao.java

As classes de `model/` representam exatamente o que fica guardado no ArrayList (o "banco de dados"). Diferente dos DTOs, que usamos como `record`, aqui usamos o conceito tradicional de classe: atributos privados, construtor e métodos `get`:

```java
package br.edu.ifpr.casalapp.model;

public class Categoria {

    private int id;
    private String nome;
    private String icone;

    public Categoria(int id, String nome, String icone) {
        this.id = id;
        this.nome = nome;
        this.icone = icone;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getIcone() {
        return icone;
    }
}
```

```java
package br.edu.ifpr.casalapp.model;

public class Transacao {

    private int id;
    private String descricao;
    private Double valor;
    private int categoriaId;

    public Transacao(int id, String descricao, Double valor, int categoriaId) {
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.categoriaId = categoriaId;
    }

    public int getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public Double getValor() {
        return valor;
    }

    public int getCategoriaId() {
        return categoriaId;
    }
}
```

Repare que `Transacao` não guarda o nome da categoria, apenas o `categoriaId`. É assim que uma relação funciona: guarda se a chave (o id), não o dado do outro recurso.

### Por que classe tradicional em model/ e record em dto/?

Os DTOs (`CategoriaRequest`, `CategoriaResponse`, `TransacaoRequest`, `TransacaoResponse`) continuam sendo `record`, porque são objetos imutáveis que só carregam dados de um lado para o outro (cliente para servidor, servidor para cliente). Já as classes de `model/` representam o "banco de dados" fake, e é comum, em um projeto real, que essas classes precisem de mais comportamento no futuro (validações, métodos auxiliares, relacionamento com outras entidades). Por isso usamos aqui o formato de classe tradicional, com atributos privados e métodos `get`, no lugar do `record`.

---

## 3. TransacaoRequest e TransacaoResponse

```java
package br.edu.ifpr.casalapp.dto;

public record TransacaoRequest(String descricao, Double valor, Integer categoriaId) {}
```

```java
package br.edu.ifpr.casalapp.dto;

public record TransacaoResponse(int id, String descricao, Double valor, int categoriaId, String categoriaNome) {}
```

- `TransacaoRequest.categoriaId` é `Integer` (não `int`), pelo mesmo motivo de `Double valor`: precisa poder ser `null` no PATCH, para indicar "campo não enviado".
- `TransacaoResponse` tem um campo a mais que o `model.Transacao`: `categoriaNome`. Esse campo não existe no banco fake, ele é calculado na hora da resposta, através do join.

---

## 4. O relacionamento: categoriaId

O `TransacaoController` guarda apenas o id da categoria, nunca o objeto inteiro:

```java
private final List<Transacao> transacoes = new ArrayList<>(List.of(
        new Transacao(1, "Almoço", 35.90, 1),
        new Transacao(2, "Corrida de aplicativo", 18.50, 2),
        new Transacao(3, "Consulta médica", 150.00, 3)
));
```

O `1` no final de cada linha é o `categoriaId`, a "chave estrangeira" que aponta para o id de uma categoria em `CategoriaController`.

---

## 5. INNER JOIN com loops básicos

### O problema

O cliente quer ver, no `GET /transacoes`, o nome da categoria ("Alimentação") e não só o número (1). Isso exige combinar dois vetores: `transacoes` e `categorias`.

### Um vetor de categorias dentro do TransacaoController

Por enquanto, cada controller mantém o seu próprio "banco fake" isolado. Para fazer o join, o `TransacaoController` mantém uma **cópia** dos dados de categorias, no mesmo formato de `CategoriaController`:

```java
private final List<Categoria> categorias = new ArrayList<>(List.of(
        new Categoria(1, "Alimentação", "prato"),
        new Categoria(2, "Transporte", "carro"),
        new Categoria(3, "Saúde", "coração")
));
```

Note que os dados estão duplicados: os mesmos três registros existem tanto em `CategoriaController` quanto em `TransacaoController`. Isso não é ideal (se uma categoria mudar de nome em um controller, o outro fica desatualizado), mas é aceitável por enquanto porque ainda não temos um lugar único para guardar esse dado e compartilhar entre controllers. Em uma aula futura, vamos criar uma camada de `service/`, responsável por centralizar o acesso aos dados, e essa duplicação vai deixar de existir.

### O join

Para cada transação, percorremos o vetor de categorias procurando o id correspondente. É exatamente o que um `INNER JOIN ON transacao.categoria_id = categoria.id` faria em SQL, só que com dois loops em vez de uma cláusula SQL:

```java
private TransacaoResponse toResponse(Transacao transacao) {
    String categoriaNome = null;
    for (Categoria categoria : categorias) {
        if (categoria.getId() == transacao.getCategoriaId()) {
            categoriaNome = categoria.getNome();
            break;
        }
    }

    return new TransacaoResponse(
            transacao.getId(),
            transacao.getDescricao(),
            transacao.getValor(),
            transacao.getCategoriaId(),
            categoriaNome
    );
}
```

`listarTransacoes()` é o outro lado do join: o loop externo, percorrendo cada transação e chamando `toResponse` (que faz o loop interno, em `categorias`):

```java
@GetMapping("/transacoes")
public List<TransacaoResponse> listarTransacoes() {
    List<TransacaoResponse> resultado = new ArrayList<>();

    for (Transacao transacao : transacoes) {       // loop externo: transações
        resultado.add(toResponse(transacao));       // loop interno: categorias (dentro de toResponse)
    }

    return resultado;
}
```

Dois loops aninhados (um dentro do outro, via `toResponse`) são a forma mais simples de implementar um INNER JOIN sem SQL: para cada linha de um vetor, procura a linha correspondente no outro vetor.

**O que acontece se o categoriaId não existir em nenhuma categoria?**

O loop termina sem encontrar nada, `categoriaNome` permanece `null` e a resposta é devolvida assim mesmo, sem quebrar. Isso é diferente de um INNER JOIN real em SQL, que simplesmente omitiria a linha sem correspondência (só um LEFT JOIN manteria a linha com null). Aqui optamos por sempre devolver a transação, com o nome ausente.

**Testando:**

```bash
curl -s http://localhost:8080/transacoes | python3 -m json.tool
```

```json
[
    {
        "id": 1,
        "descricao": "Almoço",
        "valor": 35.9,
        "categoriaId": 1,
        "categoriaNome": "Alimentação"
    },
    {
        "id": 2,
        "descricao": "Corrida de aplicativo",
        "valor": 18.5,
        "categoriaId": 2,
        "categoriaNome": "Transporte"
    }
]
```

---

## 6. TransacaoController completo (CRUD)

O CRUD segue exatamente o mesmo padrão da aula 3, mudando apenas o tipo interno (`Transacao`, não `TransacaoResponse`) e chamando `toResponse(...)` antes de devolver qualquer resposta:

```java
@GetMapping("/transacoes/{id}")
public ResponseEntity<TransacaoResponse> buscarTransacao(@PathVariable int id) {
    for (Transacao transacao : transacoes) {
        if (transacao.getId() == id) {
            return ResponseEntity.ok(toResponse(transacao));
        }
    }
    return ResponseEntity.notFound().build();
}

@PostMapping("/transacoes")
public ResponseEntity<TransacaoResponse> criarTransacao(@RequestBody TransacaoRequest request) {
    Transacao nova = new Transacao(proximoId, request.descricao(), request.valor(), request.categoriaId());
    proximoId++;
    transacoes.add(nova);
    return ResponseEntity.status(201).body(toResponse(nova));
}
```

PUT, PATCH e DELETE seguem o mesmo formato de índice (`for` com `i`) usado na aula 3, apenas trocando `CategoriaResponse`/`categorias` por `Transacao`/`transacoes`.

---

## 7. Refatorando CategoriaController para usar model/

Para manter os dois controllers consistentes, o `CategoriaController` também passou a guardar `Categoria` (model) internamente, convertendo para `CategoriaResponse` apenas na hora de responder:

```java
private final List<Categoria> categorias = new ArrayList<>(List.of(
        new Categoria(1, "Alimentação", "prato"),
        new Categoria(2, "Transporte", "carro"),
        new Categoria(3, "Saúde", "coração")
));

private CategoriaResponse toResponse(Categoria categoria) {
    return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getIcone());
}

@GetMapping("/categorias")
public List<CategoriaResponse> listarCategorias() {
    List<CategoriaResponse> resultado = new ArrayList<>();

    for (Categoria categoria : categorias) {
        resultado.add(toResponse(categoria));
    }

    return resultado;
}
```

**Por que isso importa mesmo sem Categoria ter nenhum relacionamento?**

Antes da refatoração, `categorias` era `List<CategoriaResponse>`: o "banco" e a "resposta" eram literalmente o mesmo objeto. Isso funciona só enquanto os dois nunca precisam divergir. Ao trocar para `List<Categoria>` (model) mais `toResponse(...)`, o controller fica preparado para o dia em que o banco fake e a resposta precisarem ter formatos diferentes, por exemplo se `CategoriaResponse` ganhar um campo calculado, como `quantidadeTransacoes`.

Os demais métodos (`buscarCategoria`, `criarCategoria`, `atualizarCategoria`, `atualizarParcialCategoria`, `deletarCategoria`) seguem o mesmo raciocínio: operam sobre `List<Categoria>` e usam `toResponse(...)` só na hora de montar o `ResponseEntity`.

---

## Resumo

| Elemento | Para que serve |
|---|---|
| `model/Categoria`, `model/Transacao` | Representam a linha do banco de dados fake, o que é armazenado. Classes tradicionais, com atributos privados e métodos `get`, não `record`. |
| `dto/...Request` | O que o cliente envia (sem id, sem dados de outros recursos). |
| `dto/...Response` | O que o servidor devolve (com id, podendo incluir dados combinados de outros recursos). |
| `categoriaId` | A "chave estrangeira" dentro de `model.Transacao`, só o número, nunca o objeto Categoria inteiro. |
| `toResponse(model)` | Converte o objeto do banco fake (model) no DTO de resposta, aplicando o join quando necessário. |
| cópia local de `categorias` em `TransacaoController` | Solução temporária para o join, enquanto não existe uma camada de service compartilhada. |
| loop externo mais loop interno | Implementação manual de um INNER JOIN: para cada linha de um vetor, procura a correspondente no outro. |
| `categoriaNome == null` | Quando o `categoriaId` não corresponde a nenhuma categoria, a transação ainda é devolvida, só sem o nome. |
