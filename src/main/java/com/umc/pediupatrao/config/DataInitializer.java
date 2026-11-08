package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    // Cria um usuário de cada perfil para facilitar os testes e a demonstração.
    // Troque essas senhas se for colocar o sistema para valer!
    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            criarSeNaoExistir(repo, encoder, "admin", "teste123", Perfil.ADMIN);
            criarSeNaoExistir(repo, encoder, "gerente", "teste123", Perfil.GERENTE);
            criarSeNaoExistir(repo, encoder, "atendente", "teste123", Perfil.ATENDENTE);

            // O admin antigo foi criado sem perfil; garantimos que ele seja ADMIN
            repo.findByUsername("admin").ifPresent(admin -> {
                if (!Perfil.ADMIN.name().equals(admin.getRole())) {
                    admin.setRole(Perfil.ADMIN.name());
                    repo.save(admin);
                }
            });
        };
    }

    private void criarSeNaoExistir(UsuarioRepository repo, PasswordEncoder encoder,
            String username, String senha, Perfil perfil) {
        if (repo.findByUsername(username).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPassword(encoder.encode(senha));
            usuario.setRole(perfil.name());
            repo.save(usuario);
            System.out.println("Usuário " + username + " criado (" + perfil + ")");
        }
    }
}
