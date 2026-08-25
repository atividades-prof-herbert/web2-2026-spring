async function listarUsuarios() {
    const resposta = await fetch(API_BASE_URL + "/usuarios");
    return await resposta.json();
}

async function buscarUsuario(id) {
    const resposta = await fetch(API_BASE_URL + "/usuarios/" + id);
    return await resposta.json();
}

async function criarUsuario(usuario) {
    const resposta = await fetch(API_BASE_URL + "/usuarios", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(usuario)
    });
    return await resposta.json();
}

async function atualizarUsuario(id, usuario) {
    const resposta = await fetch(API_BASE_URL + "/usuarios/" + id, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(usuario)
    });
    return await resposta.json();
}

async function atualizarParcialUsuario(id, usuario) {
    const resposta = await fetch(API_BASE_URL + "/usuarios/" + id, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(usuario)
    });
    return await resposta.json();
}

async function deletarUsuario(id) {
    await fetch(API_BASE_URL + "/usuarios/" + id, { method: "DELETE" });
}
