# Aula 6: ORM puro (sem anotações do Spring Data)

## Revisão: Aula 5

Na aula anterior criamos a camada `service/`, movendo para lá o acesso aos dados e a conversão `model` → `dto`. Vimos também como o `TransacaoService` resolve seu relacionamento com `Categoria`: o `model.Transacao` guarda só um `int categoriaId`, e é o `TransacaoService` quem, na hora de montar o `TransacaoResponseDTO`, pega esse id e pergunta ao `CategoriaService` qual é o nome correspondente. Isso é, na prática, um **JOIN manual**, escrito à mão em Java.

Nesta aula:

1. Explicamos o que é um ORM (*Object-Relational Mapping*) e por que ele existe.
2. Comparamos as duas formas de representar um relacionamento em Java: **por id** (o que fizemos até aqui) e **por referência de objeto** (o que um ORM faz).
3. Implementamos, sem nenhuma anotação do Spring Data, um relacionamento **1 para N**, incluindo um DTO composto que devolve a subcoleção relacionada.
4. Implementamos, sem nenhuma anotação do Spring Data, um relacionamento **N para M**.


---

## 1. O que é um ORM

**ORM** significa *Object-Relational Mapping*: mapeamento objeto-relacional. É a técnica (e, geralmente, a biblioteca) que traduz entre dois mundos que representam a informação de formas diferentes:

| Mundo relacional (banco de dados) | Mundo orientado a objetos (Java) |
|---|---|
| Tabela | Classe |
| Linha | Objeto (instância) |
| Coluna | Atributo |
| Chave primária (PK) | Identificador do objeto (`id`) |
| Chave estrangeira (FK) | Referência a outro objeto |

Um ORM existe porque essas duas representações não se encaixam naturalmente. Um banco relacional só entende linhas, colunas e chaves — ele não sabe o que é um objeto Java com métodos e referências para outros objetos. 



---

## 2. Duas formas de representar um relacionamento

Até a aula 5, todo relacionamento do projeto foi representado **por id**:

```java
public class Categoria {
    private int id;
    private String nome;
    private String icone;
    private int casaId;       // <- guarda só o id da Casa
}
```

Para descobrir o nome da casa de uma categoria, é preciso pedir para outra classe (`CasaService`) buscar a `Casa` correspondente a esse id. É exatamente assim que um banco relacional guarda o dado: a linha da tabela `CATEGORIA` tem uma coluna `casa_id`, não a linha inteira da `CASA`.

Um ORM substitui esse id por uma **referência de objeto**:

```java
public class Categoria {
    private int id;
    private String nome;
    private String icone;
    private Casa casa;        // <- guarda a Casa inteira, não o id
}
```

Com `casa` sendo um objeto `Casa` de verdade, `categoria.getCasa().getNome()` já dá acesso ao nome da casa, sem precisar perguntar para nenhum service. O "JOIN" deixa de ser um passo explícito escrito no código (como em `TransacaoService.toResponse()`) e passa a acontecer no momento em que o objeto é montado.

| | Por id (o que fizemos até a aula 5) | Por referência de objeto (ORM) |
|---|---|---|
| Atributo | `int casaId` | `Casa casa` |
| Para obter o nome da casa | Perguntar para `CasaService` | `categoria.getCasa().getNome()` |
| Reflete o que existe no banco | Sim, é literalmente a coluna FK | Não diretamente — é a versão "montada" em memória |
| Quem faz a montagem | O código do service, explicitamente | O ORM, escondido, ou (neste projeto) nós, na mão |

Nenhuma das duas formas está "errada": a representação por id é o que realmente existe na tabela; a representação por objeto é o que um ORM entrega para o resto da aplicação usar. O ORM existe justamente para converter de uma forma para a outra.

---

## 3. Implementando 1 para N sem Spring Data

Tomando o relacionamento `CASA ||--o{ CATEGORIA` como exemplo, a versão com id (atual) é:

```java
public class Categoria {
    private int id;
    private String nome;
    private int casaId;
    // ...
}
```

Para reescrever esse relacionamento como faria um ORM, o lado N (`Categoria`, o lado "muitos") passa a guardar uma referência direta para o objeto do lado 1 (`Casa`):

```java
public class Categoria {
    private int id;
    private String nome;
    private String icone;
    private Casa casa;

    public Categoria(int id, String nome, String icone, Casa casa) {
        this.id = id;
        this.nome = nome;
        this.icone = icone;
        this.casa = casa;
    }

    public Casa getCasa() {
        return casa;
    }
    // demais getters...
}
```

A responsabilidade de "montar" esse objeto, isto é, de pegar o `casaId` que veio do banco (ou, no nosso caso, da lista fake) e descobrir *qual* `Casa` ele referencia, continua sendo de alguém. 

```java
@Service
public class CategoriaService {

    private final CasaService casaService;

    public CategoriaService(CasaService casaService) {
        this.casaService = casaService;
    }

    private List<Categoria> montarCategorias() {
        Casa casaAlimentacao = casaService.buscarModelPorId(1);

        return new ArrayList<>(List.of(
                new Categoria(1, "Alimentação", "🍔", casaAlimentacao),
                new Categoria(2, "Transporte", "🚗", casaAlimentacao)
        ));
    }
}
```


---

## 4. Implementando N para M sem Spring Data

O relacionamento N para M é diferente do 1-N porque **nenhum dos dois lados** cabe inteiro dentro de uma única linha do outro lado: uma categoria pode ter várias transações, mas uma transação também poderia, em outro domínio, pertencer a várias categorias (ex.: tags). O projeto `casalapp` não tem um N-M hoje, então vamos usar um exemplo didático: **usuários e casas**, imaginando que um usuário pudesse participar de mais de uma casa (ex.: separado e ainda dividindo contas com o ex-cônjuge) e uma casa tivesse vários usuários.

Em um banco relacional, um N-M sempre precisa de uma **tabela associativa** (também chamada de tabela de junção) para existir:

```
USUARIO }o--o{ CASA : participa
```

não pode ser representado com uma FK simples de um lado só — nenhum dos dois lados tem "espaço" para guardar múltiplos ids em uma única coluna. A tabela associativa resolve isso guardando **uma linha para cada par** `(usuarioId, casaId)`:

```
USUARIO_CASA
  usuarioId FK
  casaId    FK
```

### Abordagem 1: lista de referências dos dois lados

A forma mais direta de representar isso em Java, sem framework, é cada classe guardar uma `List` de referências para a outra:

```java
public class Usuario {
    private int id;
    private String nome;
    private List<Casa> casas;
    // getters...
}

public class Casa {
    private int id;
    private String nome;
    private List<Usuario> usuarios;
    // getters...
}
```


### Abordagem 2: uma classe própria para a associação

Quando o relacionamento N-M carrega alguma informação própria, e não é só "A está ligado a B", a tabela associativa vira uma classe com identidade própria em vez de duas listas soltas. É o caso, por exemplo, se quiséssemos guardar *desde quando* aquele usuário participa daquela casa:

```java
public class Participacao {
    private int id;
    private Usuario usuario;
    private Casa casa;
    private LocalDate desde;
    // getters...
}
```

Aqui, `Participacao` é exatamente o equivalente Java da tabela `USUARIO_CASA`, só que, em vez de guardar `usuarioId` e `casaId` (dois `int`), guarda `usuario` e `casa` (duas referências de objeto) — seguindo a mesma lógica do 1-N da seção anterior. Um `UsuarioService` que precisasse listar "todas as casas de um usuário" percorreria a lista de `Participacao`, filtrando pelas que têm aquele `usuario`, e coletando o `casa` de cada uma — o JOIN manual, de novo, só que agora atravessando uma tabela a mais.

---

## Diagramas complementares

- [Diagrama de sequência](6_models_orm_sequencia.mmd) — a cadeia de chamadas `TransacaoController → TransacaoService → CategoriaService → CasaService` para o `GET /transacoes/{id}`, mostrando onde cada "JOIN" manual acontece.
- [Diagrama de classe com ORM aplicado ao modelo ER](../documentacao/diagrama_classe_orm.mmd) — as entidades de `documentacao/diagrama_er.mmd` reescritas como classes Java, trocando cada FK por uma referência de objeto.
