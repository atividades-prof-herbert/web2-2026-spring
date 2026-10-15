let usuarioLogado = null;

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

function mostrarUsuarioLogado() {
    nomeLogado.textContent = usuarioLogado.nome;
    casaLogada.textContent = usuarioLogado.casaNome ? usuarioLogado.casaNome : "sem casa";
    painelErro.classList.add("d-none");
    formLogin.classList.add("d-none");
    painelLogado.classList.remove("d-none");
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

    usuarioLogado = resultado.corpo;
    mostrarUsuarioLogado();
});

btnSair.addEventListener("click", function () {
    usuarioLogado = null;
    formLogin.reset();
    formLogin.classList.remove("d-none");
    painelLogado.classList.add("d-none");
});
