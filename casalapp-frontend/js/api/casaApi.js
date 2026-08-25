async function listarCasas() {
    const resposta = await fetch(API_BASE_URL + "/casas");
    return await resposta.json();
}

async function buscarCasa(id) {
    const resposta = await fetch(API_BASE_URL + "/casas/" + id);
    return await resposta.json();
}

async function criarCasa(casa) {
    const resposta = await fetch(API_BASE_URL + "/casas", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(casa)
    });
    return await resposta.json();
}

async function atualizarCasa(id, casa) {
    const resposta = await fetch(API_BASE_URL + "/casas/" + id, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(casa)
    });
    return await resposta.json();
}

async function deletarCasa(id) {
    await fetch(API_BASE_URL + "/casas/" + id, { method: "DELETE" });
}
