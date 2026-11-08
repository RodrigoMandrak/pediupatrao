package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.PontoVenda;
import com.umc.pediupatrao.dto.ProdutoRanking;
import com.umc.pediupatrao.dto.ResumoVendas;
import com.umc.pediupatrao.dto.StatusQuantidade;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Testes dos cálculos do dashboard (repositórios simulados com Mockito, sem banco). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;

    private DashboardService service;

    private static final String INICIO = "2026-11-01";
    private static final String FIM = "2026-11-03";

    @BeforeEach
    void preparar() {
        service = new DashboardService(pedidoRepository, clienteRepository);
        // 3 pedidos: dois válidos (100,00 e 90,00 com 10% de desconto) e um cancelado (50,00)
        Pedido a = pedido("a", "FINALIZADO", "100.00", "100.00", "0", dia(1), item("p1", "Calabresa", "50.00", 2));
        Pedido b = pedido("b", "RECEBIDO", "100.00", "90.00", "10", dia(3),
                item("p1", "Calabresa", "50.00", 1), item("p2", "Coca", "50.00", 1));
        Pedido c = pedido("c", "CANCELADO", "50.00", "50.00", "0", dia(3), item("p2", "Coca", "50.00", 1));
        when(pedidoRepository.findByDataEntradaBetween(any(), any())).thenReturn(List.of(a, b, c));
    }

    @Test
    void resumoNaoContaPedidoCancelado() {
        ResumoVendas r = service.resumo(INICIO, FIM, null, null);
        assertEquals(new BigDecimal("190.00"), r.faturamentoTotal());
        assertEquals(3, r.totalPedidos());
        assertEquals(2, r.pedidosValidos());
        assertEquals(1, r.pedidosCancelados());
        assertEquals(new BigDecimal("95.00"), r.ticketMedio());
        assertEquals(new BigDecimal("10.00"), r.descontosConcedidos());
        assertEquals(4, r.itensVendidos());
    }

    @Test
    void resumoSemPedidosTemTicketZero() {
        when(pedidoRepository.findByDataEntradaBetween(any(), any())).thenReturn(List.of());
        ResumoVendas r = service.resumo(INICIO, FIM, null, null);
        assertEquals(0, r.ticketMedio().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.pedidosValidos());
    }

    @Test
    void filtroDeStatusFunciona() {
        ResumoVendas r = service.resumo(INICIO, FIM, "RECEBIDO", null);
        assertEquals(1, r.totalPedidos());
        assertEquals(new BigDecimal("90.00"), r.faturamentoTotal());
    }

    @Test
    void filtroDeProdutoFunciona() {
        ResumoVendas r = service.resumo(INICIO, FIM, null, "p2");
        assertEquals(2, r.totalPedidos()); // pedidos b e c têm a Coca
    }

    @Test
    void vendasPorDiaPreencheDiasSemVenda() {
        List<PontoVenda> pontos = service.vendasPorPeriodo(INICIO, FIM, null, null, "DIA");
        assertEquals(3, pontos.size());
        assertEquals("2026-11-02", pontos.get(1).periodo());
        assertEquals(0, pontos.get(1).pedidos());
        assertEquals(new BigDecimal("100.00"), pontos.get(0).faturamento());
        assertEquals(new BigDecimal("90.00"), pontos.get(2).faturamento()); // cancelado fora
    }

    @Test
    void vendasPorMesAgrupaNoMes() {
        List<PontoVenda> pontos = service.vendasPorPeriodo(INICIO, FIM, null, null, "mes");
        assertEquals(1, pontos.size());
        assertEquals("2026-11", pontos.get(0).periodo());
        assertEquals(2, pontos.get(0).pedidos());
    }

    @Test
    void agrupamentoInvalidoEhRejeitado() {
        assertThrows(IllegalArgumentException.class,
                () -> service.vendasPorPeriodo(INICIO, FIM, null, null, "SEMANA"));
    }

    @Test
    void pedidosPorStatusListaTodosOsStatus() {
        List<StatusQuantidade> lista = service.pedidosPorStatus(INICIO, FIM, null);
        assertEquals(7, lista.size());
        assertEquals(1, lista.stream().filter(s -> s.status().equals("CANCELADO")).findFirst().get().quantidade());
        assertEquals(0, lista.stream().filter(s -> s.status().equals("PRONTO")).findFirst().get().quantidade());
    }

    @Test
    void rankingOrdenaPorQuantidadeEIgnoraCancelados() {
        List<ProdutoRanking> ranking = service.produtosMaisVendidos(INICIO, FIM, null, 5);
        assertEquals("Calabresa", ranking.get(0).nome());
        assertEquals(3, ranking.get(0).quantidadeVendida());
        assertEquals("Coca", ranking.get(1).nome());
        assertEquals(1, ranking.get(1).quantidadeVendida()); // a Coca do pedido cancelado não conta
    }

    @Test
    void rankingRespeitaOLimite() {
        assertEquals(1, service.produtosMaisVendidos(INICIO, FIM, null, 1).size());
    }

    @Test
    void parametrosInvalidosSaoRejeitados() {
        assertThrows(IllegalArgumentException.class, () -> service.resumo("01/11/2026", FIM, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.resumo(FIM, INICIO, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.resumo("2020-01-01", "2026-11-01", null, null));
        assertThrows(IllegalArgumentException.class, () -> service.resumo(INICIO, FIM, "NAO_EXISTE", null));
        assertThrows(IllegalArgumentException.class, () -> service.produtosMaisVendidos(INICIO, FIM, null, 0));
        assertThrows(IllegalArgumentException.class, () -> service.produtosMaisVendidos(INICIO, FIM, null, 51));
    }

    @Test
    void semParametrosUsaUltimos30Dias() {
        LocalDate[] periodo = service.periodo(null, null);
        assertEquals(LocalDate.now(), periodo[1]);
        assertEquals(LocalDate.now().minusDays(29), periodo[0]);
    }

    // ---- Auxiliares para montar os pedidos de teste ----
    private LocalDateTime dia(int dia) {
        return LocalDateTime.of(2026, 11, dia, 12, 0);
    }

    private ItemPedido item(String produtoId, String nome, String preco, int quantidade) {
        return new ItemPedido(produtoId, nome, new BigDecimal(preco), quantidade);
    }

    private Pedido pedido(String id, String status, String subtotal, String total, String desconto,
            LocalDateTime entrada, ItemPedido... itens) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setStatus(status);
        p.setSubtotal(new BigDecimal(subtotal));
        p.setValorTotal(new BigDecimal(total));
        p.setDescontoPercentual(new BigDecimal(desconto));
        p.setDataEntrada(entrada);
        p.setItens(List.of(itens));
        return p;
    }
}
