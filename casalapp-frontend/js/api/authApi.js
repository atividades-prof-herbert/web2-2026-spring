async function login(credenciais) {
    const resposta = await fetch(API_BASE_URL + "/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(credenciais)
    });

    const corpo = await resposta.json();

    return { ok: resposta.ok, status: resposta.status, corpo: corpo };
}

async function buscarUsuarioLogado() {
    const resposta = await requisicaoAutenticada("/auth/eu");
    return await resposta.json();
}

async function logout() {
    await fetch(API_BASE_URL + "/auth/logout", {
        method: "POST",
        headers: { "Authorization": "Bearer " + lerToken() }
    });
}
