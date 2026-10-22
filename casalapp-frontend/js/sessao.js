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

function lerUsuarioDaSessao() {
    const guardado = localStorage.getItem(CHAVE_USUARIO);
    return guardado ? JSON.parse(guardado) : null;
}

function limparSessao() {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_USUARIO);
}

function exigirLogin() {
    if (!lerToken()) {
        window.location.href = "login.html";
    }
}

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

function configurarBarraDeSessao() {
    const usuario = lerUsuarioDaSessao();
    const areaNome = document.getElementById("sessao-nome");

    if (areaNome && usuario) {
        areaNome.textContent = usuario.nome;
    }

    const linkSair = document.getElementById("link-sair");

    if (linkSair) {
        linkSair.addEventListener("click", async function (evento) {
            evento.preventDefault();
            await logout();
            limparSessao();
            window.location.href = "login.html";
        });
    }
}
