async function listarCategorias() {
    const resposta = await requisicaoAutenticada("/categorias");
    return await resposta.json();
}

async function buscarCategoria(id) {
    const resposta = await requisicaoAutenticada("/categorias/" + id);
    return await resposta.json();
}

async function criarCategoria(dados) {
    const resposta = await requisicaoAutenticada("/categorias", {
        method: "POST",
        body: JSON.stringify(dados)
    });
    return await resposta.json();
}

async function atualizarCategoria(id, dados) {
    const resposta = await requisicaoAutenticada("/categorias/" + id, {
        method: "PUT",
        body: JSON.stringify(dados)
    });
    return await resposta.json();
}

async function deletarCategoria(id) {
    await requisicaoAutenticada("/categorias/" + id, { method: "DELETE" });
}
