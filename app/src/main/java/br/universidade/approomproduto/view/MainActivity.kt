package br.universidade.approomproduto.view

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.databinding.ActivityMainBinding
import br.universidade.approomproduto.db.AppDatabase
import br.universidade.approomproduto.model.Product
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        )
            .fallbackToDestructiveMigration() // A8: recria o banco quando a versão muda (apaga os dados)
            .build()
        productDao = db.productDao()

        // ---------- Inserir (A1, A2 e A8) ----------
        binding.btnInserirProduto.setOnClickListener {
            val name = binding.editProductName.text.toString().trim()
            val priceText = binding.editProductPrice.text.toString().trim()
            val description = binding.editProductDescription.text.toString().trim()
            val quantity = binding.editProductQuantity.text.toString().toIntOrNull() ?: 0

            if (name.isEmpty()) {
                binding.editProductName.error = "Informe o nome"
                return@setOnClickListener
            }

            // A2: preço vazio, zero ou negativo não pode ser salvo
            val price = priceText.replace(",", ".").toDoubleOrNull()
            if (price == null || price <= 0.0) {
                binding.editProductPrice.error = "Informe um preço válido"
                return@setOnClickListener
            }

            val product = Product(
                name = name,
                price = price,
                description = description,
                quantity = quantity
            )

            lifecycleScope.launch {
                productDao.insertAll(product)
                Log.i("TAG", "Produto inserido: $name - R\$$price")
                Toast.makeText(this@MainActivity, "Produto inserido!", Toast.LENGTH_SHORT).show()

                // A1: limpa os campos e devolve o cursor ao campo Nome
                binding.editProductName.text.clear()
                binding.editProductPrice.text.clear()
                binding.editProductDescription.text.clear()
                binding.editProductQuantity.text.clear()
                binding.editProductName.requestFocus()
            }
        }

        // ---------- Listar (A3 e A4) ----------
        binding.btnListarProdutos.setOnClickListener {
            lifecycleScope.launch {
                val products = productDao.getAll()
                products.forEach {
                    Log.i(
                        "TAG",
                        "Produto = ${it.name}, Preço = R\$${it.price}, " +
                            "Qtd = ${it.quantity}, Descrição = ${it.description}"
                    )
                }
                mostrarProdutos(products)
            }
        }

        // ---------- Buscar por nome (A5) ----------
        binding.btnBuscar.setOnClickListener {
            val texto = binding.editBusca.text.toString().trim()
            lifecycleScope.launch {
                val products = productDao.buscarPorNome("%$texto%")
                mostrarProdutos(products)
            }
        }

        // ---------- Atualizar preço (A7) ----------
        binding.btnAtualizarPreco.setOnClickListener {
            val id = binding.editProductId.text.toString().toIntOrNull()
            val novoPreco = binding.editNovoPreco.text.toString().replace(",", ".").toDoubleOrNull()

            if (id == null) {
                binding.editProductId.error = "Informe o id"
                return@setOnClickListener
            }
            if (novoPreco == null || novoPreco <= 0.0) {
                binding.editNovoPreco.error = "Informe um preço válido"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val produto = productDao.getById(id)
                if (produto == null) {
                    Toast.makeText(this@MainActivity, "Produto não encontrado", Toast.LENGTH_SHORT).show()
                } else {
                    productDao.update(produto.copy(price = novoPreco))
                    Toast.makeText(this@MainActivity, "Preço atualizado!", Toast.LENGTH_SHORT).show()
                    mostrarProdutos(productDao.getAll())
                }
            }
        }

        // ---------- Excluir pelo id (A6, desafio extra) ----------
        binding.btnExcluirPorId.setOnClickListener {
            val id = binding.editProductId.text.toString().toIntOrNull()
            if (id == null) {
                binding.editProductId.error = "Informe o id"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val produto = productDao.getById(id)
                if (produto == null) {
                    Toast.makeText(this@MainActivity, "Produto não encontrado", Toast.LENGTH_SHORT).show()
                } else {
                    productDao.delete(produto)
                    Toast.makeText(this@MainActivity, "Produto excluído!", Toast.LENGTH_SHORT).show()
                    mostrarProdutos(productDao.getAll())
                }
            }
        }

        // ---------- Excluir todos (A6) ----------
        binding.btnExcluirTodos.setOnClickListener {
            lifecycleScope.launch {
                productDao.deleteAll()
                Toast.makeText(this@MainActivity, "Todos os produtos foram excluídos", Toast.LENGTH_SHORT).show()
                mostrarProdutos(emptyList())
            }
        }
    }

    // A3 e A4: mostra a lista no TextView, junto da quantidade de produtos e da soma dos preços
    private fun mostrarProdutos(products: List<Product>) {
        if (products.isEmpty()) {
            binding.txtResultado.text = "Nenhum produto encontrado."
            return
        }

        val linhas = products.joinToString("\n") {
            "#${it.id} ${it.name} - ${formatarPreco(it.price)} - Qtd: ${it.quantity}\n" +
                "     ${it.description ?: ""}"
        }
        val total = products.sumOf { it.price }

        binding.txtResultado.text =
            "$linhas\n\n" +
                "Produtos cadastrados: ${products.size}\n" +
                "Soma dos preços: ${formatarPreco(total)}"
    }

    private fun formatarPreco(valor: Double): String =
        "R$ " + String.format(Locale("pt", "BR"), "%.2f", valor)
}
