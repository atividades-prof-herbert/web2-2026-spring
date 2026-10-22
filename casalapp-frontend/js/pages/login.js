const formLogin = document.getElementById("form-login");
const campoEmail = document.getElementById("login-email");
const campoSenha = document.getElementById("login-senha");
const painelErro = document.getElementById("login-erro");
const painelLogado = document.getElementById("painel-logado");
const nomeLogado = document.getElementById("nome-logado");
const casaLogada = document.getElementById("casa-logada");
const btnSair = document.getElementById("btn-sair");

function mostrarErro(mensagem) {
    painelErro.textContent = mensagem;
    painelErro.classList.remove("d-none");
}

function mostrarUsuarioLogado(usuario) {
    nomeLogado.textContent = usuario.nome;
    casaLogada.textContent = usuario.casaNome ? usuario.casaNome : "sem casa";
    painelErro.classList.add("d-none");
    formLogin.classList.add("d-none");
    painelLogado.classList.remove("d-none");
}

async function conferirSessaoNoServidor() {
    if (!lerToken()) {
        return;
    }

    const usuario = await buscarUsuarioLogado();
    mostrarUsuarioLogado(usuario);
}

formLogin.addEventListener("submit", async function (evento) {
    evento.preventDefault();

    const credenciais = {
        email: campoEmail.value,
        senha: campoSenha.value
    };

    const resultado = await login(credenciais);

    if (!resultado.ok) {
        mostrarErro(resultado.corpo.mensagem ? resultado.corpo.mensagem : "não foi possível entrar");
        return;
    }

    salvarSessao(resultado.corpo);
    window.location.href = "transacoes.html";
});

btnSair.addEventListener("click", async function () {
    await logout();
    limparSessao();
    formLogin.reset();
    formLogin.classList.remove("d-none");
    painelLogado.classList.add("d-none");
});

conferirSessaoNoServidor();
