package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Testes das regras de negócio do pedido (sem banco: repositórios simulados com Mockito). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private PedidoService pedidoService;

    @BeforeEach
    void preparar() {
        pedidoService = new PedidoService(pedidoRepository, produtoRepository, clienteRepository, auditoriaService);
        // o "save" simulado devolve o próprio objeto salvo
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Pedido pedidoComStatus(String status) {
        Pedido pedido = new Pedido();
        pedido.setId("p1");
        pedido.setStatus(status);
        pedido.setSubtotal(new BigDecimal("100.00"));
        pedido.setDescontoPercentual(BigDecimal.ZERO);
        pedido.setValorTotal(new BigDecimal("100.00"));
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));
        return pedido;
    }

    @Test
    void criarPedidoCalculaTotalNoServidor() {
        Produto pizza = new Produto();
        pizza.setId("prod1");
        pizza.setNome("Calabresa");
        pizza.setPreco(40.0);
        pizza.setAtivo(true);
        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(produtoRepository.findById("prod1")).thenReturn(Optional.of(pizza));

        // A requisição tenta "forçar" total e status; o serviço deve ignorar
        Pedido requisicao = new Pedido();
        requisicao.setClienteId("c1");
        requisicao.setStatus("FINALIZADO");
        requisicao.setValorTotal(new BigDecimal("1.00"));
        requisicao.getItens().add(new ItemPedido("prod1", "qualquer", new BigDecimal("0.01"), 2));

        Pedido criado = pedidoService.criarPedido(requisicao);

        assertEquals(new BigDecimal("80.00"), criado.getValorTotal());
        assertEquals("RECEBIDO", criado.getStatus());
        assertNotNull(criado.getDataEntrada());
        verify(auditoriaService).registrar(any(TipoOperacao.class), any(), any(), any(), any());
    }

    @Test
    void criarPedidoSemItensEhRejeitado() {
        when(clienteRepository.existsById("c1")).thenReturn(true);
        Pedido requisicao = new Pedido();
        requisicao.setClienteId("c1");
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(requisicao));
    }

    @Test
    void fluxoCompletoDeStatus() {
        Pedido pedido = pedidoComStatus("RECEBIDO");
        pedidoService.atualizarStatus("p1", "EM_PREPARACAO");
        pedidoService.atualizarStatus("p1", "PRONTO");
        pedidoService.atualizarStatus("p1", "RETIRADO");
        assertNotNull(pedido.getDataSaida());
        assertNotNull(pedido.getResponsavelSaida());
        pedidoService.atualizarStatus("p1", "FINALIZADO");
        assertEquals("FINALIZADO", pedido.getStatus());
    }

    @Test
    void naoPodePularEtapas() {
        pedidoComStatus("RECEBIDO");
        assertThrows(IllegalStateException.class, () -> pedidoService.atualizarStatus("p1", "PRONTO"));
    }

    @Test
    void naoPodeFinalizarPedidoEmPreparacao() {
        pedidoComStatus("EM_PREPARACAO");
        assertThrows(IllegalStateException.class, () -> pedidoService.atualizarStatus("p1", "FINALIZADO"));
    }

    @Test
    void statusCancelamentoNaoPassaPeloFluxoNormal() {
        pedidoComStatus("RECEBIDO");
        assertThrows(IllegalArgumentException.class, () -> pedidoService.atualizarStatus("p1", "CANCELADO"));
    }

    @Test
    void descontoValidoRecalculaTotal() {
        Pedido pedido = pedidoComStatus("RECEBIDO");
        pedidoService.aplicarDesconto("p1", new BigDecimal("10"));
        assertEquals(new BigDecimal("90.00"), pedido.getValorTotal());
    }

    @Test
    void descontoAcimaDeVintePorCentoEhRejeitado() {
        pedidoComStatus("RECEBIDO");
        assertThrows(IllegalArgumentException.class, () -> pedidoService.aplicarDesconto("p1", new BigDecimal("20.01")));
    }

    @Test
    void descontoDepoisDaSaidaEhRejeitado() {
        pedidoComStatus("SAIU_PARA_ENTREGA");
        assertThrows(IllegalStateException.class, () -> pedidoService.aplicarDesconto("p1", new BigDecimal("5")));
    }

    @Test
    void cancelamentoExigeJustificativa() {
        pedidoComStatus("RECEBIDO");
        assertThrows(IllegalArgumentException.class, () -> pedidoService.cancelarPedido("p1", "  "));
    }

    @Test
    void cancelamentoGuardaDadosEBloqueiaAlteracoes() {
        Pedido pedido = pedidoComStatus("EM_PREPARACAO");
        pedidoService.cancelarPedido("p1", "Cliente desistiu");

        assertEquals("CANCELADO", pedido.getStatus());
        assertEquals("Cliente desistiu", pedido.getJustificativaCancelamento());
        assertEquals(new BigDecimal("100.00"), pedido.getSubtotal()); // dados históricos preservados
        assertThrows(IllegalStateException.class, () -> pedidoService.atualizarStatus("p1", "PRONTO"));
        assertThrows(IllegalStateException.class, () -> pedidoService.aplicarDesconto("p1", new BigDecimal("5")));
    }

    @Test
    void naoCancelaPedidoQueJaSaiu() {
        pedidoComStatus("RETIRADO");
        assertThrows(IllegalStateException.class, () -> pedidoService.cancelarPedido("p1", "tarde demais"));
    }
}
