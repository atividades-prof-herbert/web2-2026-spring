# Aula 12 (atividade): sessão no navegador

Leia [12_auth_frontend.md](../12_auth_frontend.md) antes de começar. A atividade tem três
partes: aplicar o padrão de sessão a uma tela que ainda não tem, provocar de propósito os dois
modos de falha estudados na aula, e analisar por escrito o que a aula deixou em aberto.

---

## Parte 1: uma tela nova com sessão

Escolha uma tela do `casalapp-frontend` que **não** chame `exigirLogin()` e
`configurarBarraDeSessao()` no início do seu arquivo em `js/pages/`. Se todas já chamarem, crie
uma tela simples nova (por exemplo, uma tela de perfil que mostra os dados de `GET /auth/eu`) só
para esta atividade.

- [ ] Adicione as duas chamadas no início do arquivo, na mesma ordem usada nas demais telas.
- [ ] Garanta que o `<script>` de `sessao.js` está incluído no HTML da página, antes do script da
      própria página.
- [ ] Abra a tela sem token nenhum no navegador e confirme o redirecionamento para `login.html`.
- [ ] Faça login, abra a tela de novo e confirme que a barra de sessão mostra o nome correto.

Responda: se você esquecesse de incluir `js/sessao.js` no HTML, mas mantivesse a chamada a
`exigirLogin()` no script da página, o que apareceria no console do navegador? Relacione sua
resposta com a ordem dos `<script>` nas páginas que já funcionam.

---

## Parte 2: provocar os dois modos de falha

### 1. O token que nunca existiu

- [ ] Pelo console do navegador, rode `localStorage.clear()`.
- [ ] Abra `transacoes.html` diretamente, digitando a URL.

Responda: qual função decidiu redirecionar para o login, e em que ponto exatamente ela tomou
essa decisão? Nenhuma requisição chegou a ser feita à API neste caso. Por quê?

### 2. O token que existia e deixou de valer

- [ ] Faça login normalmente e copie o valor de `casalapp.token` do `localStorage`.
- [ ] Pelo console, chame `fetch` manualmente contra `POST /auth/logout` com esse token no
      cabeçalho `Authorization`, **sem** recarregar a página.
- [ ] Sem recarregar, clique em algo na tela que dispare uma nova chamada à API (por exemplo,
      abrir a lista de transações de novo).

Responda: desta vez a página já estava aberta havia algum tempo com um token que, àquela altura,
o servidor já tinha apagado. Em que ponto do fluxo o `401` foi percebido, já que `exigirLogin`
não participou desta vez? Qual função do `sessao.js` foi responsável por tirar a pessoa da tela?

### 3. Onde está a proteção, de novo

- [ ] Repita o experimento da seção 4.1 da aula: apague temporariamente as duas linhas do início
      de `js/pages/transacoes.js`, limpe o `localStorage` e abra a página.
- [ ] Abra a aba Network das ferramentas de desenvolvedor e registre o que cada requisição
      devolveu.

Responda: algum dado de alguma casa apareceu na tela ou na aba Network? O que isso prova sobre
quem de fato impede o acesso, o código do navegador ou o `HandlerInterceptor` do backend? Desfaça
a alteração ao terminar.

---

## Parte 3: análise do que ficou em aberto

Responda por escrito, usando a seção 7 da aula como ponto de partida. Não há código a entregar
nesta parte.

### 4. XSS e o `localStorage`

Suponha que uma falha em outra parte do `casalapp-frontend` permitisse a alguém injetar um
`<script>` numa das telas.

- [ ] Escreva, em três ou quatro linhas de pseudocódigo, o que esse script precisaria fazer para
      roubar a sessão de quem estiver com a tela aberta.

Responda: essa falha existiria se o token estivesse guardado num *cookie* `HttpOnly` em vez de no
`localStorage`? O que o sistema perderia em troca, considerando a seção 1 da aula?

### 5. Avisar antes de expirar

A aula descreveu que a sessão de oito horas simplesmente para de funcionar, sem aviso.

- [ ] Descreva, em palavras, onde no código você colocaria uma verificação que avisasse "sua
      sessão termina em poucos minutos", sem fazer nenhuma chamada nova à API.

Responda: que informação, hoje guardada no `localStorage` ou devolvida pelo `LoginResponseDTO`
da aula 11, essa verificação usaria? Ela seria exata ou apenas uma estimativa? Por quê?

### 6. Um menu que diferencia usuários

Hoje a barra de sessão mostra os mesmos links para qualquer pessoa autenticada.

Responda: se o sistema ganhasse um papel de administrador, capaz de ver `GET /usuarios` de
qualquer casa, esconder ou mostrar um link no menu resolveria o problema de segurança? Justifique
usando a mesma distinção da seção 4.1 desta aula entre o que protege de verdade e o que é apenas
conveniência de interface.

---

## Checklist de verificação

- [ ] A tela escolhida (ou criada) na parte 1 redireciona para `login.html` sem token e mostra o
      nome correto com um token válido.
- [ ] O experimento do token inexistente e o do token invalidado em tempo real foram reproduzidos
      e descritos.
- [ ] O experimento de remover `exigirLogin` foi repetido, e nenhum dado de nenhuma casa apareceu
      na tela nem na aba Network.
- [ ] As respostas das questões 1 a 6 estão escritas e entregues junto com o código.

---

## Referências

1. MDN, ferramentas de desenvolvedor, a aba Network:
   https://developer.mozilla.org/pt-BR/docs/Tools/Network_Monitor
2. OWASP, Cross Site Scripting (XSS) Prevention Cheat Sheet:
   https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html
3. OWASP, Session Management Cheat Sheet, expiração e renovação de sessão:
   https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
