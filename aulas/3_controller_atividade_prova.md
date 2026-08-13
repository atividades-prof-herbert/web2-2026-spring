# Prova do 2 Bimestre — Spring Boot + Frontend

**Disciplina:** Desenvolvimento Web 2

**Entrega:** repositório Git de atividades da disciplina Web2.
- Crie a pasta `B2A3`;
- Dentro da pasta `B2A3` crie a pasta `backend` com o projeto Spring Boot;
- Dentro da pasta `B2A3` crie a pasta `frontend` com o html e javascript.


---

## Contexto

Você vai construir a funcionalidade de gerenciamento de transações financeiras de um aplicativo de casal. O backend já possui os endpoints `GET` e `DELETE` implementados. Sua tarefa é completar o backend com os endpoints de criação e atualização parcial, e construir um frontend em HTML + JavaScript que consuma a API.

---

## Código inicial

O código abaixo deve estar presente no seu projeto antes de começar. Não modifique os endpoints `GET` e `DELETE`.

**`TransacaoResponse.java`**

```java
package br.edu.ifpr.casalapp.dto;

public record TransacaoResponse(int id, String descricao, Double valor, String tipo) {}
```

**`TransacaoController.java`** (ponto de partida)

```java
package br.edu.ifpr.casalapp.controller;

import br.edu.ifpr.casalapp.dto.TransacaoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class TransacaoController {

    private int proximoId = 4;

    private final List<TransacaoResponse> transacoes = new ArrayList<>(List.of(
            new TransacaoResponse(1, "Supermercado", 350.00, "despesa"),
            new TransacaoResponse(2, "Salário",     4500.00, "receita"),
            new TransacaoResponse(3, "Academia",      99.90, "despesa")
    ));

    @GetMapping("/transacoes")
    public List<TransacaoResponse> listarTransacoes() {
        return transacoes;
    }

    @GetMapping("/transacoes/{id}")
    public ResponseEntity<TransacaoResponse> buscarTransacao(@PathVariable int id) {
        for (TransacaoResponse t : transacoes) {
            if (t.id() == id) {
                return ResponseEntity.ok(t);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/transacoes/{id}")
    public ResponseEntity<Void> deletarTransacao(@PathVariable int id) {
        for (TransacaoResponse t : transacoes) {
            if (t.id() == id) {
                transacoes.remove(t);
                return ResponseEntity.noContent().build();
            }
        }
        return ResponseEntity.notFound().build();
    }
}
```

---

## Parte 1 — Backend

### 1.1 — DTO de request

Crie a classe `TransacaoRequest` no pacote `br.edu.ifpr.casalapp.dto`.

Ela deve representar os dados que o cliente envia ao criar ou atualizar uma transação. Os campos são: descrição, valor e tipo. O identificador não deve estar presente porque é responsabilidade do servidor gerá-lo. Use `Double` (com D maiúsculo) para o campo de valor.

### 1.2 — `POST /transacoes` 

Implemente o endpoint de criação de transação no `TransacaoController`.

Comportamento esperado:

- O cliente envia os dados da nova transação no corpo da requisição em JSON.
- O servidor cria a transação com um `id` gerado sequencialmente, a adiciona à lista e retorna o objeto completo (incluindo o `id`) no corpo da resposta.
- O status HTTP retornado deve indicar que um novo recurso foi criado.

### 1.3 — `PATCH /transacoes/{id}` 

Implemente o endpoint de atualização parcial no `TransacaoController`.

Comportamento esperado:

- O `id` do recurso a ser alterado vem na URL.
- O cliente envia no corpo da requisição apenas os campos que deseja alterar. Campos ausentes no JSON chegam como `null`.
- O servidor atualiza somente os campos enviados, preservando os valores atuais dos campos não enviados.
- A resposta contém o objeto completo com os valores resultantes e status `200 OK`.
- Se o `id` informado na URL não existir, o servidor retorna `404 Not Found`.

---

## Parte 2 — Frontend

A página deve ser feita com HTML e JavaScript puro (sem frameworks, sem bibliotecas externas).

### 2.1 — Listagem

A página deve exibir, ao carregar, a lista de transações retornada por `GET /transacoes`. Para cada transação exiba: descrição, valor e tipo.

### 2.2 — Formulário de criação

A página deve conter um formulário com campos para descrição, valor e tipo. Ao submeter o formulário:

- Uma requisição `POST /transacoes` é enviada com os dados em JSON.
- A lista exibida na tela é atualizada sem recarregar a página.
- O formulário é limpo após o envio.

### 2.3 — Atualização parcial de descrição

Para cada transação exibida na lista, deve haver um controle que permita ao usuário alterar apenas a descrição. Ao confirmar a alteração:

- Uma requisição `PATCH /transacoes/{id}` é enviada contendo somente o campo `descricao`.
- A lista é atualizada na tela sem recarregar a página.



