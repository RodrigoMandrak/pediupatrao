package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.PedidoResumo;
import com.umc.pediupatrao.dto.PontoVenda;
import com.umc.pediupatrao.dto.ProdutoRanking;
import com.umc.pediupatrao.dto.ResumoVendas;
import com.umc.pediupatrao.dto.StatusQuantidade;
import com.umc.pediupatrao.service.DashboardService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de dados do dashboard. Somente leitura (GET) e restrita a ADMIN e GERENTE.
 * Filtros comuns: inicio e fim (AAAA-MM-DD), status e produtoId.
 */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public ResumoVendas resumo(@RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId) {
        return dashboardService.resumo(inicio, fim, status, produtoId);
    }

    @GetMapping("/vendas-por-periodo")
    public List<PontoVenda> vendasPorPeriodo(@RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId,
            @RequestParam(required = false) String agrupamento) {
        return dashboardService.vendasPorPeriodo(inicio, fim, status, produtoId, agrupamento);
    }

    @GetMapping("/pedidos-por-status")
    public List<StatusQuantidade> pedidosPorStatus(@RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim,
            @RequestParam(required = false) String produtoId) {
        return dashboardService.pedidosPorStatus(inicio, fim, produtoId);
    }

    @GetMapping("/produtos-mais-vendidos")
    public List<ProdutoRanking> produtosMaisVendidos(@RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limite) {
        return dashboardService.produtosMaisVendidos(inicio, fim, status, limite);
    }

    @GetMapping("/pedidos-recentes")
    public List<PedidoResumo> pedidosRecentes(@RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId,
            @RequestParam(required = false) Integer limite) {
        return dashboardService.pedidosRecentes(inicio, fim, status, produtoId, limite);
    }
}
