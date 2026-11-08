package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gera pedidos de exemplo dos últimos 30 dias para demonstrar o dashboard.
 * Só roda se application.properties tiver: pediupatrao.dados-demo=true
 * e apenas uma vez (se já existir pedido de demonstração, não cria de novo).
 */
@Configuration
@ConditionalOnProperty(name = "pediupatrao.dados-demo", havingValue = "true")
public class DadosDemoInitializer {

    private static final String MARCA_DEMO = "demo";
    private static final String[] STATUS = {"FINALIZADO", "FINALIZADO", "FINALIZADO", "RETIRADO",
        "SAIU_PARA_ENTREGA", "PRONTO", "EM_PREPARACAO", "RECEBIDO", "CANCELADO"};

    @Bean
    CommandLineRunner gerarDadosDemo(PedidoRepository pedidos, ProdutoRepository produtos, ClienteRepository clientes) {
        return args -> {
            boolean jaTemDemo = pedidos.findAll().stream().anyMatch(p -> MARCA_DEMO.equals(p.getResponsavelEntrada()));
            if (jaTemDemo) {
                return;
            }

            List<Produto> listaProdutos = produtos.findAll();
            if (listaProdutos.isEmpty()) {
                listaProdutos = criarProdutos(produtos);
            }
            List<Cliente> listaClientes = clientes.findAll();
            if (listaClientes.isEmpty()) {
                listaClientes = criarClientes(clientes);
            }

            Random sorteio = new Random(42); // semente fixa: mesmos dados a cada execução
            for (int i = 0; i < 80; i++) {
                pedidos.save(novoPedido(sorteio, listaProdutos, listaClientes));
            }
            System.out.println("Dados de demonstração do dashboard criados (80 pedidos).");
        };
    }

    private Pedido novoPedido(Random sorteio, List<Produto> produtos, List<Cliente> clientes) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(clientes.get(sorteio.nextInt(clientes.size())).getId());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<ItemPedido> itens = new ArrayList<>();
        int quantidadeItens = 1 + sorteio.nextInt(3);
        for (int j = 0; j < quantidadeItens; j++) {
            Produto produto = produtos.get(sorteio.nextInt(produtos.size()));
            ItemPedido item = new ItemPedido(produto.getId(), produto.getNome(),
                    BigDecimal.valueOf(produto.getPreco()), 1 + sorteio.nextInt(3));
            itens.add(item);
            subtotal = subtotal.add(item.getSubtotal());
        }
        pedido.setItens(itens);
        pedido.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));

        // Alguns pedidos recebem desconto de 5% a 20%
        BigDecimal desconto = sorteio.nextInt(5) == 0 ? BigDecimal.valueOf(5 * (1 + sorteio.nextInt(4))) : BigDecimal.ZERO;
        pedido.setDescontoPercentual(desconto);
        pedido.setValorTotal(pedido.getSubtotal()
                .multiply(new BigDecimal("100").subtract(desconto).divide(new BigDecimal("100")))
                .setScale(2, RoundingMode.HALF_UP));

        pedido.setStatus(STATUS[sorteio.nextInt(STATUS.length)]);
        pedido.setDataEntrada(LocalDateTime.now().minusDays(sorteio.nextInt(30))
                .withHour(11 + sorteio.nextInt(12)).withMinute(sorteio.nextInt(60)));
        pedido.setResponsavelEntrada(MARCA_DEMO);
        pedido.setQuantidade(itens.size());
        pedido.setSabor(itens.get(0).getNome());
        return pedido;
    }

    private List<Produto> criarProdutos(ProdutoRepository repo) {
        String[][] dados = {{"Pizza Calabresa", "Pizza", "42.00"}, {"Pizza Mussarela", "Pizza", "38.00"},
            {"Pizza Portuguesa", "Pizza", "46.00"}, {"Esfiha de Carne", "Esfiha", "6.50"},
            {"Coca-Cola 2L", "Bebida", "12.00"}, {"X-Burger", "Lanche", "22.00"}};
        List<Produto> lista = new ArrayList<>();
        for (String[] d : dados) {
            Produto p = new Produto();
            p.setNome(d[0]);
            p.setTipo(d[1]);
            p.setPreco(Double.valueOf(d[2]));
            p.setAtivo(true);
            lista.add(repo.save(p));
        }
        return lista;
    }

    private List<Cliente> criarClientes(ClienteRepository repo) {
        String[] nomes = {"Maria Souza", "João Lima", "Ana Costa", "Carlos Pereira"};
        List<Cliente> lista = new ArrayList<>();
        for (String nome : nomes) {
            Cliente c = new Cliente();
            c.setNome(nome);
            c.setTelefone("(11) 90000-0000");
            lista.add(repo.save(c));
        }
        return lista;
    }
}
