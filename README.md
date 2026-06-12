# web2-2026-spring

Repositório da disciplina **Web 2** do IFPR. O conteúdo cobre o desenvolvimento de aplicações web com **Java 21** e **Spring Boot 4**, utilizando Maven como ferramenta de build.

## Projeto de aplicação: Ca$alApp

Aplicativo mobile de gestão financeira compartilhada para casais. Permite registrar receitas e despesas, categorizar lançamentos, definir orçamentos mensais por categoria e calcular rateios automáticos (por média ou proporcional à renda de cada membro). 

## Estrutura do repositório

```
web2-2026-spring/
├── aulas/          # Material didático das aulas
├── casalapp/       # Projeto Spring Boot
└── documentacao/   # Visão geral e casos de uso do Ca$alApp
```

## Conteúdo das aulas

| Branch | Tema |
|--------|------|
| `aula-01-spring-web` | Setup do ambiente, Spring Initializr, estrutura do projeto, anotações do Spring, Controllers e rotas HTTP com `@RestController` |
| `aula-02-spring-controller` | Controllers em profundidade: `@PathVariable`, `ResponseEntity`, DTOs de resposta (`record`), `GET` e `DELETE` por id |
| `aula-03-spring-controller` | `POST`, `PUT` e `PATCH`: `@RequestBody`, DTO de request, ArrayList como banco fake, criação com `201 Created`, substituição completa vs atualização parcial |

## Pré-requisitos

* Java 21
* Maven

## Rodando o projeto

```bash
cd casalapp
./mvnw spring-boot:run
```

O servidor sobe em `http://localhost:8080`.
