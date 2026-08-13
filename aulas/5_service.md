# Aula 5: Camada de service

## Revisão: Aula 4

Na aula anterior criamos o pacote `model/`, com classes tradicionais representando o "banco de dados" fake, e implementamos um INNER JOIN manual dentro do `TransacaoController`. Para isso, o `TransacaoController` precisou manter uma **cópia local** da lista de categorias, porque não existia um lugar único para acessar esse dado. Deixamos isso marcado como um problema a ser resolvido com uma camada de service.

Nesta aula:

1. Explicamos as camadas do projeto e o que cada uma pode enxergar da outra.
2. Apresentamos a anotação `@Service`.
3. Injetamos o `CategoriaService` no controller pela forma recomendada: parâmetro do construtor.
4. Explicamos o `@Autowired`: o que é, por que existe e por que não é a forma recomendada.
5. Apontamos os problemas concretos do projeto que motivam essa camada.
6. Implementamos o `CategoriaService`, movendo a lógica de acesso a dados **e** a conversão para DTO para fora do controller.

A `Transacao` **não** ganhou service nesta aula. Isso fica como exercício em [5_service_exercicio.md](5_service_exercicio.md).

---

## 1. Camadas do projeto: quem enxerga quem

O projeto tem quatro pacotes, cada um com uma responsabilidade e um conjunto de coisas que ele tem permissão de conhecer:

| Pacote | Responsabilidade | O que enxerga |
|---|---|---|
| `controller/` | Receber requisição HTTP, montar resposta com o status certo | `dto/` e `service/`. **Nunca** `model/`. |
| `service/` | Guardar os dados, aplicar regras (criar, buscar, atualizar, remover) e converter entre `model` e `dto` | `model/` e `dto/`. Pode depender de outro `service/`. |
| `model/` | Representar o dado armazenado (a "linha do banco fake") | Nada além de Java puro. |
| `dto/` | Formato de entrada e saída, o contrato entre o cliente e a API | Nada além de Java puro. |

A regra mais importante desta aula é a primeira linha: **o controller não conhece o `model`**. Ele só troca `dto` com o cliente (via `@RequestBody`/retorno do método) e só troca `dto` com o service (chama o service passando um `CategoriaRequest`, recebe de volta um `CategoriaResponse`). O `model.Categoria` fica inteiramente escondido dentro do `service/`, sem nenhum outro pacote saber que ele existe.

Isso é mais rígido do que fizemos até a aula 4, quando o `CategoriaController` guardava a lista de `Categoria` e fazia `toResponse(...)` ele mesmo. Nessa versão nova, é o `CategoriaService` quem faz essa conversão, porque ele é o único que enxerga o `model`.

### Por que essa regra existe

O `model` representa como o dado é guardado **internamente**. O `dto` representa como o dado é exposto **para fora**. Essas duas coisas podem, no futuro, divergir: um `CategoriaResponse` pode ganhar um campo calculado (`quantidadeTransacoes`) que não existe em `Categoria`, ou o `model` pode ganhar um atributo interno (uma data de criação, por exemplo) que a API nunca deveria expor. Se o controller conhece só o `dto`, ele nunca precisa mudar quando o `model` muda por dentro, e nunca corre o risco de devolver, sem querer, um campo interno que não deveria sair da aplicação.

Um jeito simples de lembrar a regra: **o controller fala a língua da API (`dto`); o service fala as duas línguas (`dto` e `model`) e faz a tradução entre elas.**

---

## 2. A anotação `@Service`

`@Service` marca uma classe como um **componente gerenciado pelo Spring**. Isso significa que o Spring cria uma única instância dessa classe (um *bean*) quando a aplicação sobe, e entrega essa instância para quem precisar dela.

```java
package br.edu.ifpr.casalapp.service;

import org.springframework.stereotype.Service;

@Service
public class CategoriaService {
    // ...
}
```

`@Service` é, na prática, equivalente a `@Component`, mas com um nome que comunica a intenção: "esta classe contém regra de negócio". O Spring não trata `@Service` de forma tecnicamente diferente de `@Component`, mas usar a anotação certa deixa o código mais fácil de entender. `@RestController`, que já usamos desde a aula 2, é outro exemplo do mesmo mecanismo: uma anotação de estereótipo que também registra a classe como bean.

---

## 3. Injeção de dependência por construtor

Se o `CategoriaService` é criado pelo Spring, o `CategoriaController` precisa de um jeito de obter essa instância, sem fazer `new CategoriaService()` na mão. A forma recomendada para isso é receber a dependência como parâmetro do construtor:

```java
@RestController
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // ...
}
```

O controller declara, no construtor, que precisa de um `CategoriaService`. Quando a classe tem **um único construtor**, o Spring já entende que deve injetar as dependências por ali: na hora de criar o `CategoriaController`, ele identifica que existe um único `CategoriaService` gerenciado (o `@Bean` criado a partir da classe anotada com `@Service`) e passa essa instância como argumento.

Esse mecanismo se chama **injeção de dependência**: o `CategoriaController` recebe o `CategoriaService` de fora, em vez de criar essa dependência dentro de si mesmo. A vantagem prática, no nosso projeto, é que existe **uma única instância** de `CategoriaService` compartilhada por toda a aplicação, com uma única lista de categorias na memória. Não importa quantas classes recebam `CategoriaService` pelo construtor, todas recebem a mesma instância, com os mesmos dados.

Duas vantagens concretas de receber a dependência pelo construtor:

- `categoriaService` pode ser `final`: o compilador garante que o atributo nunca fica sem valor depois que o objeto é criado, e ninguém troca essa referência por engano depois.
- Fica explícito, só de olhar a assinatura do construtor, de quais outras classes o `CategoriaController` depende. Um construtor com muitos parâmetros é um sinal visível de que a classe está acumulando responsabilidade demais.

---

## 4. `@Autowired`: o que é, por que existe e por que não é o recomendado

Uma forma alternativa e mais curta de escrever a mesma injeção é anotar o atributo diretamente com `@Autowired`, sem construtor nenhum:

```java
@RestController
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    // ...
}
```

Isso é chamado de **injeção por campo**. `@Autowired` diz ao Spring: "depois de criar este objeto, preencha este atributo com uma instância de `CategoriaService` que você já gerencia". É o mesmo resultado da seção anterior, com uma sintaxe mais enxuta.

### Por que essa forma existe

`@Autowired` no campo é mais rápido de escrever, principalmente quando uma classe depende de várias outras: não é preciso declarar construtor nem repetir os nomes dos parâmetros. Isso o torna comum em protótipos, exemplos curtos e códigos didáticos, e é por isso que aparece com tanta frequência em tutoriais e em projetos pequenos.

### Por que não é a forma recomendada

- **Não permite `final`.** O Spring precisa criar o objeto primeiro (com o construtor padrão, sem argumentos) para só depois preencher o campo por reflexão. Um atributo preenchido dessa forma não pode ser `final`, então nada impede que ele fique `null` por um tempo, ou seja reatribuído depois.
- **Esconde as dependências.** Olhando só a assinatura da classe (sem abrir o corpo e procurar por `@Autowired`), não dá para saber do que ela depende. Na injeção por construtor, isso está explícito na primeira linha.
- **Dificulta testes sem o Spring.** Para testar a classe isoladamente, sem subir o contexto do Spring, seria preciso instanciar o objeto e preencher o campo manualmente por reflexão. Com construtor, basta chamar `new CategoriaController(umCategoriaServiceDeTeste)`.
- **Facilita esconder dependência circular.** Duas classes que dependem uma da outra por campo só falham em tempo de execução, quando o Spring tenta montar o contexto. Com construtor, esse tipo de ciclo costuma ficar mais difícil de escrever sem perceber.

Ou seja, `@Autowired` no campo continua existindo, e o Spring continua dando suporte a ele, porque é conveniente e mais rápido de digitar, especialmente em código de estudo como o deste projeto. Mas para código de produção a injeção por construtor é preferida, justamente por resolver os pontos acima.

---

## 5. Problemas do projeto que motivam o service

### Problema 1: dado duplicado

Depois da aula 4, o projeto tinha duas listas de categorias, uma dentro de `CategoriaController` e outra, copiada manualmente, dentro de `TransacaoController`:

```java
// CategoriaController
private final List<Categoria> categorias = new ArrayList<>(List.of(
        new Categoria(1, "Alimentação", "prato"),
        ...
));

// TransacaoController
private final List<Categoria> categorias = new ArrayList<>(List.of(
        new Categoria(1, "Alimentação", "prato"),
        ...
));
```

Se um `POST /categorias` criasse uma categoria nova, ela apareceria em `GET /categorias`, mas **nunca** apareceria no join feito por `TransacaoController`, porque são listas diferentes, vivendo em objetos diferentes. Esse é exatamente o tipo de bug que a camada de service resolve: com um único `CategoriaService` compartilhado, os dois controllers enxergam a mesma lista, sempre atualizada.

### Problema 2: controller fazendo trabalho demais

O `CategoriaController`, antes desta aula, tinha loops de busca, criação e atualização misturados com a montagem de `ResponseEntity`, além de conhecer o `model.Categoria` para poder converter para `CategoriaResponse`. Isso mistura três preocupações diferentes no mesmo lugar: "como eu acesso o dado", "como eu converto o dado interno para o formato da API" e "que status HTTP eu devolvo".

### Como o service resolve os dois problemas

Movendo os dados, as regras **e a conversão para DTO** para `CategoriaService`, qualquer controller que precisar de categorias recebe `CategoriaService` no construtor e conversa com ele só em termos de `CategoriaRequest`/`CategoriaResponse`, sem duplicar `ArrayList` e sem nunca importar `model.Categoria`. O controller fica só com a tradução entre "resultado do service" e "resposta HTTP".

---

## 6. Implementando `CategoriaService`

### Estrutura de pacotes

```
br/edu/ifpr/casalapp/
├── CasalappApplication.java
├── controller/
│   ├── CategoriaController.java
│   └── TransacaoController.java
├── service/
│   └── CategoriaService.java
├── model/
│   ├── Categoria.java
│   └── Transacao.java
└── dto/
    ├── CategoriaRequest.java
    ├── CategoriaResponse.java
    ├── TransacaoRequest.java
    └── TransacaoResponse.java
```

### O service

A lista, o `proximoId` e o `toResponse(...)`, que antes viviam no controller, mudam para o service. Os métodos públicos do `CategoriaService` recebem e devolvem só `dto` (`CategoriaRequest`, `CategoriaResponse`): é assim que ele mantém o `model.Categoria` completamente escondido dos outros pacotes, seguindo a regra da seção 1. Internamente, o service continua usando `model.Categoria` para representar os dados guardados, e converte para `CategoriaResponse` só na hora de devolver algo para quem chamou.

Para representar "não encontrei", usamos o próprio `null`: se o `for` não encontra o id, o método devolve `null`, e quem chamou decide o que fazer com isso, do mesmo jeito que os controllers já faziam desde a aula 3:

```java
package br.edu.ifpr.casalapp.service;

import br.edu.ifpr.casalapp.dto.CategoriaRequest;
import br.edu.ifpr.casalapp.dto.CategoriaResponse;
import br.edu.ifpr.casalapp.model.Categoria;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoriaService {

    private int proximoId = 4;

    private final List<Categoria> categorias = new ArrayList<>(List.of(
            new Categoria(1, "Alimentação", "prato"),
            new Categoria(2, "Transporte", "carro"),
            new Categoria(3, "Saúde", "coração")
    ));

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getIcone());
    }

    public List<CategoriaResponse> listar() {
        List<CategoriaResponse> resultado = new ArrayList<>();

        for (Categoria categoria : categorias) {
            resultado.add(toResponse(categoria));
        }

        return resultado;
    }

    public CategoriaResponse buscarPorId(int id) {
        for (Categoria categoria : categorias) {
            if (categoria.getId() == id) {
                return toResponse(categoria);
            }
        }
        return null;
    }

    public CategoriaResponse criar(CategoriaRequest request) {
        Categoria nova = new Categoria(proximoId, request.nome(), request.icone());
        proximoId++;
        categorias.add(nova);
        return toResponse(nova);
    }

    public CategoriaResponse atualizar(int id, CategoriaRequest request) {
        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).getId() == id) {
                Categoria atualizada = new Categoria(id, request.nome(), request.icone());
                categorias.set(i, atualizada);
                return toResponse(atualizada);
            }
        }
        return null;
    }

    public CategoriaResponse atualizarParcial(int id, CategoriaRequest request) {
        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).getId() == id) {
                Categoria existente = categorias.get(i);

                String novoNome = request.nome() != null ? request.nome() : existente.getNome();
                String novoIcone = request.icone() != null ? request.icone() : existente.getIcone();

                Categoria atualizada = new Categoria(id, novoNome, novoIcone);
                categorias.set(i, atualizada);
                return toResponse(atualizada);
            }
        }
        return null;
    }

    public boolean deletar(int id) {
        for (Categoria categoria : categorias) {
            if (categoria.getId() == id) {
                categorias.remove(categoria);
                return true;
            }
        }
        return false;
    }
}
```

Repare que `toResponse(...)` agora é `private`: ninguém fora do `CategoriaService` precisa dele, porque ninguém fora do `CategoriaService` manipula `model.Categoria`. Os métodos públicos (`listar`, `buscarPorId`, `criar`, `atualizar`, `atualizarParcial`, `deletar`) só entram e saem com `dto`, e usam `null` como retorno quando o id não existe.

### O controller depois do service

Sem precisar mais converter `model` para `dto`, o `CategoriaController` fica reduzido à tradução entre "resultado do service" e "resposta HTTP". Ele nem importa mais `model.Categoria`:

```java
package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.CategoriaRequest;
import br.edu.ifpr.casalapp.dto.CategoriaResponse;
import br.edu.ifpr.casalapp.service.CategoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return categoriaService.listar();
    }

    @GetMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponse> buscarCategoria(@PathVariable int id) {
        CategoriaResponse categoria = categoriaService.buscarPorId(id);

        if (categoria != null) {
            return ResponseEntity.ok(categoria);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaResponse> criarCategoria(@RequestBody CategoriaRequest request) {
        CategoriaResponse nova = categoriaService.criar(request);
        return ResponseEntity.status(201).body(nova);
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponse> atualizarCategoria(
            @PathVariable int id,
            @RequestBody CategoriaRequest request) {

        CategoriaResponse atualizada = categoriaService.atualizar(id, request);

        if (atualizada != null) {
            return ResponseEntity.ok(atualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/categorias/{id}")
    public ResponseEntity<CategoriaResponse> atualizarParcialCategoria(
            @PathVariable int id,
            @RequestBody CategoriaRequest request) {

        CategoriaResponse atualizada = categoriaService.atualizarParcial(id, request);

        if (atualizada != null) {
            return ResponseEntity.ok(atualizada);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> deletarCategoria(@PathVariable int id) {
        if (categoriaService.deletar(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
```

**O que mudou de fato:**

- O controller não guarda mais `List<Categoria>`, `proximoId`, nem o método `toResponse(...)`. Tudo isso agora vive dentro do `CategoriaService`.
- O controller **não importa** `br.edu.ifpr.casalapp.model.Categoria` em lugar nenhum. Ele só conhece `dto` e `service`, exatamente como descrito na tabela da seção 1.
- `listarCategorias()` virou uma linha: o service já devolve `List<CategoriaResponse>` pronta para ser o corpo da resposta.
- Cada método do controller ficou reduzido a: chamar o service, checar se o retorno é `null` (quando existe essa checagem), montar o `ResponseEntity`.

**Testando (o comportamento externo não muda):**

```bash
curl -i http://localhost:8080/categorias
curl -i -X POST http://localhost:8080/categorias -H "Content-Type: application/json" -d '{"nome":"Lazer","icone":"estrela"}'
curl -i -X PATCH http://localhost:8080/categorias/1 -H "Content-Type: application/json" -d '{"nome":"Comida"}'
curl -i -X DELETE http://localhost:8080/categorias/4
curl -i http://localhost:8080/categorias/999
# HTTP/1.1 404 Not Found
```

A API se comporta exatamente como antes. A diferença é só interna: a lógica de dados e a conversão para DTO agora vivem isoladas em `CategoriaService`, e o `model.Categoria` deixou de ser visível fora dele.

---


## Exercício

O exercício desta aula (criar o `TransacaoService`) está em [5_service_exercicio.md](5_service_exercicio.md).
