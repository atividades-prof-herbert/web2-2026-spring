# Aula 7 (atividade): repository para `Categoria` e `Casa`

Na aula, refatoramos **só o `TransacaoService`** para usar a camada `repository/`. `CategoriaService` e `CasaService` continuam exatamente como estavam na aula 6: cada um guarda sua própria `List` em memória e faz seu próprio "JOIN manual".

Isso deixou um problema pendente, apontado na seção 3.2 de [7_models-persistence.md](7_models-persistence.md): hoje existem **duas listas de categoria** (uma em `CategoriaService`, outra em `CategoriaRepository`) e **duas listas de casa** (uma em `CasaService`, outra em `CasaRepository`), sem nenhuma relação entre elas. Uma categoria criada via `POST /categorias` não aparece para o `TransacaoService`, porque ele lê de uma lista diferente.

## O que fazer

Refatore `CategoriaService` e `CasaService` seguindo exatamente o padrão usado no `TransacaoService`.

1. **`CasaService`**
   - Remova a `List<Casa>` e o `proximoId` de dentro do `CasaService`.
   - Use a `CasaRepository` já criada na aula (ela hoje só tem `listar()` e `buscarPorId(id)`; adicione os métodos que faltam: `salvar`, `atualizar` e `deletar`, seguindo o modelo de `TransacaoRepository`).
   - `CasaService` passa a injetar `CasaRepository` no construtor e delegar todo acesso a dados para ela. A regra de negócio que já existe hoje no `CasaService` (como a geração do `codigoConvite` em `criar`) continua no `service`, porque isso não é acesso a banco, é regra de negócio.

2. **`CategoriaService`**
   - Mesma coisa: remova a `List<Categoria>` e o `proximoId`, mova para dentro de `CategoriaRepository` (adicionando `salvar`, `atualizar` e `deletar`).
   - `CategoriaService` continua responsável por montar o `CategoriaResponseDTO` com o `casaNome`, mas agora, para isso, ele deveria usar `CasaRepository` (não mais `CasaService`) para buscar o `model` da casa. Pense: por que usar o repository da `Casa`, e não o `service`, evita reproduzir o problema de dependência circular que vimos na aula 6?

3. **Depois de refatorar os dois, apague a duplicação**: confira se `TransacaoService` ainda está usando o **mesmo** `CategoriaRepository` e `CasaRepository` que `CategoriaService` e `CasaService` passaram a usar. Como cada `@Repository` é uma única classe (um único bean gerenciado pelo Spring), a duplicação de dados deve desaparecer sozinha: todo mundo lê e escreve na mesma lista.

4. **Teste manualmente**: crie uma categoria nova com `POST /categorias` e, em seguida, crie uma transação para essa categoria com `POST /transacoes`. Confirme que `GET /transacoes/{id}` devolve o `categoriaNome` e o `casaNome` corretos, isso só funciona se as duas camadas estiverem, de fato, lendo a mesma fonte de dados.

## Checklist de verificação

- [ ] `CasaService` não tem mais nenhuma `List` nem controle de id, só regra de negócio e chamadas para `CasaRepository`.
- [ ] `CategoriaService` não tem mais nenhuma `List` nem controle de id, só regra de negócio e chamadas para `CategoriaRepository` (e `CasaRepository`, para montar `casaNome`).
- [ ] `CasaRepository` e `CategoriaRepository` têm `listar`, `buscarPorId`, `salvar`, `atualizar` e `deletar`.
- [ ] `CasaController` e `CategoriaController` continuam **sem nenhuma alteração**. Se você precisou mexer neles, algo na refatoração do `service` foi longe demais.
- [ ] Uma categoria criada via API aparece corretamente no `categoriaNome` de uma transação criada depois dela.
