const formUsuario = document.getElementById("form-usuario");
const campoId = document.getElementById("usuario-id");
const campoNome = document.getElementById("usuario-nome");
const campoEmail = document.getElementById("usuario-email");
const campoSenha = document.getElementById("usuario-senha");
const campoCasa = document.getElementById("usuario-casa");
const tabelaUsuarios = document.getElementById("tabela-usuarios");
const btnCancelar = document.getElementById("btn-cancelar");
const dicaSenha = document.getElementById("dica-senha");

async function carregarCasasNoSelect() {
    const casas = await listarCasas();

    for (let i = 0; i < casas.length; i++) {
        const casa = casas[i];
        const opcao = document.createElement("option");
        opcao.value = casa.id;
        opcao.textContent = casa.nome;
        campoCasa.appendChild(opcao);
    }
}

async function carregarUsuarios() {
    const usuarios = await listarUsuarios();
    tabelaUsuarios.innerHTML = "";

    for (let i = 0; i < usuarios.length; i++) {
        const usuario = usuarios[i];
        const linha = document.createElement("tr");
        linha.innerHTML =
            "<td>" + usuario.id + "</td>" +
            "<td>" + usuario.nome + "</td>" +
            "<td>" + usuario.email + "</td>" +
            "<td>" + (usuario.casaNome ? usuario.casaNome : "-") + "</td>" +
            "<td class='acoes'>" +
            "<button class='btn btn-sm btn-warning' onclick='editarUsuario(" + usuario.id + ")'>Editar</button>" +
            "<button class='btn btn-sm btn-danger' onclick='excluirUsuario(" + usuario.id + ")'>Excluir</button>" +
            "</td>";
        tabelaUsuarios.appendChild(linha);
    }
}

async function editarUsuario(id) {
    const usuario = await buscarUsuario(id);
    campoId.value = usuario.id;
    campoNome.value = usuario.nome;
    campoEmail.value = usuario.email;
    campoSenha.value = "";
    campoCasa.value = usuario.casaId;
    btnCancelar.classList.remove("d-none");
    dicaSenha.classList.remove("d-none");
}

async function excluirUsuario(id) {
    if (confirm("Deseja excluir este usuário?")) {
        await deletarUsuario(id);
        await carregarUsuarios();
    }
}

function limparFormulario() {
    campoId.value = "";
    formUsuario.reset();
    btnCancelar.classList.add("d-none");
    dicaSenha.classList.add("d-none");
}

formUsuario.addEventListener("submit", async function (evento) {
    evento.preventDefault();

    const id = campoId.value;

    if (id) {
        const alteracoes = {
            nome: campoNome.value,
            email: campoEmail.value,
            senha: campoSenha.value === "" ? null : campoSenha.value,
            casaId: Number(campoCasa.value)
        };
        await atualizarParcialUsuario(id, alteracoes);
    } else {
        const usuario = {
            nome: campoNome.value,
            email: campoEmail.value,
            senha: campoSenha.value,
            casaId: Number(campoCasa.value)
        };
        await criarUsuario(usuario);
    }

    limparFormulario();
    await carregarUsuarios();
});

btnCancelar.addEventListener("click", limparFormulario);

carregarCasasNoSelect();
carregarUsuarios();
