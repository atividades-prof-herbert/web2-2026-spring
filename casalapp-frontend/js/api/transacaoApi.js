async function listarTransacoes() {
    const resposta = await requisicaoAutenticada("/transacoes");
    return await resposta.json();
}

async function buscarTransacao(id) {
    const resposta = await requisicaoAutenticada("/transacoes/" + id);
    return await resposta.json();
}

async function criarTransacao(dados) {
    const resposta = await requisicaoAutenticada("/transacoes", {
        method: "POST",
        body: JSON.stringify(dados)
    });
    return await resposta.json();
}

async function atualizarTransacao(id, dados) {
    const resposta = await requisicaoAutenticada("/transacoes/" + id, {
        method: "PUT",
        body: JSON.stringify(dados)
    });
    return await resposta.json();
}

async function deletarTransacao(id) {
    await requisicaoAutenticada("/transacoes/" + id, { method: "DELETE" });
}
