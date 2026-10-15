async function login(credenciais) {
    const resposta = await fetch(API_BASE_URL + "/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(credenciais)
    });

    const corpo = await resposta.json();

    return { ok: resposta.ok, status: resposta.status, corpo: corpo };
}
