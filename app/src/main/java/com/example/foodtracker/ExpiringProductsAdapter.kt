package com.example.foodtracker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.databinding.ItemInventoryCompactBinding
import com.example.foodtracker.model.Product
import java.text.SimpleDateFormat
import java.util.Locale

class ExpiringProductsAdapter(private val onItemClicked: (Product) -> Unit) :
    ListAdapter<Product, ExpiringProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault()) // Short date format

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemInventoryCompactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    inner class ProductViewHolder(private val binding: ItemInventoryCompactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClicked(getItem(position))
                }
            }
        }

        fun bind(product: Product) {
            binding.productNameTextViewCompact.text = product.productName
            product.expirationDate?.let {
                binding.expirationDateTextViewCompact.text = "${dateFormat.format(it.toDate())}"
            } ?: run {
                binding.expirationDateTextViewCompact.text = "No Expiry Date"
            }

            // TODO: Set category icon based on product.category
            // For now, using a placeholder
            binding.categoryIconImageViewCompact.setImageResource(R.drawable.ic_default) // Placeholder
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}