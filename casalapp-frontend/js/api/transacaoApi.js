async function listarTransacoes() {
    const resposta = await fetch(API_BASE_URL + "/transacoes");
    return await resposta.json();
}

async function buscarTransacao(id) {
    const resposta = await fetch(API_BASE_URL + "/transacoes/" + id);
    return await resposta.json();
}

async function criarTransacao(transacao) {
    const resposta = await fetch(API_BASE_URL + "/transacoes", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(transacao)
    });
    return await resposta.json();
}

async function atualizarTransacao(id, transacao) {
    const resposta = await fetch(API_BASE_URL + "/transacoes/" + id, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(transacao)
    });
    return await resposta.json();
}

async function deletarTransacao(id) {
    await fetch(API_BASE_URL + "/transacoes/" + id, { method: "DELETE" });
}
