async function listarCategorias() {
    const resposta = await fetch(API_BASE_URL + "/categorias");
    return await resposta.json();
}

async function buscarCategoria(id) {
    const resposta = await fetch(API_BASE_URL + "/categorias/" + id);
    return await resposta.json();
}

async function criarCategoria(categoria) {
    const resposta = await fetch(API_BASE_URL + "/categorias", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(categoria)
    });
    return await resposta.json();
}

async function atualizarCategoria(id, categoria) {
    const resposta = await fetch(API_BASE_URL + "/categorias/" + id, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(categoria)
    });
    return await resposta.json();
}

async function deletarCategoria(id) {
    await fetch(API_BASE_URL + "/categorias/" + id, { method: "DELETE" });
}
