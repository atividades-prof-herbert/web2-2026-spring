async function listarCasas() {
    const resposta = await requisicaoAutenticada("/casas");
    return await resposta.json();
}

async function buscarCasa(id) {
    const resposta = await requisicaoAutenticada("/casas/" + id);
    return await resposta.json();
}

async function criarCasa(dados) {
    const resposta = await requisicaoAutenticada("/casas", {
        method: "POST",
        body: JSON.stringify(dados)
    });
    return await resposta.json();
}

async function atualizarCasa(id, dados) {
    const resposta = await requisicaoAutenticada("/casas/" + id, {
        method: "PUT",
        body: JSON.stringify(dados)
    });
    return await resposta.json();
}

async function deletarCasa(id) {
    await requisicaoAutenticada("/casas/" + id, { method: "DELETE" });
}
