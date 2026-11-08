package com.umc.pediupatrao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pedidos")
public class Pedido {

    @Id
    private String id;
    private String clienteId;
    private List<ItemPedido> itens = new ArrayList<>();

    // Valores calculados SEMPRE no servidor
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal descontoPercentual = BigDecimal.ZERO;
    private BigDecimal valorTotal = BigDecimal.ZERO;

    // Guardado como texto para não quebrar pedidos antigos que tinham outros status
    private String status;

    // Entrada
    private LocalDateTime dataEntrada;
    private String responsavelEntrada;

    // Saída (entrega ou retirada)
    private LocalDateTime dataSaida;
    private String responsavelSaida;

    // Cancelamento
    private LocalDateTime dataCancelamento;
    private String responsavelCancelamento;
    private String justificativaCancelamento;

    // Campos antigos, mantidos para compatibilidade com pedidos já salvos
    private String tipoPizza;
    private int quantidade;
    private String sabor;

    // ---- Métodos auxiliares usados pela tela ----

    @JsonIgnore
    public boolean isCancelavel() {
        return statusAtual() != null && statusAtual().antesDaSaida();
    }

    @JsonIgnore
    public List<String> getProximosStatus() {
        List<String> proximos = new ArrayList<>();
        StatusPedido atual = statusAtual();
        if (atual == null) {
            return proximos;
        }
        for (StatusPedido s : StatusPedido.values()) {
            if (s != StatusPedido.CANCELADO && atual.podeIrPara(s)) {
                proximos.add(s.name());
            }
        }
        return proximos;
    }

    private StatusPedido statusAtual() {
        try {
            return StatusPedido.deTexto(status);
        } catch (IllegalArgumentException e) {
            return null; // status antigo/desconhecido
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDescontoPercentual() { return descontoPercentual; }
    public void setDescontoPercentual(BigDecimal descontoPercentual) { this.descontoPercentual = descontoPercentual; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDateTime dataEntrada) { this.dataEntrada = dataEntrada; }

    public String getResponsavelEntrada() { return responsavelEntrada; }
    public void setResponsavelEntrada(String responsavelEntrada) { this.responsavelEntrada = responsavelEntrada; }

    public LocalDateTime getDataSaida() { return dataSaida; }
    public void setDataSaida(LocalDateTime dataSaida) { this.dataSaida = dataSaida; }

    public String getResponsavelSaida() { return responsavelSaida; }
    public void setResponsavelSaida(String responsavelSaida) { this.responsavelSaida = responsavelSaida; }

    public LocalDateTime getDataCancelamento() { return dataCancelamento; }
    public void setDataCancelamento(LocalDateTime dataCancelamento) { this.dataCancelamento = dataCancelamento; }

    public String getResponsavelCancelamento() { return responsavelCancelamento; }
    public void setResponsavelCancelamento(String responsavelCancelamento) { this.responsavelCancelamento = responsavelCancelamento; }

    public String getJustificativaCancelamento() { return justificativaCancelamento; }
    public void setJustificativaCancelamento(String justificativaCancelamento) { this.justificativaCancelamento = justificativaCancelamento; }

    public String getTipoPizza() { return tipoPizza; }
    public void setTipoPizza(String tipoPizza) { this.tipoPizza = tipoPizza; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public String getSabor() { return sabor; }
    public void setSabor(String sabor) { this.sabor = sabor; }
}
