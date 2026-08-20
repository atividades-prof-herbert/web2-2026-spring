# Aula 8: Spring Data JPA — persistência real em banco de dados

## Revisão: Aula 7

Na aula anterior criamos a camada `repository/`, mas ela ainda guardava os dados numa `List` em memória, dentro de classes anotadas com `@Repository` escritas à mão (`buscarPorId` percorrendo a lista com `for`, `salvar` controlando um `proximoId`, etc.). O ganho da aula 7 foi organizacional: separar "onde o dado mora" de "qual regra de negócio se aplica a ele". O dado em si continuava desaparecendo toda vez que a aplicação era reiniciada.

Nesta aula, essa `List` em memória é substituída por um banco de dados **MySQL** de verdade, usando **Spring Data JPA**. O objetivo é que, ao final, os dados sobrevivam a um restart da aplicação — e que a forma de escrever a camada `repository/` fique, paradoxalmente, **mais simples** do que estava na aula 7, não mais complexa.

---

## 1. Duas peças novas: JPA e Spring Data

Duas tecnologias diferentes entram em jogo, e vale separar o que cada uma faz:

| Tecnologia | O que é | Papel neste projeto |
|---|---|---|
| **JPA** (*Jakarta Persistence API*) | Uma especificação: um conjunto de anotações e interfaces (`@Entity`, `@Id`, `@ManyToOne`...) que define *como* mapear uma classe Java para uma tabela. Quem implementa essa especificação, de fato, é o **Hibernate** (incluído por baixo do `spring-boot-starter-data-jpa`). | Fornece as anotações usadas em `model/` e o motor que traduz objeto Java ⇄ linha de tabela. |
| **Spring Data JPA** | Um projeto do Spring construído *em cima* da JPA. Sua função é eliminar a necessidade de implementar manualmente métodos de CRUD (`buscarPorId`, `salvar`, `deletar`...): basta declarar uma **interface**, e o Spring gera a implementação em tempo de execução. | Fornece a interface `JpaRepository`, usada em `repository/`. |

Ou seja: a JPA resolve o problema "como uma `Categoria` vira uma linha da tabela `categoria`" (o que era resolvido manualmente com os `for` da aula 7); o Spring Data resolve o problema "quem escreve o `INSERT`/`SELECT`/`UPDATE`/`DELETE`" (o que também era escrito manualmente na aula 7).

---

## 2. Dependências novas: `pom.xml`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

- `spring-boot-starter-data-jpa` traz, transitivamente, o Hibernate (a implementação da JPA) e o Spring Data JPA (a geração automática de repositories).
- `mysql-connector-j` é o **driver JDBC**: a biblioteca que sabe, especificamente, como abrir uma conexão de rede com um MySQL e traduzir comandos SQL para o protocolo que esse banco entende. Sem essa dependência, o Hibernate saberia *o que* enviar (SQL), mas não *como* enviar para um MySQL. Ela fica em `scope=runtime` porque nenhuma classe do projeto a referencia diretamente no código — só é usada internamente pelo pool de conexões.

---

## 3. Configuração da conexão: `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/casal-app-web2?createDatabaseIfNotExist=true&useTimezone=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

| Propriedade | Para que serve |
|---|---|
| `spring.datasource.url` | Endereço JDBC do banco: protocolo (`jdbc:mysql://`), host e porta (`localhost:3306`), e o **nome do banco** (`casal-app-web2`). `createDatabaseIfNotExist=true` diz ao driver para criar esse banco automaticamente na primeira conexão, caso ele ainda não exista no servidor MySQL. |
| `spring.datasource.username` / `password` | Credenciais usadas para autenticar no MySQL. |
| `spring.datasource.driver-class-name` | Qual driver JDBC usar para abrir a conexão — o Spring Boot normalmente detecta isso sozinho a partir do `mysql-connector-j` no classpath, mas deixamos explícito. |
| `spring.jpa.hibernate.ddl-auto` | Controla se e como o Hibernate altera o **schema** (as tabelas) automaticamente para bater com as classes anotadas com `@Entity`. Detalhado na seção 7. |
| `spring.jpa.show-sql` / `format_sql` | Fazem o Hibernate imprimir, no console, cada SQL que ele gera — essencial para entender o que está acontecendo por baixo dos panos enquanto se aprende. Em produção normalmente fica desligado. |

Repare que **nada disso aparece em código Java**. A camada `service/` e `controller/` não sabem, e não precisam saber, que existe um MySQL, um host, uma porta ou uma senha — só o Spring, ao ler `application.properties` na inicialização, sabe disso.

---

## 4. Transformando `model/` em entidades JPA

Até a aula 7, `Casa`, `Categoria` e `Transacao` eram classes Java comuns (POJOs), sem nenhuma relação formal com um banco — a "tradução" para lista era feita manualmente, dentro do repository. Agora essas classes ganham anotações que dizem ao Hibernate exatamente como mapear cada uma para uma tabela.

### 4.1 `@Entity` e `@Table`

```java
@Entity
@Table(name = "casa")
public class Casa {
    // ...
}
```

- `@Entity` marca a classe como uma **entidade JPA**: o Hibernate passa a gerenciá-la, sabendo que instâncias dela correspondem a linhas de alguma tabela.
- `@Table(name = "casa")` diz explicitamente qual o nome dessa tabela. É opcional — sem ela, o Hibernate usaria o nome da classe (`Casa`) convertido para a convenção de nomes do banco. Escrever explicitamente deixa claro e evita surpresas.

### 4.2 `@Id` e `@GeneratedValue`

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private int id;
```

- `@Id` marca qual atributo é a **chave primária** da tabela.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` delega a geração do valor desse id **para o banco**, usando a coluna `AUTO_INCREMENT` do MySQL. É o substituto direto do `proximoId` que cada `repository` controlava manualmente na aula 7: antes, o `int proximoId` e o `proximoId++` viviam no Java; agora, essa responsabilidade sai do código e vai para o banco — exatamente como já acontecia comentário por comentário na aula 7 ("é assim que um banco de verdade funciona: uma coluna com auto-incremento é responsabilidade do banco, não de quem faz o `INSERT`"). Aqui isso deixa de ser só uma analogia e passa a ser literal.

### 4.3 `@ManyToOne` e `@JoinColumn`

Na aula 6 fizemos, **na mão**, a diferença entre representar um relacionamento por id (`int casaId`) ou por referência de objeto (`Casa casa`). Com JPA, essa segunda forma é declarada com uma anotação, e o "JOIN" deixa de ser escrito manualmente:

```java
@Entity
@Table(name = "categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nome;
    private String icone;

    @ManyToOne
    @JoinColumn(name = "casa_id")
    private Casa casa;

    // ...
}
```

- `@ManyToOne` declara o lado "muitos" do relacionamento **1 para N** entre `Casa` e `Categoria`: muitas categorias pertencem a uma casa.
- `@JoinColumn(name = "casa_id")` diz qual é o nome da coluna de chave estrangeira (FK) na tabela `categoria` que aponta para `casa.id`.

O mesmo padrão se repete entre `Transacao` e `Categoria`:

```java
@ManyToOne
@JoinColumn(name = "categoria_id")
private Categoria categoria;
```

Com isso, `categoria.getCasa().getNome()` e `transacao.getCategoria().getNome()` funcionam de verdade — o Hibernate é quem, ao carregar uma `Categoria` do banco, executa o `SELECT` (ou usa um `JOIN`) para preencher o atributo `casa`. O "JOIN manual" que os `service`s faziam desde a aula 4, perguntando para outro repository/service pelo id, deixa de ser necessário para *ler* o relacionamento.

### 4.4 O construtor sem argumentos

```java
public Casa() {
}
```

O Hibernate precisa conseguir instanciar a entidade **antes** de preencher seus atributos. Por isso, toda entidade `@Entity` precisa de um construtor público ou protegido sem argumentos, além de qualquer construtor "de conveniência" que o código da aplicação continue usando.

### 4.5 Lazy vs Eager: quando o relacionamento é carregado

Toda anotação de relacionamento (`@ManyToOne`, `@OneToMany`, etc.) tem um atributo `fetch`, que decide **quando** o Hibernate busca o objeto relacionado no banco. Existem duas estratégias:

| Estratégia | Quando o relacionamento é carregado | Quantas consultas SQL |
|---|---|---|
| **Eager** (`FetchType.EAGER`) | Junto com o objeto principal, na mesma hora em que ele é carregado. | O Hibernate já traz o relacionado no mesmo `SELECT` (ou dispara um `SELECT` adicional imediatamente), mesmo que o código nunca chegue a usar `categoria.getCasa()`. |
| **Lazy** (`FetchType.LAZY`) | Só no momento em que o código efetivamente acessa o atributo (ex.: `categoria.getCasa()`). Até lá, o Hibernate guarda apenas um *proxy* (um objeto "vazio", que sabe qual id buscar, mas ainda não buscou). | Um `SELECT` a mais é disparado **na hora do acesso**, não antes. |

No projeto, `@ManyToOne` **não declara `fetch` explicitamente**:

```java
@ManyToOne
@JoinColumn(name = "casa_id")
private Casa casa;
```

Isso importa porque o padrão da JPA **não é o mesmo para todo tipo de relacionamento**:

- `@ManyToOne` e `@OneToOne` são **Eager por padrão**.
- `@OneToMany` e `@ManyToMany` são **Lazy por padrão**.

Ou seja: sempre que `CategoriaRepository.findById(id)` busca uma `Categoria`, o Hibernate já traz a `Casa` associada junto, sem precisar de nenhuma configuração adicional — é por isso que `categoria.getCasa().getNome()` funciona direto em `CategoriaService.toResponse`, mesmo sem nenhum código explícito buscando a `Casa` de novo.

Para forçar o comportamento oposto, a anotação aceita o parâmetro `fetch`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "casa_id")
private Casa casa;
```

**Por que essa escolha importa na prática:**

- **Eager** é mais simples de raciocinar (o objeto relacionado sempre "já está lá"), mas pode trazer dado que ninguém vai usar, e, em relacionamentos maiores, pode disparar mais `SELECT`s do que o necessário.
- **Lazy** evita buscar o que não será usado, mas tem uma armadilha clássica: se o código tentar acessar `categoria.getCasa()` **depois** que a conexão com o banco daquela requisição já foi fechada (por exemplo, dentro do `controller`, ou depois que o `service` já retornou), o Hibernate não consegue mais buscar o proxy e lança `LazyInitializationException`. É por isso que, no projeto, a conversão para DTO (`toResponse`) acontece sempre **dentro do `service`**, enquanto a "sessão" do Hibernate ainda está aberta — outro motivo, além da separação de camadas já discutida na aula 5, para o `model` nunca vazar para fora do `service`.

Como as entidades deste projeto são pequenas e cada `service` já lê o relacionamento na mesma chamada em que busca a entidade principal, manter o padrão (Eager em `@ManyToOne`) é a escolha mais simples. Em relacionamentos maiores — por exemplo, se `Casa` guardasse uma lista de todas as suas `Categoria` (`@OneToMany`) — o padrão Lazy evita carregar uma coleção inteira só para responder um `GET /casas/{id}` que nem precisa dela.

---

## 5. `repository/`

Esta é a mudança mais visível da aula. Compare o `CasaRepository` da aula 7:

```java
@Repository
public class CasaRepository {
    private final List<Casa> casas = new ArrayList<>(...);

    public List<Casa> listar() { ... }
    public Casa buscarPorId(int id) { ... /* for */ }
}
```

com o `CasaRepository` desta aula:

```java
package br.edu.ifpr.casalapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifpr.casalapp.model.Casa;

public interface CasaRepository extends JpaRepository<Casa, Integer> {
}
```

Não sobrou nenhum método, nenhum `for`, nenhuma `List`, nenhuma implementação — e a classe virou **interface**.

### 5.1 Os dois parâmetros de `JpaRepository<Casa, Integer>`

`JpaRepository` é uma interface **genérica**: ela declara dois **parâmetros de tipo**, que precisa preencher.

```java
public interface CasaRepository extends JpaRepository<Casa, Integer> {
}
```

| Parâmetro | Valor neste caso | O que significa |
|---|---|---|
| `T` (1º parâmetro) | `Casa` | Qual **entidade** esse repository gerencia. Define o tipo de retorno de `findAll()` (`List<Casa>`), `findById()` (`Optional<Casa>`), o tipo de parâmetro de `save(Casa)`, etc. |
| `ID` (2º parâmetro) | `Integer` | O tipo do **`@Id`** dessa entidade — o mesmo tipo do atributo `private int id` em `Casa` (`Integer`, porque genéricos em Java não aceitam tipos primitivos como `int` diretamente). Define o tipo de parâmetro de `findById(Integer)`, `existsById(Integer)`, `deleteById(Integer)`. |

Sem esses dois parâmetros preenchidos, `JpaRepository` sozinha não sabe *nada* sobre o projeto — ela não conhece `Casa`, não sabe qual é a chave primária. É só um "molde" de repository, escrito uma única vez pelo Spring Data, capaz de servir para qualquer entidade de qualquer projeto que a use. `CategoriaRepository extends JpaRepository<Categoria, Integer>` e `TransacaoRepository extends JpaRepository<Transacao, Integer>` reaproveitam exatamente o mesmo molde, só trocando o `T`.

**O conceito de orientação a objetos por trás disso é polimorfismo, na forma de generics (polimorfismo paramétrico).** A ideia central do polimorfismo é escrever um código que funciona de forma uniforme para tipos diferentes, sem duplicar a lógica para cada um. Sem generics, `JpaRepository` teria duas opções ruins: (1) trabalhar com `Object` (perdendo checagem de tipo — qualquer `cast` errado só quebraria em tempo de execução), ou (2) uma interface separada para cada entidade (`CasaRepository`, `CategoriaRepository`... cada uma com seus próprios métodos `save(Casa)`, `save(Categoria)` escritos à mão). Generics resolve os dois problemas ao mesmo tempo: o **mesmo código** de `save`, `findById`, `findAll` etc. serve para qualquer entidade, e o compilador continua sabendo, em cada uso específico (`CasaRepository`), que ali só circula `Casa` — o erro de tipo é pego na hora de compilar, não em produção.

Em tempo de execução, o Spring Data **gera automaticamente** uma implementação dessa interface e a registra como *bean*, do mesmo jeito que `@Service` e `@Repository` registravam beans manualmente escritos. Essa implementação já vem com métodos prontos, entre eles:

| Método herdado de `JpaRepository` | Equivalente manual da aula 7 |
|---|---|
| `save(entidade)` | `salvar(...)` — insere se o id for novo, atualiza se já existir |
| `findById(id)` → `Optional<T>` | `buscarPorId(id)` |
| `findAll()` | `listar()` |
| `existsById(id)` | verificação feita "na mão" antes de atualizar/deletar |
| `deleteById(id)` | `deletar(id)` |

Repare que nenhuma anotação `@Repository` foi escrita nesta interface — o Spring Data já sabe que qualquer interface que estende `JpaRepository` deve virar um bean gerenciado, então a anotação manual (necessária na aula 7, para uma classe comum) deixou de ser preciso aqui.

`CategoriaRepository` e `TransacaoRepository` seguem exatamente o mesmo formato, cada um com sua entidade.

---

## 6. `service/`: volta a ser só regra de negócio

Com o `repository/` cuidando de tudo que é acesso a dado, o `service/` muda pouco na estrutura, mas troca chamadas manuais por chamadas ao `JpaRepository`. Veja `CasaService.criar`:

```java
public CasaResponseDTO criar(CasaRequestDTO request) {
    Casa nova = new Casa(0, request.nome(), gerarCodigoConvite());
    Casa salva = casaRepository.save(nova);
    return toResponse(salva);
}
```

O `id = 0` é só um placeholder, igual na aula 7: como o atributo `id` está anotado com `@GeneratedValue`, o Spring Data entende que uma entidade com id "zerado" ainda não existe no banco, e `save()` executa um `INSERT`, devolvendo a entidade já com o id de verdade preenchido pelo MySQL.

### 6.1 O que é `Optional<T>`

`findById` não devolve `Casa` diretamente, nem `null` quando não encontra nada (como os métodos escritos à mão na aula 7) — devolve `Optional<Casa>`. `Optional<T>` é outra classe **genérica** do Java (`java.util.Optional`, o mesmo `<T>` de `List<T>` e de `JpaRepository<T, ID>`), mas o que ela guarda não é uma coleção de itens: é **um valor que pode existir ou não**. Em vez de o método devolver `Casa` e, silenciosamente, esse `Casa` poder ser `null`, ele devolve uma "caixa" (`Optional<Casa>`) que ou contém uma `Casa` lá dentro, ou está vazia — e isso fica explícito **no tipo de retorno**, visível já na assinatura do método, sem precisar ler o corpo dele ou a documentação para saber que o resultado pode faltar.

Isso resolve um problema conhecido do `null` em Java: nada no tipo `Casa` avisa que uma referência pode estar ausente, então é fácil esquecer o `if (casa != null)` e o programa quebra em produção com `NullPointerException`. Com `Optional<Casa>`, o compilador força a abrir a caixa antes de usar o que está (ou não) dentro dela — os métodos mais usados para isso são:

| Método | O que faz |
|---|---|
| `isPresent()` | `true` se tem um valor dentro |
| `isEmpty()` | `true` se está vazio (o oposto de `isPresent()`) |
| `get()` | Devolve o valor de dentro — só deve ser chamado depois de confirmar `isPresent()`, senão lança exceção |


```java
public CasaResponseDTO buscarPorId(int id) {
    Optional<Casa> casaOpt = casaRepository.findById(id);
    if (casaOpt.isPresent()) {
        return toResponse(casaOpt.get());
    }
    return null;
}
```

`Optional<T>` não tem como ser evitado aqui. É o tipo de retorno que `JpaRepository.findById` devolve, faz parte da assinatura do método herdado. 


```java
public CategoriaResponseDTO criar(CategoriaRequestDTO request) {
    Casa casa = null;
    Optional<Casa> casaOpt = casaRepository.findById(request.casaId());
    if (casaOpt.isPresent()) {
        casa = casaOpt.get();
    }

    Categoria nova = new Categoria(0, request.nome(), request.icone(), casa);
    Categoria salva = categoriaRepository.save(nova);
    return toResponse(salva);
}
```

E montar o DTO de resposta não precisa mais perguntar para outro repository pelo nome da casa, o relacionamento já vem carregado no objeto:

```java
private CategoriaResponseDTO toResponse(Categoria categoria) {
    Casa casa = categoria.getCasa();
    int casaId = casa != null ? casa.getId() : 0;
    String casaNome = casa != null ? casa.getNome() : null;

    return new CategoriaResponseDTO(categoria.getId(), categoria.getNome(), categoria.getIcone(), casaId, casaNome);
}
```


**`controller/` continua sem nenhuma alteração**, pelo mesmo motivo apontado na aula 7: ele só troca `dto` com o `service`, então trocar a implementação de `repository/` (de lista em memória para banco real) é, de novo, invisível para ele.

---

## 7. O que acontece quando o `model` muda: `ddl-auto`

A propriedade `spring.jpa.hibernate.ddl-auto=update` controla o que o Hibernate faz com o **schema** do banco (as tabelas, colunas e constraints) toda vez que a aplicação sobe, comparando o que existe no banco com o que as classes `@Entity` declaram. Os valores possíveis:

| Valor | Comportamento |
|---|---|
| `none` | Não toca no schema. O banco precisa já existir, com a estrutura certa, criada por fora. |
| `validate` | Não altera nada, mas falha a inicialização se o schema do banco não bater com as entidades. |
| `update` | Compara entidades com o schema atual e aplica só as mudanças necessárias para "alcançar" as entidades: cria tabelas que faltam, adiciona colunas que faltam. **Nunca remove nem renomeia nada.** (usado neste projeto) |
| `create` | Apaga e recria todo o schema toda vez que a aplicação sobe. Os dados são perdidos a cada restart. |
| `create-drop` | Como `create`, e além disso apaga o schema quando a aplicação encerra. Útil só para testes automatizados. |

### O que aconteceu na primeira subida da aplicação

Com o banco `casal-app-web2` vazio, subir a aplicação pela primeira vez (com `ddl-auto=update`) gerou, e executou automaticamente, este SQL (capturado do log, graças a `show-sql=true`):

```sql
create table casa (
    id integer not null auto_increment,
    codigo_convite varchar(255),
    nome varchar(255),
    primary key (id)
) engine=InnoDB

create table categoria (
    id integer not null auto_increment,
    icone varchar(255),
    nome varchar(255),
    casa_id integer,
    primary key (id)
) engine=InnoDB

create table transacao (
    id integer not null auto_increment,
    descricao varchar(255),
    valor float(53),
    categoria_id integer,
    primary key (id)
) engine=InnoDB

alter table categoria
   add constraint FK4jgpvdk39ouc5yicgi9swbk9t
   foreign key (casa_id)
   references casa (id)

alter table transacao
   add constraint FK1i3bdmrp3xf5fvs84gc9w2iid
   foreign key (categoria_id)
   references categoria (id)
```

Repare em três coisas:

1. **Nomes de coluna em `snake_case`**: os atributos Java `codigoConvite` e `casaId`/`categoriaId` (dos `@JoinColumn`) viraram `codigo_convite`, `casa_id`, `categoria_id`. Essa tradução (`camelCase` → `snake_case`) é feita automaticamente pelo padrão do Hibernate — não precisamos declarar isso em lugar nenhum.
2. **A FK só aparece depois das duas tabelas existirem**: o Hibernate ordena os `CREATE TABLE` e só executa o `ALTER TABLE ... FOREIGN KEY` depois, exatamente porque a FK depende das duas tabelas já existirem.
3. **Isso tudo veio só das anotações em `model/`**: nenhuma linha de SQL foi escrita manualmente em lugar nenhum do projeto.

