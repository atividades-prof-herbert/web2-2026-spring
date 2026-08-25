const formTransacao = document.getElementById("form-transacao");
const campoId = document.getElementById("transacao-id");
const campoDescricao = document.getElementById("transacao-descricao");
const campoValor = document.getElementById("transacao-valor");
const campoCategoria = document.getElementById("transacao-categoria");
const tabelaTransacoes = document.getElementById("tabela-transacoes");
const btnCancelar = document.getElementById("btn-cancelar");

async function carregarCategoriasNoSelect() {
    const categorias = await listarCategorias();

    for (let i = 0; i < categorias.length; i++) {
        const categoria = categorias[i];
        const opcao = document.createElement("option");
        opcao.value = categoria.id;
        opcao.textContent = categoria.nome;
        campoCategoria.appendChild(opcao);
    }
}

async function carregarTransacoes() {
    const transacoes = await listarTransacoes();
    tabelaTransacoes.innerHTML = "";

    for (let i = 0; i < transacoes.length; i++) {
        const transacao = transacoes[i];
        const linha = document.createElement("tr");
        linha.innerHTML =
            "<td>" + transacao.id + "</td>" +
            "<td>" + transacao.descricao + "</td>" +
            "<td>" + transacao.valor + "</td>" +
            "<td>" + (transacao.categoriaNome ? transacao.categoriaNome : "-") + "</td>" +
            "<td>" + (transacao.casaNome ? transacao.casaNome : "-") + "</td>" +
            "<td class='acoes'>" +
            "<button class='btn btn-sm btn-warning' onclick='editarTransacao(" + transacao.id + ")'>Editar</button>" +
            "<button class='btn btn-sm btn-danger' onclick='excluirTransacao(" + transacao.id + ")'>Excluir</button>" +
            "</td>";
        tabelaTransacoes.appendChild(linha);
    }
}

async function editarTransacao(id) {
    const transacao = await buscarTransacao(id);
    campoId.value = transacao.id;
    campoDescricao.value = transacao.descricao;
    campoValor.value = transacao.valor;
    campoCategoria.value = transacao.categoriaId;
    btnCancelar.classList.remove("d-none");
}

async function excluirTransacao(id) {
    if (confirm("Deseja excluir esta transação?")) {
        await deletarTransacao(id);
        await carregarTransacoes();
    }
}

function limparFormulario() {
    campoId.value = "";
    formTransacao.reset();
    btnCancelar.classList.add("d-none");
}

formTransacao.addEventListener("submit", async function (evento) {
    evento.preventDefault();

    const transacao = {
        descricao: campoDescricao.value,
        valor: Number(campoValor.value),
        categoriaId: Number(campoCategoria.value)
    };
    const id = campoId.value;

    if (id) {
        await atualizarTransacao(id, transacao);
    } else {
        await criarTransacao(transacao);
    }

    limparFormulario();
    await carregarTransacoes();
});

btnCancelar.addEventListener("click", limparFormulario);

carregarCategoriasNoSelect();
carregarTransacoes();
