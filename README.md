# web2-2026-spring

Repositório da disciplina **Web 2** do IFPR. O conteúdo cobre o desenvolvimento de aplicações web com **Java 21** e **Spring Boot 4**, utilizando Maven como ferramenta de build.

## Projeto de aplicação: Ca$alApp

Aplicativo mobile de gestão financeira compartilhada para casais. Permite registrar receitas e despesas, categorizar lançamentos, definir orçamentos mensais por categoria e calcular rateios automáticos (por média ou proporcional à renda de cada membro). 

## Estrutura do repositório

```
web2-2026-spring/
├── aulas/              # Material didático das aulas
│   └── atividades/     # Exercícios e atividades propostas em cada aula
├── casalapp/           # Projeto Spring Boot
├── casalapp-frontend/  # Frontend estático consumindo a API
└── documentacao/       # Visão geral e casos de uso do Ca$alApp
```

## Conteúdo das aulas

| Branch | Tema |
|--------|------|
| `aula-01-spring-web` | Setup do ambiente, Spring Initializr, estrutura do projeto, anotações do Spring, Controllers e rotas HTTP com `@RestController` |
| `aula-02-spring-controller` | Controllers em profundidade: `@PathVariable`, `ResponseEntity`, DTOs de resposta (`record`), `GET` e `DELETE` por id |
| `aula-03-spring-controller` | `POST`, `PUT` e `PATCH`: `@RequestBody`, DTO de request, ArrayList como banco fake, criação com `201 Created`, substituição completa vs atualização parcial |
| `aula-04-spring-relacao-vetor` | Relacionamento entre recursos: `TransacaoController` relacionado a `Categoria` via `categoriaId`, pacote `model/` (classes tradicionais, separadas dos DTOs em `record`) e INNER JOIN manual com loops básicos |
| `aula-05-spring-service` | Camada de `service/`: regra de visibilidade entre `controller`, `service`, `model` e `dto`, anotação `@Service`, injeção de dependência por construtor vs `@Autowired`, `CategoriaService` centralizando dados e conversão para DTO. Exercício: entidade `Casa` (UC-5) relacionada a `Categoria` via `casaId`, e `TransacaoService` resolvendo o join via `CategoriaService` |
| `aula-06-model-orm1` | ORM  |
| `aula-07-model-persistence` | Camada de `repository/` |
| `aula-08-spring-data` | Spring Data JPA: anotações de entidade (`@Entity`, `@Id`, `@GeneratedValue`, `@ManyToOne`, `@JoinColumn`), `JpaRepository` substituindo o `repository/` escrito à mão, persistência real em MySQL, `ddl-auto` e seus limites como ferramenta de migração |
| `aula-09-spring-validation-frontend` | `casalapp-frontend/`: front estático (HTML + Bootstrap + JS puro) separado em camadas `api/` e `pages/`, integrado via `fetch`; CORS liberado no backend; validação de request com Bean Validation (`@NotBlank`, `@NotNull`, `@Positive`, `@Email`) aplicada via `@Valid` em `POST`/`PUT`; documentação da API com springdoc-openapi (Swagger UI); nova entidade `Usuario` associada a `Casa`, preparando o modelo de dados para autenticação (sem implementar autenticação) |
| `aula-10-auth-login` | Autenticação, parte 1: fluxo de login sem proteção (propositalmente inseguro). Rota `POST /auth/login` com `AuthController`/`AuthService`, os métodos mágicos do `repository` (consultas derivadas do nome do método: gramática, navegação por relacionamentos, tipos de retorno, erro na subida da aplicação e a saída via `@Query`/JPQL), aplicados em `findByEmail` e `existsByEmail`, email como identificador único (`@Column(unique = true)`) e o limite do `ddl-auto` diante de dados antigos, `LoginRequestDTO`/`LoginResponseDTO`/`ErroResponseDTO`, `401 Unauthorized` com mensagem genérica (enumeração de usuários) e `409 Conflict` no cadastro de email repetido, tela `login.html` com camadas `api/authApi.js` e `pages/login.js` guardando o usuário logado apenas em memória; diagrama de atividade do fluxo de autenticação e diagramas de sequência da requisição de login e da geração do proxy do repository |
| `aula-11-auth-hash-token` | Autenticação, parte 2: os quatro defeitos da aula 10 corrigidos. Hash de senha com `BCryptPasswordEncoder` (`spring-security-crypto`, sem o starter completo, sem entrar na teoria de criptografia), `encode`/`matches` e por que `equals` não serve dado o salt sorteado a cada chamada, bean declarado com `@Bean` em `SegurancaConfig`, campo `senhaHash` e a migração manual que o `ddl-auto` não faz; sessão com token opaco na entidade `Token` (`UUID.randomUUID`, `expiraEm`, `estaExpirado`) trafegando em `Authorization: Bearer`; proteção de rotas com `HandlerInterceptor` registrado em `addInterceptors`, `preHandle` devolvendo `false` com `401`, exceções para o preflight do CORS e para o cadastro público; identificação do usuário logado via `request.setAttribute` e `@RequestAttribute`, rota `GET /auth/eu`, `POST /auth/logout` e `GET /transacoes` filtrado pela casa de quem pede (`findByCategoriaCasaId`); frontend com camada `js/sessao.js`, token no `localStorage`, `requisicaoAutenticada` centralizando o cabeçalho, rotas privadas e expulsão no `401`; diagramas do fluxo do hash, do caminho de decisão do interceptor e da sequência completa da sessão |

## Pré-requisitos

* Java 21
* Maven

## Rodando o projeto

```bash
cd casalapp
./mvnw spring-boot:run
```

O servidor sobe em `http://localhost:8080`.
