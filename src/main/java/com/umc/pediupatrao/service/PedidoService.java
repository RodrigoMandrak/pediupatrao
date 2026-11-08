package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Alteracao;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private static final BigDecimal DESCONTO_MAXIMO = new BigDecimal("20");
    private static final BigDecimal CEM = new BigDecimal("100");

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository,
            ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    // ------------------------------------------------------------
    // RF02 - criação do pedido (o total é sempre calculado aqui)
    // ------------------------------------------------------------
    public Pedido criarPedido(Pedido dados) {
        if (dados.getClienteId() == null || !clienteRepository.existsById(dados.getClienteId())) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }
        if (dados.getItens() == null || dados.getItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa ter pelo menos um item.");
        }

        // Cria um pedido novo e copia só o que o cliente pode informar (cliente e itens).
        // Status, total, desconto e responsável NUNCA vêm da requisição.
        Pedido pedido = new Pedido();
        pedido.setClienteId(dados.getClienteId());

        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int quantidadeTotal = 0;

        for (ItemPedido pedidoItem : dados.getItens()) {
            if (pedidoItem.getProdutoId() == null) {
                throw new IllegalArgumentException("Item sem produto.");
            }
            if (pedidoItem.getQuantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade de cada item deve ser maior que zero.");
            }
            Produto produto = produtoRepository.findById(pedidoItem.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
            if (Boolean.FALSE.equals(produto.getAtivo()) || produto.getPreco() == null) {
                throw new IllegalArgumentException("Produto indisponível: " + produto.getNome());
            }

            // O preço vem do cadastro do produto, não da requisição
            ItemPedido item = new ItemPedido(produto.getId(), produto.getNome(),
                    BigDecimal.valueOf(produto.getPreco()), pedidoItem.getQuantidade());
            itens.add(item);
            subtotal = subtotal.add(item.getSubtotal());
            quantidadeTotal += item.getQuantidade();
        }

        pedido.setItens(itens);
        pedido.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        pedido.setDescontoPercentual(BigDecimal.ZERO);
        recalcularTotal(pedido);

        pedido.setStatus(StatusPedido.RECEBIDO.name());
        pedido.setDataEntrada(LocalDateTime.now());
        pedido.setResponsavelEntrada(UsuarioLogado.nome());

        // Campos antigos, só para a listagem antiga continuar fazendo sentido
        pedido.setQuantidade(quantidadeTotal);
        pedido.setSabor(itens.get(0).getNome());

        Pedido salvo = pedidoRepository.save(pedido);

        List<Alteracao> alteracoes = new ArrayList<>();
        alteracoes.add(new Alteracao("status", null, salvo.getStatus()));
        alteracoes.add(new Alteracao("valorTotal", null, salvo.getValorTotal().toPlainString()));
        alteracoes.add(new Alteracao("itens", null, resumoItens(itens)));
        auditoriaService.registrar(TipoOperacao.CRIACAO_PEDIDO, "PEDIDO", salvo.getId(), alteracoes, null);

        return salvo;
    }

    // ------------------------------------------------------------
    // RF03 - mudança de status (sem cancelamento)
    // ------------------------------------------------------------
    public Pedido atualizarStatus(String id, String novoTexto) {
        StatusPedido novo = StatusPedido.deTexto(novoTexto);
        if (novo == StatusPedido.CANCELADO) {
            throw new IllegalArgumentException("Para cancelar use a operação de cancelamento (somente gerente).");
        }

        Pedido pedido = buscar(id);
        StatusPedido atual = statusDe(pedido);

        if (atual == StatusPedido.CANCELADO) {
            throw new IllegalStateException("Pedido cancelado não pode mais ser alterado.");
        }
        if (!atual.podeIrPara(novo)) {
            throw new IllegalStateException("Transição inválida: " + atual + " -> " + novo);
        }

        List<Alteracao> alteracoes = new ArrayList<>();
        alteracoes.add(new Alteracao("status", atual.name(), novo.name()));

        // Na saída (entrega ou retirada) registramos data/hora e quem liberou
        if (novo == StatusPedido.SAIU_PARA_ENTREGA || novo == StatusPedido.RETIRADO) {
            pedido.setDataSaida(LocalDateTime.now());
            pedido.setResponsavelSaida(UsuarioLogado.nome());
            alteracoes.add(new Alteracao("responsavelSaida", null, pedido.getResponsavelSaida()));
        }

        pedido.setStatus(novo.name());
        Pedido salvo = pedidoRepository.save(pedido);

        auditoriaService.registrar(TipoOperacao.MUDANCA_STATUS, "PEDIDO", id, alteracoes, null);
        return salvo;
    }

    // ------------------------------------------------------------
    // RF04 - só o gerente (a regra também está no serviço, não só na rota)
    // ------------------------------------------------------------
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelarPedido(String id, String justificativa) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("A justificativa do cancelamento é obrigatória.");
        }

        Pedido pedido = buscar(id);
        StatusPedido atual = statusDe(pedido);

        if (atual == StatusPedido.CANCELADO) {
            throw new IllegalStateException("O pedido já está cancelado.");
        }
        if (!atual.antesDaSaida()) {
            throw new IllegalStateException("Não é possível cancelar um pedido que já saiu para entrega/retirada.");
        }

        pedido.setStatus(StatusPedido.CANCELADO.name());
        pedido.setDataCancelamento(LocalDateTime.now());
        pedido.setResponsavelCancelamento(UsuarioLogado.nome());
        pedido.setJustificativaCancelamento(justificativa.trim());
        Pedido salvo = pedidoRepository.save(pedido); // os dados do pedido continuam guardados

        List<Alteracao> alteracoes = new ArrayList<>();
        alteracoes.add(new Alteracao("status", atual.name(), StatusPedido.CANCELADO.name()));
        auditoriaService.registrar(TipoOperacao.CANCELAMENTO_PEDIDO, "PEDIDO", id, alteracoes, justificativa.trim());
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        if (percentual == null || percentual.signum() <= 0) {
            throw new IllegalArgumentException("Informe um desconto maior que zero.");
        }
        if (percentual.compareTo(DESCONTO_MAXIMO) > 0) {
            throw new IllegalArgumentException("O desconto máximo é de 20%.");
        }

        Pedido pedido = buscar(id);
        StatusPedido atual = statusDe(pedido);

        if (atual == StatusPedido.CANCELADO) {
            throw new IllegalStateException("Pedido cancelado não pode mais ser alterado.");
        }
        if (!atual.antesDaSaida()) {
            throw new IllegalStateException("O desconto só pode ser aplicado antes da saída do pedido.");
        }

        BigDecimal descontoAntes = pedido.getDescontoPercentual();
        BigDecimal totalAntes = pedido.getValorTotal();

        pedido.setDescontoPercentual(percentual);
        recalcularTotal(pedido);
        Pedido salvo = pedidoRepository.save(pedido);

        List<Alteracao> alteracoes = new ArrayList<>();
        alteracoes.add(new Alteracao("descontoPercentual", descontoAntes.toPlainString(), percentual.toPlainString()));
        alteracoes.add(new Alteracao("valorTotal", totalAntes.toPlainString(), salvo.getValorTotal().toPlainString()));
        auditoriaService.registrar(TipoOperacao.DESCONTO_PEDIDO, "PEDIDO", id, alteracoes, null);
        return salvo;
    }

    // ------------------------------------------------------------
    // Métodos de apoio
    // ------------------------------------------------------------
    private Pedido buscar(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado."));
    }

    private StatusPedido statusDe(Pedido pedido) {
        try {
            return StatusPedido.deTexto(pedido.getStatus());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Pedido com status antigo/desconhecido: " + pedido.getStatus());
        }
    }

    // total = subtotal - desconto%
    private void recalcularTotal(Pedido pedido) {
        BigDecimal fator = CEM.subtract(pedido.getDescontoPercentual()).divide(CEM);
        pedido.setValorTotal(pedido.getSubtotal().multiply(fator).setScale(2, RoundingMode.HALF_UP));
    }

    private String resumoItens(List<ItemPedido> itens) {
        StringBuilder sb = new StringBuilder();
        for (ItemPedido item : itens) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(item.getQuantidade()).append("x ").append(item.getNome());
        }
        return sb.toString();
    }
}
