package br.com.demo.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
public class MigrationTest {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationCriaTodasAsTabelas() {
        List<String> tabelas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );
        assertThat(tabelas).contains(
                "users", "category", "supplier", "product", "stock_movement", "stock_balance"
        );
    }

    @Test
    void movimentacaoConfirmadaEImutavel() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID movementId = UUID.randomUUID();

        jdbc.update("INSERT INTO users (id, name, email, password_hash, role) VALUES (?, 'T', 't@t.com', 'x', 'MANAGER')", userId);
        jdbc.update("INSERT INTO product (id, sku, name, unit) VALUES (?, 'SKU-1', 'Produto', 'UN')", productId);
        jdbc.update("INSERT INTO stock_movement (id, product_id, type, quantity, reason, actor_id) VALUES (?, ?, 'ENTRY', 5, 'teste', ?)",
                movementId, productId, userId);

        assertThatThrownBy(() -> jdbc.update("UPDATE stock_movement SET quantity = 99 WHERE id = ?", movementId))
                .hasMessageContaining("imutável");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM stock_movement WHERE id = ?", movementId))
                .hasMessageContaining("imutável");
    }

    @Test
    void skuUnicoIgnoraEspacosEMaiusculas() {
        jdbc.update("INSERT INTO product (sku, name, unit) VALUES ('ABC-1', 'A', 'UN')");

        assertThatThrownBy(() -> jdbc.update("INSERT INTO product (sku, name, unit) VALUES ('  abc-1 ', 'B', 'UN')"))
                .hasMessageContaining("ux_product_sku");
    }

    @Test
    void saldoNaoPodeFicarNegativo() {
        UUID productId = UUID.randomUUID();
        jdbc.update("INSERT INTO product (id, sku, name, unit) VALUES (?, 'SKU-NEG', 'P', 'UN')", productId);

        assertThatThrownBy(() -> jdbc.update("INSERT INTO stock_balance (product_id, quantity) VALUES (?, -1)", productId))
                .hasMessageContaining("quantity");
    }
}
