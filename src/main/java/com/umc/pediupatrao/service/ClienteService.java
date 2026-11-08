package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Alteracao;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public Cliente novoCliente(Cliente cliente) {
        return salvar(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Exclui o cliente e registra na auditoria
    public void excluir(String id) {
        Optional<Cliente> existente = clienteRepository.findById(id);
        clienteRepository.deleteById(id);

        if (existente.isPresent()) {
            List<Alteracao> alteracoes = new ArrayList<>();
            alteracoes.add(new Alteracao("nome", existente.get().getNome(), null));
            auditoriaService.registrar(TipoOperacao.EXCLUSAO_CLIENTE, "CLIENTE", id, alteracoes, null);
        }
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Salva um cliente novo ou atualiza um existente, auditando o que mudou
    public Cliente salvar(Cliente cliente) {
        // Cliente novo
        if (cliente.getId() == null || cliente.getId().isBlank()) {
            cliente.setId(null);
            Cliente criado = clienteRepository.save(cliente);

            List<Alteracao> alteracoes = compararClientes(new Cliente(), criado);
            auditoriaService.registrar(TipoOperacao.CRIACAO_CLIENTE, "CLIENTE", criado.getId(), alteracoes, null);
            return criado;
        }

        // Cliente existente: busca como está no banco para comparar com o novo
        Cliente antes = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para atualização."));

        List<Alteracao> alteracoes = compararClientes(antes, cliente);
        Cliente atualizado = clienteRepository.save(cliente);

        // Só registra se alguma coisa realmente mudou
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO_CLIENTE, "CLIENTE", atualizado.getId(), alteracoes, null);
        }
        return atualizado;
    }

    // Compara campo a campo (endereço e telefone são os mais importantes)
    private List<Alteracao> compararClientes(Cliente antes, Cliente depois) {
        List<Alteracao> lista = new ArrayList<>();
        auditoriaService.compararCampo(lista, "nome", antes.getNome(), depois.getNome());
        auditoriaService.compararCampo(lista, "telefone", antes.getTelefone(), depois.getTelefone());
        auditoriaService.compararCampo(lista, "cep", antes.getCep(), depois.getCep());
        auditoriaService.compararCampo(lista, "logradouro", antes.getLogradouro(), depois.getLogradouro());
        auditoriaService.compararCampo(lista, "numero", antes.getNumero(), depois.getNumero());
        auditoriaService.compararCampo(lista, "complemento", antes.getComplemento(), depois.getComplemento());
        auditoriaService.compararCampo(lista, "bairro", antes.getBairro(), depois.getBairro());
        auditoriaService.compararCampo(lista, "cidade", antes.getCidade(), depois.getCidade());
        auditoriaService.compararCampo(lista, "estado", antes.getEstado(), depois.getEstado());
        auditoriaService.compararCampo(lista, "endereco", antes.getEndereco(), depois.getEndereco());
        return lista;
    }
}
