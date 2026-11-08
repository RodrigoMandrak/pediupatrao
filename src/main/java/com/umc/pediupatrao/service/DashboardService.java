package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.PedidoResumo;
import com.umc.pediupatrao.dto.PontoVenda;
import com.umc.pediupatrao.dto.ProdutoRanking;
import com.umc.pediupatrao.dto.ResumoVendas;
import com.umc.pediupatrao.dto.StatusQuantidade;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * Regras de cálculo do dashboard. Os pedidos do período são lidos do MongoDB
 * e os indicadores são calculados aqui, na camada de serviço.
 *
 * Regra de negócio: pedido CANCELADO não conta como venda (faturamento, ticket médio, ranking).
 */
@Service
public class DashboardService {

    private static final int PERIODO_PADRAO_DIAS = 30;
    private static final int PERIODO_MAXIMO_DIAS = 366;
    private static final int LIMITE_MAXIMO = 50;

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;

    public DashboardService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
    }

    // ------------------------------------------------------------
    // RF01 - indicadores gerais
    // ------------------------------------------------------------
    public ResumoVendas resumo(String inicio, String fim, String status, String produtoId) {
        LocalDate[] periodo = periodo(inicio, fim);
        List<Pedido> pedidos = buscar(periodo, status, produtoId);
        List<Pedido> validos = apenasVendas(pedidos);

        BigDecimal faturamento = BigDecimal.ZERO;
        BigDecimal descontos = BigDecimal.ZERO;
        long itens = 0;
        for (Pedido p : validos) {
            faturamento = faturamento.add(valor(p.getValorTotal()));
            descontos = descontos.add(valor(p.getSubtotal()).subtract(valor(p.getValorTotal())));
            for (ItemPedido item : itensDe(p)) {
                itens += item.getQuantidade();
            }
        }

        BigDecimal ticketMedio = validos.isEmpty()
                ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(BigDecimal.valueOf(validos.size()), 2, RoundingMode.HALF_UP);

        return new ResumoVendas(periodo[0], periodo[1], faturamento.setScale(2, RoundingMode.HALF_UP),
                pedidos.size(), validos.size(), pedidos.size() - validos.size(),
                ticketMedio, descontos.setScale(2, RoundingMode.HALF_UP), itens);
    }

    // ------------------------------------------------------------
    // RF02 - vendas ao longo do tempo (gráfico)
    // ------------------------------------------------------------
    public List<PontoVenda> vendasPorPeriodo(String inicio, String fim, String status, String produtoId,
            String agrupamento) {
        String agrupar = agrupamento == null || agrupamento.isBlank() ? "DIA" : agrupamento.trim().toUpperCase();
        if (!agrupar.equals("DIA") && !agrupar.equals("MES")) {
            throw new IllegalArgumentException("Agrupamento inválido. Use DIA ou MES.");
        }

        LocalDate[] periodo = periodo(inicio, fim);
        List<Pedido> validos = apenasVendas(buscar(periodo, status, produtoId));

        // TreeMap mantém os períodos em ordem cronológica
        Map<String, long[]> quantidade = new TreeMap<>();
        Map<String, BigDecimal> faturamento = new TreeMap<>();

        // Preenche os dias sem venda com zero, para o gráfico não "pular" datas
        if (agrupar.equals("DIA")) {
            for (LocalDate d = periodo[0]; !d.isAfter(periodo[1]); d = d.plusDays(1)) {
                quantidade.put(d.toString(), new long[] {0});
                faturamento.put(d.toString(), BigDecimal.ZERO);
            }
        }

        for (Pedido p : validos) {
            String chave = agrupar.equals("DIA")
                    ? p.getDataEntrada().toLocalDate().toString()
                    : YearMonth.from(p.getDataEntrada()).toString();
            quantidade.computeIfAbsent(chave, k -> new long[] {0})[0]++;
            faturamento.merge(chave, valor(p.getValorTotal()), BigDecimal::add);
        }

        List<PontoVenda> pontos = new ArrayList<>();
        for (Map.Entry<String, long[]> e : quantidade.entrySet()) {
            pontos.add(new PontoVenda(e.getKey(), e.getValue()[0],
                    faturamento.get(e.getKey()).setScale(2, RoundingMode.HALF_UP)));
        }
        return pontos;
    }

    // ------------------------------------------------------------
    // RF03 - situação dos pedidos
    // ------------------------------------------------------------
    public List<StatusQuantidade> pedidosPorStatus(String inicio, String fim, String produtoId) {
        List<Pedido> pedidos = buscar(periodo(inicio, fim), null, produtoId);

        // Começa com todos os status em zero, na ordem do fluxo
        Map<String, Long> contagem = new LinkedHashMap<>();
        for (StatusPedido s : StatusPedido.values()) {
            contagem.put(s.name(), 0L);
        }
        for (Pedido p : pedidos) {
            String s = p.getStatus() == null ? "DESCONHECIDO" : p.getStatus();
            contagem.merge(s, 1L, Long::sum);
        }

        List<StatusQuantidade> lista = new ArrayList<>();
        contagem.forEach((s, q) -> lista.add(new StatusQuantidade(s, q)));
        return lista;
    }

    public List<PedidoResumo> pedidosRecentes(String inicio, String fim, String status, String produtoId,
            Integer limite) {
        int max = validarLimite(limite, 10);
        List<Pedido> pedidos = new ArrayList<>(buscar(periodo(inicio, fim), status, produtoId));
        pedidos.sort(Comparator.comparing(Pedido::getDataEntrada).reversed());

        Map<String, String> nomes = new HashMap<>();
        for (Cliente c : clienteRepository.findAll()) {
            nomes.put(c.getId(), c.getNome());
        }

        List<PedidoResumo> lista = new ArrayList<>();
        for (Pedido p : pedidos) {
            if (lista.size() >= max) {
                break;
            }
            lista.add(new PedidoResumo(p.getId(), nomes.getOrDefault(p.getClienteId(), "—"),
                    p.getStatus(), valor(p.getValorTotal()), p.getDataEntrada()));
        }
        return lista;
    }

    // ------------------------------------------------------------
    // RF04 - produtos mais vendidos
    // ------------------------------------------------------------
    public List<ProdutoRanking> produtosMaisVendidos(String inicio, String fim, String status, Integer limite) {
        int max = validarLimite(limite, 5);
        List<Pedido> validos = apenasVendas(buscar(periodo(inicio, fim), status, null));

        Map<String, long[]> quantidades = new HashMap<>();
        Map<String, BigDecimal> faturamentos = new HashMap<>();
        Map<String, String> nomes = new HashMap<>();

        for (Pedido p : validos) {
            for (ItemPedido item : itensDe(p)) {
                String id = item.getProdutoId();
                nomes.put(id, item.getNome());
                quantidades.computeIfAbsent(id, k -> new long[] {0})[0] += item.getQuantidade();
                faturamentos.merge(id, item.getSubtotal(), BigDecimal::add);
            }
        }

        List<ProdutoRanking> ranking = new ArrayList<>();
        for (String id : quantidades.keySet()) {
            ranking.add(new ProdutoRanking(id, nomes.get(id), quantidades.get(id)[0],
                    faturamentos.get(id).setScale(2, RoundingMode.HALF_UP)));
        }
        // Mais vendido primeiro; empate resolvido pelo nome
        ranking.sort(Comparator.comparingLong(ProdutoRanking::quantidadeVendida).reversed()
                .thenComparing(ProdutoRanking::nome));

        return ranking.size() > max ? new ArrayList<>(ranking.subList(0, max)) : ranking;
    }

    // ------------------------------------------------------------
    // RF05 - filtros (validação e busca)
    // ------------------------------------------------------------

    // Converte e valida o período. Sem parâmetros: últimos 30 dias.
    LocalDate[] periodo(String inicio, String fim) {
        LocalDate dataFim = lerData(fim, "fim", LocalDate.now());
        LocalDate dataInicio = lerData(inicio, "inicio", dataFim.minusDays(PERIODO_PADRAO_DIAS - 1L));

        if (dataInicio.isAfter(dataFim)) {
            throw new IllegalArgumentException("A data inicial não pode ser maior que a data final.");
        }
        if (ChronoUnit.DAYS.between(dataInicio, dataFim) > PERIODO_MAXIMO_DIAS) {
            throw new IllegalArgumentException("O período máximo de consulta é de " + PERIODO_MAXIMO_DIAS + " dias.");
        }
        return new LocalDate[] {dataInicio, dataFim};
    }

    private LocalDate lerData(String texto, String nome, LocalDate padrao) {
        if (texto == null || texto.isBlank()) {
            return padrao;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida no parâmetro '" + nome + "'. Use o formato AAAA-MM-DD.");
        }
    }

    private List<Pedido> buscar(LocalDate[] periodo, String status, String produtoId) {
        StatusPedido filtroStatus = (status == null || status.isBlank()) ? null : StatusPedido.deTexto(status);

        LocalDateTime de = periodo[0].atStartOfDay();
        LocalDateTime ate = periodo[1].plusDays(1).atStartOfDay().minusNanos(1);

        List<Pedido> resultado = new ArrayList<>();
        for (Pedido p : pedidoRepository.findByDataEntradaBetween(de, ate)) {
            if (filtroStatus != null && !filtroStatus.name().equals(p.getStatus())) {
                continue;
            }
            if (produtoId != null && !produtoId.isBlank() && !contemProduto(p, produtoId.trim())) {
                continue;
            }
            resultado.add(p);
        }
        return resultado;
    }

    private boolean contemProduto(Pedido pedido, String produtoId) {
        for (ItemPedido item : itensDe(pedido)) {
            if (produtoId.equals(item.getProdutoId())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------
    private List<Pedido> apenasVendas(List<Pedido> pedidos) {
        List<Pedido> validos = new ArrayList<>();
        for (Pedido p : pedidos) {
            if (!StatusPedido.CANCELADO.name().equals(p.getStatus())) {
                validos.add(p);
            }
        }
        return validos;
    }

    private List<ItemPedido> itensDe(Pedido pedido) {
        return pedido.getItens() == null ? List.of() : pedido.getItens();
    }

    private BigDecimal valor(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private int validarLimite(Integer limite, int padrao) {
        if (limite == null) {
            return padrao;
        }
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new IllegalArgumentException("O limite deve estar entre 1 e " + LIMITE_MAXIMO + ".");
        }
        return limite;
    }
}
