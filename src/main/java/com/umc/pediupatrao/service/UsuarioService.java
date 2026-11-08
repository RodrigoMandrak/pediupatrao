package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    public void salvarUsuario(Usuario usuario) {
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new IllegalArgumentException("Informe o nome de usuário.");
        }
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("Informe a senha.");
        }
        if (!Perfil.valido(usuario.getRole())) {
            throw new IllegalArgumentException("Perfil inválido. Use ADMIN, GERENTE ou ATENDENTE.");
        }
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Já existe um usuário com esse nome.");
        }

        usuario.setId(null); // garante que é um cadastro novo
        usuario.setRole(usuario.getRole().trim().toUpperCase());
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        usuarioRepository.save(usuario);
    }

    // Deleta um usuário pelo ID (não deixa o usuário apagar a si mesmo)
    public void deletarUsuario(String id) {
        usuarioRepository.findById(id).ifPresent(u -> {
            if (u.getUsername().equals(UsuarioLogado.nome())) {
                throw new IllegalArgumentException("Você não pode excluir o próprio usuário.");
            }
        });
        usuarioRepository.deleteById(id);
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário
    public void atualizarUsuario(String id, Usuario usuarioAtualizado) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (!Perfil.valido(usuarioAtualizado.getRole())) {
            throw new IllegalArgumentException("Perfil inválido. Use ADMIN, GERENTE ou ATENDENTE.");
        }
        String novoPerfil = usuarioAtualizado.getRole().trim().toUpperCase();

        // Proteção contra autoelevação/autorrebaixamento: ninguém troca o próprio perfil
        boolean ehOProprioUsuario = existente.getUsername().equals(UsuarioLogado.nome());
        if (ehOProprioUsuario && !novoPerfil.equals(Perfil.deTexto(existente.getRole()).name())) {
            throw new IllegalArgumentException("Você não pode alterar o seu próprio perfil.");
        }

        // Troca de username não pode colidir com outro usuário
        Optional<Usuario> mesmoNome = usuarioRepository.findByUsername(usuarioAtualizado.getUsername());
        if (mesmoNome.isPresent() && !mesmoNome.get().getId().equals(id)) {
            throw new IllegalArgumentException("Já existe um usuário com esse nome.");
        }

        existente.setUsername(usuarioAtualizado.getUsername());
        existente.setRole(novoPerfil);

        // Só atualiza a senha se uma nova foi informada
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            existente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }

        usuarioRepository.save(existente);
    }
}
