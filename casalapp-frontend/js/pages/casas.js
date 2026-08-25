const formCasa = document.getElementById("form-casa");
const campoId = document.getElementById("casa-id");
const campoNome = document.getElementById("casa-nome");
const tabelaCasas = document.getElementById("tabela-casas");
const btnCancelar = document.getElementById("btn-cancelar");

async function carregarCasas() {
    const casas = await listarCasas();
    tabelaCasas.innerHTML = "";

    for (let i = 0; i < casas.length; i++) {
        const casa = casas[i];
        const linha = document.createElement("tr");
        linha.innerHTML =
            "<td>" + casa.id + "</td>" +
            "<td>" + casa.nome + "</td>" +
            "<td>" + (casa.codigoConvite ? casa.codigoConvite : "-") + "</td>" +
            "<td class='acoes'>" +
            "<button class='btn btn-sm btn-warning' onclick='editarCasa(" + casa.id + ")'>Editar</button>" +
            "<button class='btn btn-sm btn-danger' onclick='excluirCasa(" + casa.id + ")'>Excluir</button>" +
            "</td>";
        tabelaCasas.appendChild(linha);
    }
}

async function editarCasa(id) {
    const casa = await buscarCasa(id);
    campoId.value = casa.id;
    campoNome.value = casa.nome;
    btnCancelar.classList.remove("d-none");
}

async function excluirCasa(id) {
    if (confirm("Deseja excluir esta casa?")) {
        await deletarCasa(id);
        await carregarCasas();
    }
}

function limparFormulario() {
    campoId.value = "";
    formCasa.reset();
    btnCancelar.classList.add("d-none");
}

formCasa.addEventListener("submit", async function (evento) {
    evento.preventDefault();

    const casa = { nome: campoNome.value };
    const id = campoId.value;

    if (id) {
        await atualizarCasa(id, casa);
    } else {
        await criarCasa(casa);
    }

    limparFormulario();
    await carregarCasas();
});

btnCancelar.addEventListener("click", limparFormulario);

carregarCasas();
