async function listarUsuarios() {
    const resposta = await requisicaoAutenticada("/usuarios");
    return await resposta.json();
}

async function buscarUsuario(id) {
    const resposta = await requisicaoAutenticada("/usuarios/" + id);
    return await resposta.json();
}

async function criarUsuario(dados) {
    const resposta = await requisicaoAutenticada("/usuarios", {
        method: "POST",
        body: JSON.stringify(dados)
    });
    return await resposta.json();
}

async function atualizarUsuario(id, dados) {
    const resposta = await requisicaoAutenticada("/usuarios/" + id, {
        method: "PUT",
        body: JSON.stringify(dados)
    });
    return await resposta.json();
}

async function atualizarParcialUsuario(id, dados) {
    const resposta = await requisicaoAutenticada("/usuarios/" + id, {
        method: "PATCH",
        body: JSON.stringify(dados)
    });
    return await resposta.json();
}

async function deletarUsuario(id) {
    await requisicaoAutenticada("/usuarios/" + id, { method: "DELETE" });
}
