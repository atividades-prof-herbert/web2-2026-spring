package br.edu.ifpr.casalapp.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import br.edu.ifpr.casalapp.model.Usuario;
import br.edu.ifpr.casalapp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_USUARIO_LOGADO = "usuarioLogado";

    private static final String PREFIXO_BEARER = "Bearer ";

    private final AuthService authService;

    public AutenticacaoInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (dispensaToken(request)) {
            return true;
        }

        String token = extrairToken(request.getHeader("Authorization"));
        Usuario usuario = authService.resolverUsuario(token);

        if (usuario == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"mensagem\":\"token ausente, inválido ou expirado\"}");
            return false;
        }

        request.setAttribute(ATRIBUTO_USUARIO_LOGADO, usuario);
        return true;
    }

    private boolean dispensaToken(HttpServletRequest request) {
        boolean preflightCors = "OPTIONS".equals(request.getMethod());
        boolean cadastroDeNovoUsuario = "POST".equals(request.getMethod())
                && "/usuarios".equals(request.getRequestURI());

        return preflightCors || cadastroDeNovoUsuario;
    }

    private String extrairToken(String cabecalho) {
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO_BEARER)) {
            return null;
        }
        return cabecalho.substring(PREFIXO_BEARER.length());
    }
}
