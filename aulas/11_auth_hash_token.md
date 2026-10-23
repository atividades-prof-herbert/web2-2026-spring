# Aula 11: autenticação, parte 2. Hash de senha, token e proteção de rotas

> **Aviso ao estudante.** A aula 10 era propositalmente insegura e avisava disso em quatro
> pontos. Esta aula fecha os quatro. O que resulta no final é um esquema de autenticação
> honesto, do tipo que sistemas reais usam, mas ainda incompleto em um aspecto importante, a
> autorização em nível de objeto, que a seção 9 vai nomear e a atividade vai cobrar.

## Revisão: a conta que a aula 10 deixou aberta

A aula anterior terminou com um inventário de quatro defeitos. Vale reler aquela lista, porque
ela é exatamente o roteiro de hoje.

| Defeito deixado pela aula 10 | Seção desta aula que o resolve |
|---|---|
| As senhas estão legíveis no banco. | Seção 2, com funções de hash e o BCrypt. |
| A senha seria reenviada em cada operação. | Seção 3, trocando a senha por um token emitido uma vez. |
| Quem entrou é esquecido imediatamente. | Seções 3 e 5, com a tabela `token` e o cabeçalho `Authorization`. |
| Nada está protegido: as rotas respondem a qualquer um. | Seções 4 e 5, com um interceptor e o filtro por casa. |

Repare que os quatro defeitos não são independentes. Eles formam uma corrente: não dá para
proteger uma rota sem reconhecer quem chama, não dá para reconhecer quem chama sem uma marca
durável, e não faz sentido emitir uma marca durável se a senha que a originou está exposta no
banco. Vamos seguir a corrente nessa ordem.

---

## 1. Autenticação, sessão e autorização

A aula 10 separou identificação, autenticação e autorização. Hoje precisamos de um quarto
conceito no meio do caminho, que é o que torna a autorização possível em um protocolo sem
memória.

| Conceito | Pergunta que ele responde | Onde ele vive nesta aula |
|---|---|---|
| **Autenticação** | Você consegue provar quem diz ser? | `POST /auth/login`, comparando a senha com o hash. |
| **Sessão** | Como continuo sabendo quem você é, sem pedir a senha de novo? | A tabela `token` e o cabeçalho `Authorization`. |
| **Autorização** | Já que sei quem você é, o que você pode fazer e ver? | O `AutenticacaoInterceptor` e o filtro por casa em `GET /transacoes`. |

A frase que encerrou a aula 10, "autenticar não protege nada por si só", pode agora ser
completada: autenticar produz um fato, a sessão transporta esse fato no tempo, e a autorização
decide o que fazer com ele. Nenhum dos três substitui os outros dois.

---

## 2. A senha não deve estar armazenada em lugar nenhum

Comecemos pelo problema mais grave e, felizmente, o de solução mais estabelecida.

### 2.1 Verificar a senha sem armazenar a senha

A percepção que organiza toda esta seção é esta: **o sistema não precisa saber a sua senha**.
Ele só precisa responder a uma pergunta de sim ou não, "a senha que você acabou de digitar é a
mesma de antes?". Essa pergunta pode ser respondida sem nunca guardar a senha, e é isso que
uma função de hash permite, porque dela não existe caminho de volta.

A consequência prática é forte e vale enunciar: depois desta aula, **nem nós conseguimos
descobrir a senha de um usuário**. Essa impossibilidade é a prova de que o dado está
protegido, e explica por que um sistema sério nunca envia a sua senha por email quando você
clica em "esqueci minha senha": ele não tem como, então envia um link para você cadastrar
outra.

O algoritmo escolhido aqui é o **BCrypt**, que é a recomendação corrente para senhas. Duas
características dele importam para o nosso código, e são as únicas que vamos usar:

1. **Ele é lento de propósito.** Em senhas, lentidão é requisito e não defeito, porque encarece
   a vida de quem tentar adivinhar senhas em massa.
2. **Ele sorteia um *salt* aleatório por senha** e o guarda dentro do próprio resultado. Por
   isso, cadastrar a mesma senha duas vezes produz valores diferentes. Nos testes desta aula,
   Ana e Bia foram cadastradas ambas com `1234`, e o banco guardou
   `$2a$10$nRle1Jxk...` e `$2a$10$ZMdTw11H...`. Isso não é erro, e tem uma consequência direta
   na seção 2.4.

> **O que esta aula não discute.** A diferença entre cifrar e aplicar hash, as propriedades
> formais de uma função de hash, o papel do salt, o fator de custo e a comparação entre
> famílias de algoritmos são assunto da disciplina de criptografia, e é lá que eles devem ser
> estudados. Aqui tratamos do que é específico da nossa disciplina: onde essa ferramenta entra
> em uma aplicação web, qual camada a aplica e que erros de uso ela permite cometer.

### 2.2 A dependência, e por que não o starter completo

O BCrypt do ecossistema Spring vive na biblioteca `spring-security-crypto`, que é a parte
puramente criptográfica do Spring Security:

```xml
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-crypto</artifactId>
		</dependency>
```

Note que **não** estamos adicionando `spring-boot-starter-security`. Essa decisão é
pedagógica e vale explicar. O starter completo traria uma cadeia de filtros que protege todas
as rotas automaticamente, com uma tela de login própria, e passaria a cuidar da autenticação,
da sessão e da autorização sem que fique visível onde cada coisa acontece. É a escolha correta
em um projeto profissional, e será assunto de uma aula futura. Hoje, o objetivo é entender o
mecanismo, e para isso precisamos construí-lo: a biblioteca `spring-security-crypto` nos dá
apenas o algoritmo de hash, bem testado, e deixa o resto por nossa conta.

A dependência não tem `<version>`, pelo mesmo motivo das demais: o `spring-boot-starter-parent`
já declara a versão compatível.

### 2.3 O bean `PasswordEncoder`

```java
@Configuration
public class SegurancaConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Esta classe pequena introduz um recurso do Spring que o projeto ainda não tinha usado. Até
agora, todos os beans injetados vinham de classes nossas anotadas com `@Service` ou
`@RestController`, e o Spring as encontrava pela varredura de pacotes. `BCryptPasswordEncoder`
é uma classe de biblioteca, que não tem anotação nossa e que não vamos editar. A anotação
`@Bean` sobre um método de uma classe `@Configuration` resolve esse caso: ela diz ao Spring
"execute este método na subida e registre o objeto devolvido como bean disponível para
injeção".

Observe também o tipo declarado no retorno. O método devolve `PasswordEncoder`, a interface, e
não `BCryptPasswordEncoder`, a implementação. Isso faz com que o `AuthService` e o
`UsuarioService` dependam apenas do contrato "algo que sabe gerar e conferir hash". Trocar o
BCrypt por outro algoritmo no futuro é mudar esta única linha, e nenhum `service` precisa saber.
É a mesma ideia que a aula 5 aplicou à injeção por construtor, agora em outro nível.

### 2.4 `encode` e `matches`

A interface `PasswordEncoder` tem dois métodos, e a distinção entre eles é o coração desta
seção.

| Método | Entrada | Saída | Quando é chamado |
|---|---|---|---|
| `encode(senha)` | A senha em texto puro. | A `String` de 60 caracteres, com salt novo. | No cadastro e na troca de senha. |
| `matches(senha, hashGuardado)` | A senha em texto puro e o hash do banco. | `boolean`. | No login. |

A pergunta que todo estudante faz neste ponto é por que existe um `matches`, em vez de
simplesmente aplicar `encode` na senha recebida e comparar o resultado com o que está no banco,
com o `equals` da aula 10. A resposta está na seção 2.1: como `encode` sorteia um salt novo a
cada chamada, o hash que ele produz agora **nunca** será igual ao que foi gravado no cadastro.
Comparar dois `encode` com `equals` produziria um sistema em que ninguém consegue entrar, e o
defeito seria dificílimo de diagnosticar, porque o código parece certo.

O que o `matches` faz é o que a seção 2.1 antecipou: ele lê o salt de dentro do hash guardado,
refaz o cálculo com aquele salt específico sobre a senha recebida, e compara os resultados. O
hash guardado é, ao mesmo tempo, o dado e a receita de como verificá-lo.

O diagrama abaixo mostra os dois caminhos, o do cadastro e o do login, lado a lado. Ele também
está em [`aulas/11_auth_hash_token_senha_fluxo.mmd`](11_auth_hash_token_senha_fluxo.mmd).

```mermaid
flowchart TD
    subgraph CAD["Cadastro: POST /usuarios"]
        A([Cliente envia senha em texto puro]) --> B["UsuarioService.criar"]
        B --> C["passwordEncoder.encode(senha)"]
        C --> D["BCrypt sorteia um salt aleatório<br/>e aplica a função lenta de propósito"]
        D --> E["String de 60 caracteres<br/>com o salt embutido"]
        E --> F[("coluna usuario.senha_hash")]
        A --> G["a senha em texto puro existe<br/>apenas na memória do servidor,<br/>durante esta requisição"]
    end

    subgraph LOG["Login: POST /auth/login"]
        H([Cliente envia senha em texto puro]) --> I["AuthService.autenticar"]
        I --> J["findByEmail"]
        J --> K[("lê usuario.senha_hash")]
        K --> L["passwordEncoder.matches(senha, senhaHash)"]
        L --> M["extrai o salt de dentro do hash armazenado"]
        M --> N["aplica a mesma função com aquele salt<br/>sobre a senha recebida"]
        N --> O{"os dois hashes<br/>são iguais?"}
        O -- não --> P[/"401<br/>email ou senha inválidos"/]
        O -- sim --> Q[/"emite token e responde 200"/]
    end

    R["O caminho inverso não existe:<br/>de senha_hash não se obtém a senha.<br/>Nem para o invasor, nem para nós."]
    E -.-> R

    classDef falha fill:#f8d7da,stroke:#842029,color:#000
    classDef ok fill:#d1e7dd,stroke:#0f5132,color:#000
    classDef nota fill:#fff3cd,stroke:#664d03,color:#000
    class P falha
    class Q,E ok
    class G,R nota
```

Vale observar no diagrama a caixa amarela da esquerda. A senha em texto puro continua
existindo: ela é digitada, trafega na requisição e fica na memória do servidor durante o
processamento. O que deixa de existir é a senha **armazenada**. É por isso que as duas defesas
da aula 10 continuam valendo e ganham mais sentido agora: a credencial vai no corpo de um
`POST`, nunca na URL, e o `LoginResponseDTO` jamais a devolve.

### 2.5 A entidade passa a dizer a verdade

```java
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;
```

O atributo mudou de nome, de `senha` para `senhaHash`, e o `@Column(name = ...)` leva a mudança
para a coluna do banco. Isso não é cosmético. Um campo chamado `senha` que guarda um hash
convida ao erro: na próxima vez que alguém escrever `usuario.getSenha()`, vai esperar uma senha
e receber um hash. O nome do campo é documentação, e documentação errada é pior do que nenhuma.

A mudança atravessa a classe inteira, inclusive o construtor e os métodos de acesso, que agora
são `getSenhaHash` e `setSenhaHash`. E repare que `UsuarioRequestDTO`, o DTO de entrada,
**continua** com um campo chamado `senha`, corretamente: o que o cliente envia é uma senha de
verdade. A conversão de uma coisa na outra acontece no `service`, e é justamente a fronteira
entre o DTO e a entidade que a aula 5 estabeleceu.

### 2.6 Mais um encontro com o limite do `ddl-auto`

Esta é a terceira aula consecutiva em que `spring.jpa.hibernate.ddl-auto=update` nos dá uma
lição, e desta vez a mais severa. Ao subir a aplicação, o Hibernate vê uma coluna nova chamada
`senha_hash` e a cria. Ele **não** vê uma renomeação, porque não existe nada no mapeamento que
diga "esta coluna se chamava `senha` antes". O resultado é uma tabela com duas colunas:

| Coluna | Estado depois de subir a aplicação |
|---|---|
| `senha` | Continua existindo, com todas as senhas antigas legíveis, e nunca mais é lida pela aplicação. |
| `senha_hash` | Existe, vazia para os usuários antigos. |

Ou seja, a aplicação passou a ser segura e o banco continuou inseguro. Pior: a coluna esquecida
é mais perigosa do que a original, porque ninguém mais olha para ela. A conclusão é a mesma das
aulas 8 e 10, agora com consequência prática imediata: `ddl-auto` não é ferramenta de migração.
Os dois passos abaixo são decisão humana e precisam ser executados à mão.

```sql
ALTER TABLE usuario DROP COLUMN senha;
DELETE FROM usuario;
```

O `DELETE` é necessário porque os usuários antigos ficaram sem hash e, portanto, não conseguem
mais entrar. Em um sistema em produção não se apagam os usuários: o caminho seria um processo de
migração que, no próximo login bem sucedido, calcula o hash a partir da senha recebida e
descarta a coluna antiga. Aqui, com dados de teste, apagar e cadastrar de novo é honesto e
suficiente. Em uma turma, este é o momento de rodar a consulta antes e depois:

```sql
SELECT id, nome, email, senha_hash FROM usuario;
```

### 2.7 Onde o hash é gerado

No `UsuarioService`, no único lugar por onde toda senha entra no sistema:

```java
    public UsuarioResponseDTO criar(UsuarioRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            return null;
        }
        ...
        String senhaHash = passwordEncoder.encode(request.senha());

        Usuario novo = new Usuario(0, request.nome(), request.email(), senhaHash, casa);
        Usuario salvo = usuarioRepository.save(novo);
        return toResponse(salvo);
    }
```

O mesmo acontece em `atualizar`. No `atualizarParcial`, que implementa o `PATCH`, há um detalhe
que merece olhar:

```java
        String novaSenhaHash = existente.getSenhaHash();
        if (request.senha() != null) {
            novaSenhaHash = passwordEncoder.encode(request.senha());
        }
```

A versão da aula 10 era uma linha só, com operador ternário. Agora precisa de um `if`, porque
as duas pontas são de naturezas diferentes: se o cliente mandou uma senha nova, ela precisa ser
transformada em hash; se não mandou, o que se preserva é o hash que já estava no banco, que não
pode passar pelo `encode` de novo. Aplicar hash em um hash geraria um valor com o qual nenhuma
senha jamais vai casar, e o usuário perderia o acesso silenciosamente, sem nenhum erro aparecer
em lugar algum.

E por que o hash é gerado no `service`, e não no `controller` ou na entidade? Pelo critério da
aula 5: o `controller` cuida do protocolo HTTP, o `service` cuida de regra de negócio, e "senha
nunca é armazenada em texto puro" é uma regra de negócio. Colocá-la no `service` garante que
ela valha para todas as portas de entrada, inclusive as que ainda não existem.

---

## 3. Do login à sessão: o conceito de token

A senha está protegida. Agora precisamos resolver o esquecimento.

### 3.1 O problema, enunciado com precisão

O HTTP é um protocolo **sem estado**: cada requisição é processada de forma independente, e o
servidor não guarda nada entre uma e outra por conta própria. Essa não é uma limitação
acidental, é uma decisão de projeto que permite à web escalar, porque qualquer servidor pode
atender qualquer requisição.

A consequência é que o `200 OK` do login não deixa marca nenhuma. A próxima requisição chega
anônima, e o servidor precisa receber **com ela** alguma coisa que prove quem está falando.
Existem três formas de fazer isso.

### 3.2 As três formas, comparadas

| Estratégia | Como funciona | Problema |
|---|---|---|
| **Reenviar a senha** | O cliente guarda a senha e a manda em cada requisição. | A senha passa a trafegar dezenas de vezes por sessão, e precisa ficar guardada no cliente. Multiplica as oportunidades de vazamento e não tem como ser revogada sem trocar a senha. |
| **Token opaco** | O servidor emite um valor aleatório sem significado, guarda a associação com o usuário, e o cliente o reapresenta. | Exige uma consulta ao banco em cada requisição. É a escolha desta aula. |
| **Token autocontido** (`JWT`) | O servidor emite um token que **contém** os dados do usuário, assinado criptograficamente. Não guarda nada. | Não exige consulta, mas não dá para invalidar antes de expirar, justamente porque o servidor não guarda nada. |

A palavra **opaco** é o ponto. O valor `143b1896-9d92-4c71-8696-ccc7ab720824` não diz nada
sobre quem é o usuário: ele é só uma etiqueta, e a informação está na linha da tabela que
aquela etiqueta aponta. É a mesma lógica da ficha do guarda-volumes: o número na ficha não
descreve a mala, apenas permite encontrá-la, e quem acha a ficha no chão não descobre o que tem
dentro.

A escolha do token opaco nesta aula é deliberada e tem dois motivos. O primeiro é que a sessão
fica visível: ela é uma linha em uma tabela que se pode consultar com `SELECT`, e isso torna o
mecanismo inspecionável para quem está aprendendo. O segundo é que o `logout` funciona de
verdade, apagando a linha, o que com `JWT` exige estrutura adicional.

### 3.3 A entidade `Token`

```java
@Entity
@Table(name = "token")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private String valor;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @Column(nullable = false)
    private LocalDateTime expiraEm;
```

Nenhuma anotação nova em relação à aula 8. O que há de novo é o raciocínio por trás de cada
campo.

| Campo | Por que ele existe |
|---|---|
| `valor` | O que o cliente guarda e reapresenta. `unique = true` porque é por ele que se busca, pela mesma razão da seção 2 da aula 10. |
| `usuario` | A associação que dá sentido ao token. Sem ela o valor não aponta para ninguém. É `nullable = false`: um token sem dono é um registro impossível. |
| `criadoEm` | Não é usado pela lógica de hoje, e existe para auditoria: responde "desde quando esta sessão está aberta". |
| `expiraEm` | O que impede um token roubado de valer para sempre. |

A entidade tem ainda um método que não é um simples acessório:

```java
    public boolean estaExpirado() {
        return expiraEm.isBefore(LocalDateTime.now());
    }
```

Este é o primeiro **comportamento** em uma classe do pacote `model/` deste projeto. Até aqui as
entidades eram puramente dados, com construtor e métodos de acesso. A pergunta "este token
ainda vale?" é respondida inteiramente pelos dados do próprio token, então colocá-la na
entidade mantém o dado e a regra que o interpreta no mesmo lugar. Se essa lógica estivesse
espalhada pelos `services`, cada um escreveria sua versão da comparação de datas, e uma delas
estaria errada.

### 3.4 Como o valor é sorteado

```java
    private Token emitirToken(Usuario usuario) {
        LocalDateTime agora = LocalDateTime.now();

        Token token = new Token(
                0,
                UUID.randomUUID().toString(),
                usuario,
                agora,
                agora.plusHours(HORAS_DE_VALIDADE)
        );

        return tokenRepository.save(token);
    }
```

`UUID.randomUUID()` produz um identificador de 128 bits a partir de uma fonte aleatória
criptograficamente segura. Duas propriedades importam:

1. **Imprevisibilidade.** Ver um token não ajuda a adivinhar o próximo. Um contador sequencial
   seria desastroso aqui: quem recebesse o token 41 tentaria o 42 e entraria na conta de outra
   pessoa. Note a diferença de propósito em relação ao `@GeneratedValue` do `id`, que é
   sequencial justamente porque não precisa ser secreto.
2. **Espaço de busca.** Com 128 bits, tentar adivinhar um token válido por força bruta é
   inviável, ainda que a rota não tenha nenhuma proteção contra tentativas repetidas.

O método é `private`, e isso é intencional: emitir token não é um serviço que alguém de fora
possa pedir, é uma consequência interna de um login bem sucedido.

### 3.5 Expiração

```java
    private static final int HORAS_DE_VALIDADE = 8;
```

A validade é um prazo escolhido, e a escolha é um equilíbrio: prazo curto incomoda o usuário,
que precisa entrar de novo; prazo longo aumenta a janela em que um token roubado continua
servindo. Oito horas é, aproximadamente, um dia de uso.

O que importa aqui é entender **por que** o prazo existe. O token é uma credencial de posse
ou *bearer token*, expressão que a RFC 6750 usa e que significa exatamente isto: quem o porta é
tratado como o dono, sem nenhuma outra prova. Se ele não expirasse, um token copiado uma vez
daria acesso permanente, e a vítima não teria como saber nem como impedir.

### 3.6 O `AuthService` completo

```java
    public LoginResponseDTO autenticar(LoginRequestDTO request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.email());

        if (usuarioOpt.isEmpty()) {
            return null;
        }

        Usuario usuario = usuarioOpt.get();

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            return null;
        }

        Token token = emitirToken(usuario);
        ...
    }
```

Compare com a versão da aula 10. A estrutura é idêntica, as duas condições de falha continuam
devolvendo o mesmo `null` pelo motivo da seção 6.3 daquela aula, e exatamente duas coisas
mudaram: a linha do `equals` virou `matches`, e o sucesso agora emite um token. A linha que a
aula 10 pediu para guardar deixou de existir.

Ao lado de `autenticar`, o `service` ganhou duas operações, que são o que torna a sessão
utilizável:

```java
    public Usuario resolverUsuario(String valorDoToken) {
        if (valorDoToken == null) {
            return null;
        }

        Optional<Token> tokenOpt = tokenRepository.findByValor(valorDoToken);

        if (tokenOpt.isEmpty()) {
            return null;
        }

        Token token = tokenOpt.get();

        if (token.estaExpirado()) {
            tokenRepository.delete(token);
            return null;
        }

        return token.getUsuario();
    }
```

Leia a assinatura com atenção, porque ela é o resumo da aula: entra uma `String` sem
significado, sai um `Usuario`. Esse método é a tradução de "etiqueta" para "pessoa", e é o que
permite que todo o resto do sistema volte a trabalhar com objetos de domínio.

Observe também que o token expirado é **apagado** no momento em que é encontrado. É uma
faxina oportunista: não resolve o problema das linhas que ninguém mais consulta, mas evita
acumular tokens mortos que continuam sendo reapresentados.

```java
    public void encerrarSessao(String valorDoToken) {
        Optional<Token> tokenOpt = tokenRepository.findByValor(valorDoToken);

        if (tokenOpt.isPresent()) {
            tokenRepository.delete(tokenOpt.get());
        }
    }
```

Este método de três linhas é o `logout`, e ele merece uma observação. Sair do sistema, do ponto
de vista do servidor, é apagar uma linha de tabela. Não existe nada mais a desfazer, porque
nunca houve nada além daquela linha. Em um esquema com `JWT` esta operação simplesmente não
teria como existir, o que mostra que a escolha da seção 3.2 tem consequências concretas.

### 3.7 Como o token viaja: `Authorization: Bearer`

O token não vai na URL, não vai no corpo e não vai em um cabeçalho inventado por nós. Ele vai
no cabeçalho padronizado para isso:

```
Authorization: Bearer 143b1896-9d92-4c71-8696-ccc7ab720824
```

| Alternativa | Por que foi descartada |
|---|---|
| Na URL, como `?token=...` | A URL é registrada no histórico do navegador, nos logs de acesso do servidor e no cabeçalho `Referer` enviado a terceiros. É o mesmo argumento da seção 6.1 da aula 10, e vale para o token com a mesma força que valia para a senha. |
| No corpo da requisição | Requisições `GET` e `DELETE` não têm corpo. A identidade precisa viajar de uma forma que funcione em todos os métodos. |
| Em um cabeçalho próprio | Funcionaria, mas perderia a interoperabilidade: clientes HTTP, bibliotecas, proxies e ferramentas como o Swagger UI já entendem `Authorization`. |

A palavra `Bearer` é o **esquema** de autenticação, definido na RFC 6750, e informa ao servidor
como interpretar o que vem depois. O prefixo não é decorativo: é por ele que o nosso código
distingue um cabeçalho que lhe diz respeito de um `Authorization: Basic ...`, que seguiria outra
regra.

---

## 4. Protegendo as rotas

Temos o hash, temos o token e temos como traduzir token em usuário. Falta aplicar isso nas rotas
que precisam de proteção.

Para orientar a leitura desta seção, vale adiantar a estrutura do que vamos construir. Antes de
o método do `controller` executar, o servidor precisa responder três perguntas, **nesta ordem**:

1. Esta requisição precisa de token?
2. Se precisa, o token apresentado vale?
3. Se vale, de quem ele é, e como passar essa informação adiante?

Uma única peça responde às três. As subseções 4.4, 4.5 e 4.6 tratam de uma pergunta cada, na
mesma ordem em que o código as faz.

### 4.1 Onde a verificação deve morar

Antes da peça, a decisão de projeto: em que ponto da aplicação essa verificação acontece.

| Lugar | Como seria | Por que não |
|---|---|---|
| Em cada método de `controller` | Uma chamada a `resolverUsuario` no início de cada um dos vinte e tantos métodos. | A regra ficaria repetida vinte vezes, e proteger a rota nova de amanhã dependeria de alguém lembrar. Esquecer é silencioso: a rota fica aberta e nenhum teste reclama. |
| Na camada de `service` | Cada `service` receberia o token e o verificaria. | O `service` passaria a conhecer um detalhe de protocolo HTTP, quebrando a separação de camadas da aula 5. Token é assunto de transporte. |
| Em um `Filter` do Jakarta Servlet | Uma peça que roda antes do Spring MVC. | Funciona, mas é cedo demais: nesse ponto o Spring ainda não decidiu qual método vai atender, e o filtro trabalha com texto de URL em vez de rotas. |
| Em um `HandlerInterceptor` | Uma peça do Spring MVC que roda entre o mapeamento da rota e a execução do método. | É a escolha desta aula. |

O critério é o de sempre: a verificação de token é uma regra **transversal**, que vale para
muitas rotas e não pertence a nenhuma delas em particular. Regras transversais ficam em um lugar
só, atravessando as requisições, em vez de serem copiadas em cada ponto.

### 4.2 A peça e o momento: `HandlerInterceptor`

Um interceptor é uma classe que o Spring MVC chama no meio do processamento de uma requisição. A
interface `HandlerInterceptor` oferece três momentos de entrada, e vamos usar apenas o primeiro:

| Método | Quando roda | Para que serve |
|---|---|---|
| `preHandle` | Depois de o Spring escolher o método do `controller` e **antes** de executá-lo. | Decidir se a requisição segue adiante. É o nosso caso. |
| `postHandle` | Depois do `controller`, antes de a resposta ser escrita. | Acrescentar algo à resposta. |
| `afterCompletion` | Depois de a resposta ter sido enviada. | Registrar tempo de execução, liberar recursos. |

O que torna o `preHandle` capaz de proteger uma rota é o seu tipo de retorno, que é `boolean`:

| O `preHandle` devolve | O que acontece |
|---|---|
| `true` | A requisição segue, e o método do `controller` executa normalmente. |
| `false` | **A requisição para ali.** O `controller` não é chamado, e o cliente recebe o que o interceptor já tiver escrito na resposta. |

Guarde essa segunda linha, porque é nela que mora a proteção: o nosso interceptor vai devolver
`false` quando não houver token válido, e nenhuma linha de nenhum `controller` chegará a rodar.

### 4.3 O `preHandle` do `casalapp`

Este é o método completo. Leia uma vez sem se preocupar com os detalhes: cada bloco é assunto de
uma subseção adiante.

```java
@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_USUARIO_LOGADO = "usuarioLogado";

    private static final String PREFIXO_BEARER = "Bearer ";

    private final AuthService authService;

    public AutenticacaoInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (dispensaToken(request)) {
            return true;
        }

        String token = extrairToken(request.getHeader("Authorization"));
        Usuario usuario = authService.resolverUsuario(token);

        if (usuario == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"mensagem\":\"token ausente, inválido ou expirado\"}");
            return false;
        }

        request.setAttribute(ATRIBUTO_USUARIO_LOGADO, usuario);
        return true;
    }
```

O método tem exatamente a forma das três perguntas anunciadas no início da seção, e é assim que
convém lê-lo:

| Trecho | Pergunta que responde | Subseção |
|---|---|---|
| `if (dispensaToken(request))` | Esta requisição precisa de token? | 4.4 |
| `extrairToken` e `resolverUsuario`, até o `return false` | O token apresentado vale? | 4.5 |
| `request.setAttribute(...)` | De quem é o token, e como passar isso adiante? | 4.6 |

Note também que a classe é um `@Component` e recebe o `AuthService` por construtor, como
qualquer outro bean do projeto. Um interceptor não é uma peça de infraestrutura isolada: ele
participa da injeção de dependência igual ao resto.

### 4.4 Primeira pergunta: esta requisição precisa de token?

```java
    private boolean dispensaToken(HttpServletRequest request) {
        boolean preflightCors = "OPTIONS".equals(request.getMethod());
        boolean cadastroDeNovoUsuario = "POST".equals(request.getMethod())
                && "/usuarios".equals(request.getRequestURI());

        return preflightCors || cadastroDeNovoUsuario;
    }
```

São duas exceções, de naturezas completamente diferentes, e cada uma ensina algo.

**O preflight do CORS.** Como o frontend passou a enviar o cabeçalho `Authorization`, que não
está na lista de cabeçalhos considerados simples, o navegador deixa de disparar a requisição
diretamente. Antes dela, ele envia uma requisição `OPTIONS` perguntando ao servidor se aquela
combinação de origem, método e cabeçalho é permitida. Essa pergunta chega **sem** o cabeçalho
`Authorization`, porque ela ainda não é a requisição de verdade.

O efeito de não tratar esse caso é instrutivo, e foi observado durante a escrita desta aula: o
interceptor responde `401` ao `OPTIONS`, o navegador conclui que a requisição não é permitida, e
**nenhuma** chamada do frontend chega ao servidor. O curioso é que, pelo `curl`, tudo funciona,
porque o `curl` não faz preflight. É exatamente o tipo de defeito que só aparece no navegador, e
a lição é que a configuração de CORS e a de autenticação precisam ser pensadas juntas.

**O cadastro de novo usuário.** Aqui a razão não é técnica, é lógica. Quem está se cadastrando
ainda não tem conta, logo não pode ter token. Se `POST /usuarios` exigisse autenticação, o
sistema ficaria em um impasse: seria impossível criar o primeiro usuário, e cada novo usuário
dependeria de alguém já cadastrado. Note que a exceção é bem estreita, e isso é deliberado: ela
exige que o método seja `POST` **e** que o caminho seja exatamente `/usuarios`. A listagem
`GET /usuarios` e a exclusão `DELETE /usuarios/7` continuam protegidas.

### 4.5 Segunda pergunta: o token apresentado vale?

Esta é a parte central, e ela se apoia inteiramente no que a seção 3.6 já construiu. O
interceptor não sabe nada sobre tabelas nem sobre prazos: ele lê um cabeçalho e pergunta ao
`AuthService`.

```java
        String token = extrairToken(request.getHeader("Authorization"));
        Usuario usuario = authService.resolverUsuario(token);
```

A leitura do cabeçalho é uma peça pequena e vale olhá-la, porque ela evita um defeito real:

```java
    private String extrairToken(String cabecalho) {
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO_BEARER)) {
            return null;
        }
        return cabecalho.substring(PREFIXO_BEARER.length());
    }
```

O cabeçalho ausente e o cabeçalho com esquema errado são tratados **antes** de qualquer tentativa
de `substring`, que nesses casos lançaria exceção e produziria um `500` onde cabe um `401`.
Lembre da seção 3.7: é o prefixo `Bearer ` que distingue um cabeçalho que nos diz respeito de um
`Authorization: Basic ...`, que seguiria outra regra.

Quando `resolverUsuario` devolve `null`, por qualquer um dos três motivos possíveis (cabeçalho
ausente, token inexistente ou token expirado), o interceptor recusa a requisição:

```java
        if (usuario == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"mensagem\":\"token ausente, inválido ou expirado\"}");
            return false;
        }
```

Três observações sobre essas quatro linhas, que são diferentes de tudo o que o projeto escreveu
até aqui.

1. **O status e o corpo são montados à mão, sem `ResponseEntity`.** Isso não é desleixo: como o
   método devolve `false`, nenhum `controller` vai executar, e portanto não existe `controller`
   para devolver um `ResponseEntity`. Quem escreve a resposta é o próprio interceptor.
2. **O JSON imita o formato do `ErroResponseDTO` de propósito.** O cliente passa a ver sempre a
   mesma estrutura de erro, venha ela de um `controller` ou do interceptor.
3. **O `charset=UTF-8` não é detalhe estético.** Sem ele, a palavra "inválido" chega corrompida
   ao cliente, porque o contêiner adota a codificação padrão da plataforma.

Repare, por fim, que a mensagem é uma só para os três motivos de recusa. É a mesma disciplina da
seção 6.3 da aula 10: dizer ao cliente que o token "existe mas expirou", em vez de apenas
recusá-lo, é informação que só interessa a quem está testando tokens alheios.

### 4.6 Terceira pergunta: de quem é o token

```java
        request.setAttribute(ATRIBUTO_USUARIO_LOGADO, usuario);
        return true;
```

Esta é a linha mais fácil de passar batido e a mais importante da seção, porque é ela que
transforma o interceptor de porteiro em algo útil para o resto da aplicação.

Repare no desperdício que existiria sem ela. Para decidir se a requisição passa, o interceptor
precisou consultar o banco e obter a entidade `Usuario` inteira. Se ele apenas devolvesse `true`
e descartasse esse objeto, cada `controller` que quisesse saber quem está logado teria de
consultar o banco **de novo**, repetindo um trabalho que acabou de ser feito.

Um `HttpServletRequest` carrega, além do método, da URL, dos cabeçalhos e do corpo, um mapa de
atributos que vive **durante aquela requisição**. Qualquer peça que participe do processamento
pode escrever nele, e qualquer peça seguinte pode ler. É um espaço de rascunho com tempo de vida
exato: nasce quando a requisição chega e morre quando a resposta é enviada.

Vale reparar que o nome do atributo é a constante `public static final` declarada no topo da
classe. Se fosse uma `String` literal repetida em cada `controller`, um erro de digitação em um
deles produziria um `null` em tempo de execução, sem aviso nenhum do compilador. Com a constante,
quem escreve e quem lê usam necessariamente o mesmo nome.

Como o `controller` pega esse valor de volta é o assunto da seção 5.

### 4.7 O registro: sem ele, nada acontece

Um interceptor não protege nada enquanto não for registrado. Quem faz isso é o `WebConfig`, a
mesma classe que a aula 9 criou para o CORS:

```java
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacaoInterceptor)
                .addPathPatterns(
                        "/casas/**",
                        "/categorias/**",
                        "/transacoes/**",
                        "/usuarios/**",
                        "/auth/eu",
                        "/auth/logout");
    }
```

Vale separar com clareza os dois papéis, porque é aqui que a leitura costuma se embaralhar:

| Peça | Responsabilidade |
|---|---|
| `WebConfig`, com `addPathPatterns` | **Quais** rotas passam pelo interceptor. |
| `AutenticacaoInterceptor`, no `preHandle` | **O que** acontece com as rotas que passam por ele. |

Uma rota que não casa com nenhum padrão da lista nunca chega ao `preHandle`: para ela, o
interceptor simplesmente não existe. Esta lista é, literalmente, a política de segurança da
aplicação, e por isso é importante que esteja em um único lugar legível. Note o que **não** está
nela e por quê:

| Rota fora da lista | Motivo |
|---|---|
| `POST /auth/login` | Quem pede um token ainda não tem um. Exigir token aqui tornaria o sistema inacessível. |
| `/swagger-ui/**` e `/v3/api-docs/**` | É documentação. Em um ambiente de produção, o acesso a ela se restringe de outra maneira. |

Surge daí uma pergunta natural: se `POST /auth/login` fica de fora pela lista do `WebConfig`,
por que `POST /usuarios` precisou da exceção dentro do interceptor, na seção 4.4? Porque a lista
do `WebConfig` trabalha com **caminhos**, e não com métodos HTTP. Deixar `/usuarios` fora da
lista abriria também o `GET /usuarios`, que lista nome e email de todo mundo. Como a exceção que
queremos vale para um método só, ela não cabe no `WebConfig` e desce para o `preHandle`.

Por fim, a decisão de **listar as rotas protegidas**, em vez de proteger `/**` e abrir exceções,
tem uma consequência que vale discutir honestamente em sala: **uma rota nova nasce
desprotegida**. Se amanhã alguém criar um `OrcamentoController`, ele responderá a qualquer um até
que a linha correspondente apareça aqui. A política inversa, chamada *deny by default*, é mais
segura por construção, e é a que o Spring Security adota. A opção desta aula troca essa garantia
por clareza didática, e saber que a troca existe é parte da lição.

### 4.8 O CORS acompanha

```java
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5500", "http://127.0.0.1:5500")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization");
```

Duas adições à configuração da aula 9: `OPTIONS` entre os métodos e `Authorization` entre os
cabeçalhos permitidos. Sem a segunda, o navegador recusaria a requisição mesmo que o servidor
estivesse disposto a atendê-la, pois o preflight responderia que aquele cabeçalho não é aceito.
Vale enfatizar o que isso revela sobre o CORS: ele é uma restrição aplicada **pelo navegador**,
com base na resposta do servidor, e não uma proteção do servidor contra clientes.

### 4.9 O caminho de decisão, em um diagrama

O diagrama abaixo reúne, em um único caminho, a lista do `WebConfig` da seção 4.7 e as três
perguntas do `preHandle`. Ele também está em
[`aulas/11_auth_hash_token_fluxo.mmd`](11_auth_hash_token_fluxo.mmd).

```mermaid
flowchart TD
    A([Chega uma requisição HTTP]) --> B{"A URL casa com algum<br/>addPathPatterns do WebConfig?"}
    B -- não --> C["Nenhum interceptor é executado.<br/>Ex.: POST /auth/login, Swagger UI"]
    C --> Z["Controller executa"]

    B -- sim --> D["AutenticacaoInterceptor.preHandle"]
    D --> E{"Método é OPTIONS?<br/>preflight do CORS"}
    E -- sim --> Z

    E -- não --> F{"É POST /usuarios?<br/>cadastro de quem ainda não tem conta"}
    F -- sim --> Z

    F -- não --> G["Lê o cabeçalho Authorization"]
    G --> H{"Começa com<br/>o prefixo Bearer?"}
    H -- não --> X[/"401<br/>token ausente, inválido ou expirado"/]

    H -- sim --> I["authService.resolverUsuario<br/>tokenRepository.findByValor"]
    I --> J{"Existe uma linha<br/>na tabela token?"}
    J -- não --> X

    J -- sim --> K{"expiraEm já passou?"}
    K -- sim --> L["Apaga a linha do token"]
    L --> X

    K -- não --> M["request.setAttribute<br/>usuarioLogado = token.getUsuario()"]
    M --> N["preHandle devolve true"]
    N --> Z

    Z --> O{"O método do controller declara<br/>@RequestAttribute usuarioLogado?"}
    O -- sim --> P["Recebe a entidade Usuario<br/>e decide o que mostrar"]
    O -- não --> Q["Ignora quem está logado<br/>e responde o mesmo para todos"]

    P --> R([Resposta])
    Q --> R

    classDef falha fill:#f8d7da,stroke:#842029,color:#000
    classDef ok fill:#d1e7dd,stroke:#0f5132,color:#000
    classDef aberto fill:#fff3cd,stroke:#664d03,color:#000,stroke-dasharray: 5 5
    class X,L falha
    class M,N,P ok
    class Q aberto
```

Compare este diagrama com o da seção 1.1 da aula 10. Lá, um retângulo amarelo solto no canto
representava as rotas que não se conectavam a nada. Aqui, o amarelo mudou de lugar e de
significado: ele agora marca os `controllers` que **podem** saber quem está logado e escolhem
ignorar. A porta está fechada; o que resta decidir é quanto cada sala lá dentro diferencia um
visitante do outro.

### 4.10 E não existe uma anotação que faça isso?

Essa é a pergunta certa a se fazer neste ponto, e a resposta tem uma ressalva que vale guardar:
**anotação é declaração, não implementação**. Uma anotação sozinha não verifica nada. Alguém
precisa lê-la em tempo de execução, e esse alguém é um interceptor, um filtro ou um aspecto, isto
é, uma peça do tipo que acabamos de escrever.

Há dois caminhos, e eles respondem a necessidades diferentes.

| Caminho | Como seria | O que ainda é preciso escrever |
|---|---|---|
| **Spring Security**, com o starter completo | `@EnableMethodSecurity` na configuração e `@PreAuthorize("isAuthenticated()")` sobre os métodos, ou a política declarada de uma vez na cadeia de filtros. `@AuthenticationPrincipal` substitui o que a seção 5.1 vai mostrar. | Para um token opaco nosso, um filtro que traduza o token em um objeto de autenticação do framework. A peça não desaparece: muda de nome e passa a conviver com a cadeia inteira. |
| **Uma anotação própria**, sem Spring Security | Criar, por exemplo, `@Publico`, e anotar os métodos que dispensam token. | O mesmo interceptor, que passaria a ler a anotação do método em vez de comparar caminhos. O parâmetro `Object handler` do `preHandle`, que esta aula ignora, é justamente o método escolhido pelo Spring, e por ele se chega às anotações. |

O segundo caminho resolveria de forma elegante a fraqueza admitida no final da seção 4.7:
bastaria registrar o interceptor em `/**`, obtendo *deny by default*, e declarar cada exceção na
própria rota, ao lado do `@PostMapping`, em vez de em uma lista distante. No mesmo espírito, um
`HandlerMethodArgumentResolver` permitiria trocar o parâmetro verboso da seção 5.1 por um
`@UsuarioLogado Usuario`.

Nada disso foi feito aqui, e por um motivo didático: construir primeiro o mecanismo e só depois
escondê-lo atrás de uma anotação é a ordem que permite entender o que a anotação faz. Quem
começa pela anotação aprende a usá-la e não sabe diagnosticar quando ela não funciona. O
caminho do Spring Security é assunto de uma aula futura, e o da anotação própria é um bom
exercício para quem terminar a atividade antes da hora.

---

## 5. Do interceptor ao `controller`: quem está logado

A seção 4.6 deixou a entidade `Usuario` guardada no mapa de atributos da requisição. Falta a
outra ponta: como o método do `controller` recebe esse objeto, e o que ele passa a poder fazer
com ele.

### 5.1 `@RequestAttribute`, o outro lado da ponte

Um `controller` só recebe o que declara nos parâmetros do método. Para pegar o que o interceptor
guardou, ele declara isto:

```java
    public UsuarioResponseDTO eu(
            @RequestAttribute(AutenticacaoInterceptor.ATRIBUTO_USUARIO_LOGADO) Usuario usuarioLogado) {
```

`@RequestAttribute` entra na mesma família de anotações que o projeto já conhece, e colocá-la ao
lado das outras ajuda a fixar a ideia:

| Anotação | De onde o Spring tira o valor | Origem do dado |
|---|---|---|
| `@PathVariable` | De um trecho da URL. | Cliente |
| `@RequestParam` | Da *query string*. | Cliente |
| `@RequestBody` | Do corpo da requisição, convertido de JSON. | Cliente |
| `@RequestHeader` | De um cabeçalho HTTP. | Cliente |
| `@RequestAttribute` | Do mapa de atributos da requisição. | **Servidor** |

Todas respondem à mesma pergunta, "de onde vem o valor deste parâmetro", e é isso que faz o
mecanismo parecer familiar na primeira vez. A coluna da direita é a que importa para a segurança.
As quatro primeiras trazem dados que o **cliente** enviou, e por isso são dados não confiáveis,
que precisam de validação. A última traz um dado que o **próprio servidor** produziu: o
`controller` recebe uma entidade `Usuario` em que pode confiar, porque ela não veio da
requisição, veio do banco, depois de o token ter sido conferido.

A alternativa deixa isso evidente. Se o `controller` lesse o id do usuário de um `@RequestParam`,
como em `GET /transacoes?usuarioId=7`, bastaria trocar o número na URL para agir como outra
pessoa. Confiar no atributo é seguro justamente porque o cliente não tem como escrever nele.

### 5.2 O uso mais simples: a rota que responde "quem sou eu"

```java
    @Operation(summary = "Devolve o usuário dono do token enviado no cabeçalho Authorization")
    @GetMapping("/auth/eu")
    public UsuarioResponseDTO eu(
            @RequestAttribute(AutenticacaoInterceptor.ATRIBUTO_USUARIO_LOGADO) Usuario usuarioLogado) {

        Casa casa = usuarioLogado.getCasa();
        int casaId = casa != null ? casa.getId() : 0;
        String casaNome = casa != null ? casa.getNome() : null;

        return new UsuarioResponseDTO(...);
    }
```

Esta rota não tem lógica nenhuma: ela apenas devolve, em forma de DTO, o usuário que o
interceptor já havia resolvido. Por isso serve bem como primeiro exemplo. Mas por que ela existe,
se o `LoginResponseDTO` já trouxe esses mesmos dados no login?

Porque o cliente pode ter sido fechado e reaberto. Na próxima vez que a página carregar, o
frontend terá no `localStorage` um token e alguns dados de quando o login aconteceu, e três
perguntas sem resposta: o token ainda é válido? já expirou? foi invalidado por um `logout` em
outro dispositivo? Chamar `GET /auth/eu` responde às três de uma vez, com o servidor como fonte
da verdade. Em uma falha, o próprio interceptor devolve `401`, e o frontend sabe que precisa
pedir login de novo.

Esse padrão é tão comum que a rota tem nomes consagrados em APIs reais, geralmente `/me`,
`/users/me` ou `/auth/me`. Usamos `/auth/eu` por coerência com o restante do projeto, que está em
português.

### 5.3 Autorização: a resposta que depende de quem pergunta

Até esta aula, `GET /transacoes` devolvia todas as transações de todas as casas para qualquer
pessoa. Com o usuário em mão, a rota passa a responder outra coisa:

```java
    @Operation(summary = "Lista as transações da casa do usuário autenticado")
    @GetMapping("/transacoes")
    public List<TransacaoResponseDTO> listarTransacoes(
            @RequestAttribute(AutenticacaoInterceptor.ATRIBUTO_USUARIO_LOGADO) Usuario usuarioLogado) {

        Casa casa = usuarioLogado.getCasa();

        if (casa == null) {
            return List.of();
        }

        return transacaoService.listarDaCasa(casa.getId());
    }
```

Este é o momento em que a autorização aparece, e é bom nomear o que mudou: **a resposta passou a
depender de quem perguntou**. Duas pessoas chamando a mesma URL, com o mesmo método e sem nenhum
parâmetro, recebem conteúdos diferentes. Foi exatamente o que os testes desta aula mostraram, com
duas usuárias em casas diferentes:

```
Ana, da Casa A:  [{"id":1,"descricao":"Feira","valor":120.5,...,"casaNome":"Casa A"}]
Bia, da Casa B:  [{"id":2,"descricao":"Cinema","valor":60.0,...,"casaNome":"Casa B"}]
```

Note de onde vem o filtro: do token, nunca do cliente. Se o id da casa viesse da requisição, como
em `GET /transacoes?casaId=1`, a Bia poderia simplesmente trocar o número e ler a casa da Ana. O
dado em que confiamos é o que o servidor obteve por conta própria. É a coluna da direita da
tabela da seção 5.1, agora com consequência prática.

No `repository`, o filtro é mais uma consulta derivada, do tipo que a aula 10 estudou, e aproveita
a navegação por relacionamentos da seção 3.4 daquela aula:

```java
public interface TransacaoRepository extends JpaRepository<Transacao, Integer> {

    List<Transacao> findByCategoriaCasaId(int casaId);
}
```

O nome atravessa dois níveis de associação: de `Transacao` para `categoria`, de `Categoria` para
`casa`, e finalmente o `id`. O Spring Data monta os dois `JOIN` necessários. Escrever isso à mão
era o exercício da aula 4.

Repare por fim no tratamento de `casa == null`. Um usuário sem casa recebe uma lista vazia, e não
um erro. A decisão é defensável: ele está autenticado e tem direito de ver o que é dele, que
neste caso é nada. Devolver `500` seria errado, e devolver todas as transações seria muito pior.

### 5.4 Encerrar a sessão

```java
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.encerrarSessao(authorization.substring("Bearer ".length()));
        return ResponseEntity.noContent().build();
    }
```

Três detalhes em cinco linhas.

1. **É `POST`, e não `GET`.** A rota altera o estado do servidor, apagando uma linha. Métodos
   `GET` devem ser seguros no sentido técnico do termo, isto é, não causar efeito. Um `logout` em
   `GET` seria disparado por qualquer coisa que a página carregasse, inclusive o
   pré-carregamento de links feito por alguns navegadores.
2. **Devolve `204 No Content`.** A operação deu certo e não há nada para contar.
3. **Esta rota está na lista de protegidas**, e por isso o `substring` é seguro: se o cabeçalho
   não existisse ou não começasse com `Bearer `, o interceptor já teria respondido `401` e o
   método não executaria. Note que aqui o `controller` usa `@RequestHeader`, e não
   `@RequestAttribute`: ele não precisa saber quem é o usuário, precisa do valor do token para
   apagá-lo.

### 5.5 O retrato completo, em um diagrama de sequência

O diagrama abaixo acompanha as três fases da vida de uma sessão, do login à expulsão. Ele também
está em
[`aulas/11_auth_hash_token_sequencia.mmd`](11_auth_hash_token_sequencia.mmd).

```mermaid
sequenceDiagram
    actor U as Usuário
    participant P as pages/login.js
    participant S as sessao.js
    participant LS as localStorage
    participant API as api/
    participant INT as AutenticacaoInterceptor
    participant AC as AuthController
    participant TC as TransacaoController
    participant AS as AuthService
    participant PE as BCryptPasswordEncoder
    participant TR as TokenRepository
    participant DB as MySQL

    rect rgb(232, 244, 253)
    Note over U,DB: Fase 1: login. Acontece uma vez.
    U->>P: submit do formulário
    P->>API: login(credenciais)
    API->>AC: POST /auth/login
    Note over INT: /auth/login não está em<br/>addPathPatterns: o interceptor<br/>não roda aqui
    AC->>AS: autenticar
    AS->>DB: SELECT ... FROM usuario WHERE email = ?
    DB-->>AS: usuario com senha_hash
    AS->>PE: matches(senha recebida, senha_hash)
    PE-->>AS: true
    AS->>TR: save(Token com UUID e expiraEm)
    TR->>DB: INSERT INTO token
    AS-->>AC: LoginResponseDTO com o token
    AC-->>API: 200 + JSON
    API-->>P: corpo da resposta
    P->>S: salvarSessao(login)
    S->>LS: grava casalapp.token
    P->>U: redireciona para transacoes.html
    end

    rect rgb(226, 243, 233)
    Note over U,DB: Fase 2: cada requisição seguinte.
    U->>API: abre transacoes.html
    API->>S: requisicaoAutenticada("/transacoes")
    S->>LS: lerToken()
    LS-->>S: o UUID guardado
    S->>INT: GET /transacoes<br/>Authorization: Bearer UUID
    INT->>AS: resolverUsuario(UUID)
    AS->>TR: findByValor(UUID)
    TR->>DB: SELECT ... FROM token WHERE valor = ?
    DB-->>TR: token com o usuario associado
    TR-->>AS: Token
    Note over AS: estaExpirado()?
    AS-->>INT: Usuario
    INT->>INT: request.setAttribute("usuarioLogado", usuario)
    INT->>TC: preHandle devolveu true
    Note over TC: @RequestAttribute injeta<br/>a entidade Usuario no parâmetro
    TC->>DB: SELECT ... WHERE categoria.casa_id = ?
    DB-->>TC: só as transações da casa dele
    TC-->>S: 200 + JSON
    S-->>U: tabela na tela
    end

    rect rgb(248, 215, 218)
    Note over U,DB: Fase 3: o token deixa de valer.
    U->>S: clica em Sair
    S->>AC: POST /auth/logout + Bearer
    AC->>AS: encerrarSessao
    AS->>TR: delete(token)
    TR->>DB: DELETE FROM token WHERE id = ?
    AC-->>S: 204
    S->>LS: limparSessao()
    Note over INT,DB: a partir daqui, o mesmo UUID<br/>produz 401: a linha não existe mais
    end
```

Duas comparações com o diagrama equivalente da aula 10 fecham o raciocínio. A primeira é o
desaparecimento da nota final daquele diagrama, "terminada a resposta, o servidor esquece por
completo quem acabou de entrar": o servidor agora tem uma linha na tabela `token` que sobrevive à
resposta. A segunda é a assimetria entre as fases 1 e 2. A fase 1 é caríssima, porque o BCrypt é
lento de propósito, e acontece uma vez. A fase 2 é apenas uma consulta por chave única, e
acontece em cada requisição. É para isso que serve o token: tirar a verificação caríssima do
caminho comum.

---

## 6. O frontend com sessão

O frontend da aula 10 guardava o usuário em uma variável que morria ao recarregar a página.
Agora ele precisa guardar o token entre páginas e enviá-lo em cada chamada.

### 6.1 Onde guardar o token no navegador

| Opção | Tempo de vida | Enviado automaticamente? |
|---|---|---|
| Variável JavaScript | Morre ao trocar ou recarregar a página. Era a solução da aula 10. | Não. |
| `sessionStorage` | Vive enquanto a aba estiver aberta. | Não. |
| `localStorage` | Persiste até ser apagado, inclusive depois de fechar o navegador. | Não. |
| *Cookie* | Prazo definido pelo servidor. | **Sim**, o navegador o anexa sozinho a cada requisição do domínio. |

Escolhemos o `localStorage`, por ser explícito: é o nosso código que lê o valor e o coloca no
cabeçalho, e isso mantém visível o mecanismo que estamos estudando. A escolha tem um custo que
precisa ser dito, e não escondido: `localStorage` é acessível a qualquer JavaScript da página,
de modo que uma falha de *cross site scripting* permitiria a um script injetado ler o token.
Cookies com as marcas `HttpOnly` e `Secure` são inacessíveis ao JavaScript e, por isso, mais
resistentes a esse ataque específico, em troca de ficarem expostos a outro, o *cross site
request forgery*, que exige defesa própria. Nenhuma das duas opções é gratuitamente superior, e
a escolha informada é o que se espera de quem projeta o sistema.

### 6.2 Uma camada nova: `js/sessao.js`

O front ganhou um arquivo que não é `api/` nem `pages/`, porque não é nenhuma das duas coisas:
ele não conhece rotas do servidor e não toca no DOM das telas. Ele cuida do estado da sessão no
navegador.

```js
const CHAVE_TOKEN = "casalapp.token";
const CHAVE_USUARIO = "casalapp.usuario";

function salvarSessao(login) {
    localStorage.setItem(CHAVE_TOKEN, login.token);
    localStorage.setItem(CHAVE_USUARIO, JSON.stringify({
        usuarioId: login.usuarioId,
        nome: login.nome,
        email: login.email,
        casaNome: login.casaNome
    }));
}

function lerToken() {
    return localStorage.getItem(CHAVE_TOKEN);
}

function limparSessao() {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_USUARIO);
}
```

Duas observações. O `localStorage` guarda apenas texto, e é por isso que o objeto do usuário
passa por `JSON.stringify` na ida e por `JSON.parse` na volta. E os dados do usuário gravados
ali servem somente para a tela mostrar um nome sem esperar o servidor: eles são uma cópia
possivelmente desatualizada, nunca a fonte da verdade. A fonte da verdade é o `GET /auth/eu`.

### 6.3 O cabeçalho em um lugar só

Esta é a mudança mais importante do frontend:

```js
async function requisicaoAutenticada(caminho, opcoes) {
    const configuracao = opcoes ? opcoes : {};

    configuracao.headers = {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + lerToken()
    };

    const resposta = await fetch(API_BASE_URL + caminho, configuracao);

    if (resposta.status === 401) {
        limparSessao();
        window.location.href = "login.html";
        throw new Error("sessão encerrada");
    }

    return resposta;
}
```

Com ela, a camada `api/` inteira fica mais simples do que era antes. Compare o `casaApi.js` da
aula 9 com o de hoje:

```js
async function listarCasas() {
    const resposta = await requisicaoAutenticada("/casas");
    return await resposta.json();
}

async function criarCasa(dados) {
    const resposta = await requisicaoAutenticada("/casas", {
        method: "POST",
        body: JSON.stringify(dados)
    });
    return await resposta.json();
}
```

A `API_BASE_URL`, o `Content-Type` e o `Authorization` desapareceram de cada função. O ganho não
é apenas de digitação: ele é de **garantia**. Com o cabeçalho montado em um único ponto, não
existe a possibilidade de uma função nova esquecer de enviá-lo. É o mesmo raciocínio que levou a
verificação do token para um interceptor no backend, aplicado do outro lado do fio.

O bloco do `401` merece atenção. Ele trata, em um lugar só, o caso do token que expirou ou foi
invalidado: a sessão local é descartada e o navegador vai para a tela de login. O `throw` que o
segue existe porque o redirecionamento não interrompe o código em execução, e sem ele a função
chamadora tentaria ler o JSON de uma resposta de erro.

### 6.4 Rotas privadas no frontend

Cada página protegida começa com duas linhas:

```js
exigirLogin();
configurarBarraDeSessao();
```

```js
function exigirLogin() {
    if (!lerToken()) {
        window.location.href = "login.html";
    }
}
```

É preciso ser muito claro sobre o que essas linhas são e o que não são. **Elas não protegem
nada.** Uma verificação feita no navegador é uma conveniência de interface: ela evita que o
usuário veja uma tela quebrada, cheia de erros, quando sua sessão expirou. Quem protege o dado é
o interceptor do backend, e somente ele.

A diferença fica evidente num experimento simples, que vale fazer em sala: apagar as duas
linhas, abrir a página e observar. A tela aparece, pois não há nada no navegador impedindo, mas
as tabelas vêm vazias, porque toda chamada à API volta `401`. Nenhum dado escapa. A segurança
nunca mora no cliente, porque o cliente está inteiramente nas mãos de quem o usa: o código
JavaScript pode ser alterado, e as chamadas podem ser feitas direto pelo `curl`.

### 6.5 O login e a saída

A camada `pages/login.js` mudou em dois pontos. No sucesso, ela salva a sessão e navega para
dentro do sistema, em vez de guardar o usuário em uma variável:

```js
    salvarSessao(resultado.corpo);
    window.location.href = "transacoes.html";
```

E ao abrir a tela, ela pergunta ao servidor se a sessão guardada ainda vale:

```js
async function conferirSessaoNoServidor() {
    if (!lerToken()) {
        return;
    }

    const usuario = await buscarUsuarioLogado();
    mostrarUsuarioLogado(usuario);
}
```

É o uso do `GET /auth/eu` descrito na seção 5.2: quem está com uma sessão válida vê quem está e
não precisa digitar nada; quem está com um token velho recebe `401`, é limpo pela
`requisicaoAutenticada` e cai na própria tela de login.

Por fim, uma assimetria proposital em `authApi.js`. Todas as chamadas autenticadas passam pela
`requisicaoAutenticada`, com uma exceção:

```js
async function logout() {
    await fetch(API_BASE_URL + "/auth/logout", {
        method: "POST",
        headers: { "Authorization": "Bearer " + lerToken() }
    });
}
```

O `logout` usa `fetch` direto. Se ele passasse pela função geral e o token já estivesse
inválido, o `throw` do `401` interromperia o fluxo justamente antes de `limparSessao`, e o
usuário ficaria preso com um token morto no `localStorage`. Sair do sistema precisa funcionar
mesmo quando a sessão já não vale, o que é uma boa regra geral: operações de recuperação não
devem depender da coisa que está quebrada.

---

## 7. Testando pelo terminal

Vale percorrer a sequência inteira pelo `curl`, porque ela torna visível cada peça da aula. Os
resultados abaixo são os obtidos durante a escrita do material.

```bash
# 1. rota protegida, sem token
curl -i http://localhost:8080/transacoes
# HTTP/1.1 401
# {"mensagem":"token ausente, inválido ou expirado"}

# 2. cadastro, que dispensa token
curl -s -X POST http://localhost:8080/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nome":"Ana","email":"ana@casa.com","senha":"1234","casaId":1}'

# 3. login, que emite o token
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@casa.com","senha":"1234"}'
# {"usuarioId":1,...,"token":"143b1896-9d92-4c71-8696-ccc7ab720824",
#  "expiraEm":"2026-10-22T22:02:32.438903"}

# 4. a mesma rota do passo 1, agora com o token
curl -s http://localhost:8080/transacoes \
  -H "Authorization: Bearer 143b1896-9d92-4c71-8696-ccc7ab720824"

# 5. quem sou eu
curl -s http://localhost:8080/auth/eu \
  -H "Authorization: Bearer 143b1896-9d92-4c71-8696-ccc7ab720824"

# 6. sair
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer 143b1896-9d92-4c71-8696-ccc7ab720824"
# 204

# 7. o mesmo token, depois do logout
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/auth/eu \
  -H "Authorization: Bearer 143b1896-9d92-4c71-8696-ccc7ab720824"
# 401
```

A tabela abaixo resume o que cada caso deve produzir, e serve de roteiro de verificação:

| Situação | Resposta esperada |
|---|---|
| Rota protegida, sem cabeçalho `Authorization` | `401` com a mensagem do interceptor. |
| Rota protegida, com token inexistente ou já expirado | `401`, com a **mesma** mensagem. |
| Rota protegida, com token válido | `200`, e o conteúdo depende de quem é o dono do token. |
| `POST /auth/login` com senha errada | `401` com "email ou senha inválidos", resposta do `controller`. |
| `POST /usuarios` sem token | `201`, pois é a exceção da seção 4.4. |
| `GET /usuarios` sem token | `401`. |

E vale conferir no banco o que esta aula produziu:

```sql
SELECT id, nome, email, senha_hash FROM usuario;
SELECT id, valor, usuario_id, criado_em, expira_em FROM token;
```

Duas coisas a observar. A coluna `senha_hash` não contém nenhuma senha legível, e duas contas
cadastradas com a mesma senha têm hashes diferentes. E a tabela `token` ganha uma linha nova a
cada login, o que significa que um mesmo usuário pode ter várias sessões simultâneas, uma por
dispositivo. Isso é desejável, e tem uma implicação que a atividade vai explorar: o `logout`
encerra apenas a sessão de onde foi pedido.

Uma observação sobre o Swagger UI. A rota aparece documentada, mas a interface ainda não tem o
botão **Authorize**, porque não declaramos ao springdoc que a API usa um esquema de segurança.
Sem isso, testar uma rota protegida pelo Swagger devolve `401`. Configurar esse esquema é a
primeira parte da atividade.

---

## 8. O quadro antes e depois

| Pergunta | Aula 10 | Aula 11 |
|---|---|---|
| O que está na coluna de senha? | A senha, legível. | Um hash BCrypt com salt, irreversível. |
| Como o servidor sabe quem chama? | Não sabe. | Pelo token no cabeçalho `Authorization`. |
| O que acontece ao recarregar a página? | O usuário perde o login. | A sessão continua, apoiada no `localStorage` e conferida no servidor. |
| `GET /transacoes` sem credencial | `200` com tudo. | `401`. |
| `GET /transacoes` com credencial | não existia | `200` só com as transações da casa de quem pediu. |
| Como se sai do sistema? | Limpando uma variável. | Apagando a linha do token no servidor. |

---

## 9. O que ainda está errado

A disciplina de encerrar a aula com os defeitos conhecidos se mantém, e aqui ela é mais
importante do que nunca: um sistema que parece seguro convida menos ao exame do que um que
avisa ser inseguro.

**Primeiro, e mais grave, a autorização ainda é só na porta.** O interceptor verifica se a
pessoa entrou, não o que ela tem direito de tocar lá dentro. A demonstração, feita durante os
testes desta aula com um token legítimo da Bia, que é da Casa B:

```
GET /transacoes/1   → 200, a transação "Feira", que pertence à Casa A
GET /categorias     → 200, as categorias das duas casas
GET /usuarios       → 200, nome e email de todos os usuários do sistema
```

A listagem de transações foi corrigida na seção 5.3, mas a busca por id não. Basta trocar o
número na URL para ler o dado de outra casa. Essa falha tem nome, **referência direta insegura
a objeto**, e aparece com destaque nas listas de vulnerabilidades mais comuns em APIs, sob o
rótulo de autorização quebrada em nível de objeto. Repare na lição: estar autenticado não é
estar autorizado, e a verificação de autorização precisa acontecer **por recurso**, no
`service`, onde se sabe a quem o recurso pertence. É o eixo da atividade desta aula.

**Segundo, não há limite de tentativas de login.** Um programa pode chamar `POST /auth/login`
milhares de vezes testando senhas. O BCrypt ajuda muito aqui, pois cada tentativa custa caro, e
é também por isso que a lentidão é um requisito e não um defeito. Mas a defesa adequada é
limitar as tentativas por email e por origem, o que o projeto não faz.

**Terceiro, tudo trafega em HTTP puro.** A senha vai no corpo do `POST` e o token vai no
cabeçalho, ambos legíveis para quem observa a rede. Nenhuma das defesas desta aula resiste a
isso. Em produção, `HTTPS` não é um detalhe de infraestrutura, é parte do esquema de
autenticação, e é a razão pela qual, no mundo real, a conversa entre o frontend e a API nunca
acontece sem ele.

**Quarto, a tabela `token` só cresce.** Tokens expirados são apagados apenas se alguém tentar
usá-los. Os que simplesmente são abandonados ficam lá para sempre. A solução é uma rotina de
limpeza periódica, assunto de uma aula sobre tarefas agendadas.

**Quinto, o cadastro público aceita qualquer `casaId`.** Como `POST /usuarios` dispensa token,
qualquer pessoa pode criar um usuário apontando para a casa de número 1 e, a partir daí, ver as
transações daquela casa pela rota que a seção 5.3 acabou de "proteger". O filtro por casa é
correto; o que falha é o controle de quem pode entrar em uma casa. A entidade `Casa` tem um
campo `codigoConvite`, até hoje sem uso, e ele é a pista de como isso deve ser resolvido.

Vale notar o padrão que os cinco itens formam. Nenhum deles é um erro de programação: são
decisões de escopo, cada uma sabendo o que deixou de fora. A diferença entre um sistema inseguro
e um sistema em construção é essa lista existir.

---

## 10. Atividade

A atividade desta aula está em
[atividades/11_auth_hash_token_atividade.md](atividades/11_auth_hash_token_atividade.md). Ela
tem quatro partes: declarar o esquema de segurança no Swagger UI, migrar as senhas antigas do
banco à mão, fechar a autorização em nível de objeto nas rotas que ficaram abertas, e analisar
por escrito o que o esquema desta aula ainda não cobre.

---

## Referências

Documentação oficial dos recursos usados nesta aula.

1. Spring Security, armazenamento de senhas, `PasswordEncoder` e BCrypt:
   https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
2. Spring Security, javadoc de `BCryptPasswordEncoder`:
   https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html
3. Spring Framework, `HandlerInterceptor`:
   https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/interceptors.html
4. Spring Framework, javadoc de `HandlerInterceptor`:
   https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/HandlerInterceptor.html
5. Spring Framework, argumentos aceitos em métodos de `@RequestMapping`, incluindo
   `@RequestAttribute`:
   https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/arguments.html
6. Spring Framework, anotação `@Bean` e classes de configuração:
   https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html
7. Spring Framework, suporte a CORS no Spring MVC:
   https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html
8. RFC 6750, `The OAuth 2.0 Authorization Framework: Bearer Token Usage`:
   https://www.rfc-editor.org/rfc/rfc6750.html
9. RFC 9110, HTTP Semantics, seção 11, autenticação HTTP e o cabeçalho `Authorization`:
   https://www.rfc-editor.org/rfc/rfc9110.html#name-http-authentication
10. OWASP, Password Storage Cheat Sheet, recomendações de algoritmo para senhas:
    https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
11. OWASP, Session Management Cheat Sheet, geração e expiração de identificadores de sessão:
    https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
12. OWASP API Security Top 10, `API1:2023 Broken Object Level Authorization`:
    https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/
13. MDN, `localStorage` e o escopo de armazenamento no navegador:
    https://developer.mozilla.org/pt-BR/docs/Web/API/Window/localStorage
14. MDN, requisições com preflight no CORS:
    https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Guides/CORS#requisi%C3%A7%C3%B5es_com_preflight
15. Java, javadoc de `UUID.randomUUID`:
    https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/UUID.html#randomUUID()
