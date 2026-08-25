const formCategoria = document.getElementById("form-categoria");
const campoId = document.getElementById("categoria-id");
const campoNome = document.getElementById("categoria-nome");
const campoIcone = document.getElementById("categoria-icone");
const campoCasa = document.getElementById("categoria-casa");
const tabelaCategorias = document.getElementById("tabela-categorias");
const btnCancelar = document.getElementById("btn-cancelar");

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

async function carregarCategorias() {
    const categorias = await listarCategorias();
    tabelaCategorias.innerHTML = "";

    for (let i = 0; i < categorias.length; i++) {
        const categoria = categorias[i];
        const linha = document.createElement("tr");
        linha.innerHTML =
            "<td>" + categoria.id + "</td>" +
            "<td>" + categoria.nome + "</td>" +
            "<td>" + (categoria.icone ? categoria.icone : "-") + "</td>" +
            "<td>" + (categoria.casaNome ? categoria.casaNome : "-") + "</td>" +
            "<td class='acoes'>" +
            "<button class='btn btn-sm btn-warning' onclick='editarCategoria(" + categoria.id + ")'>Editar</button>" +
            "<button class='btn btn-sm btn-danger' onclick='excluirCategoria(" + categoria.id + ")'>Excluir</button>" +
            "</td>";
        tabelaCategorias.appendChild(linha);
    }
}

async function editarCategoria(id) {
    const categoria = await buscarCategoria(id);
    campoId.value = categoria.id;
    campoNome.value = categoria.nome;
    campoIcone.value = categoria.icone ? categoria.icone : "";
    campoCasa.value = categoria.casaId;
    btnCancelar.classList.remove("d-none");
}

async function excluirCategoria(id) {
    if (confirm("Deseja excluir esta categoria?")) {
        await deletarCategoria(id);
        await carregarCategorias();
    }
}

function limparFormulario() {
    campoId.value = "";
    formCategoria.reset();
    btnCancelar.classList.add("d-none");
}

formCategoria.addEventListener("submit", async function (evento) {
    evento.preventDefault();

    const categoria = {
        nome: campoNome.value,
        icone: campoIcone.value,
        casaId: Number(campoCasa.value)
    };
    const id = campoId.value;

    if (id) {
        await atualizarCategoria(id, categoria);
    } else {
        await criarCategoria(categoria);
    }

    limparFormulario();
    await carregarCategorias();
});

btnCancelar.addEventListener("click", limparFormulario);

carregarCasasNoSelect();
carregarCategorias();
