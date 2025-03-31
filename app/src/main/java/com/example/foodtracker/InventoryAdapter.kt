
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.foodtracker.R
import com.example.foodtracker.model.InventoryItem
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class InventoryAdapter(
    private val inventoryList: List<InventoryItem>,
    private val onItemClick: (InventoryItem) -> Unit,
    private val onDeleteClick: (InventoryItem) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameTextView)
        val expirationDateTextView: TextView = itemView.findViewById(R.id.expirationDateTextView)
        val categoryIconImageView: ImageView = itemView.findViewById(R.id.categoryIconImageView)
        val cardView: CardView = itemView.findViewById(R.id.inventoryItemCardView)
        val deleteIconImageView: ImageView = itemView.findViewById(R.id.deleteIconImageView)

        fun bind(item: InventoryItem, onItemClick: (InventoryItem) -> Unit, onDeleteClick: (InventoryItem) -> Unit) {
            productNameTextView.text = item.productName
            expirationDateTextView.text = formatTimestamp(item.expirationDate)
            categoryIconImageView.setImageResource(item.iconResId)
            cardView.setOnClickListener { onItemClick(item) }
            deleteIconImageView.setOnClickListener { onDeleteClick(item) }

            val tintColor = getTintColor(itemView.context, item.expirationDate)
            cardView.setCardBackgroundColor(tintColor)
        }

        private fun formatTimestamp(timestamp: Timestamp?): String {
            if (timestamp == null) {
                return "No Expiration Date"
            }
            val date = timestamp.toDate()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return dateFormat.format(date)
        }

        private fun getTintColor(context: android.content.Context, timestamp: Timestamp?): Int {
            if (timestamp == null) {
                return ContextCompat.getColor(context, R.color.default_background)
            }

            val expirationDate = timestamp.toDate()
            val now = Date()
            val timeDiff = expirationDate.time - now.time
            val daysRemaining = TimeUnit.MILLISECONDS.toDays(timeDiff)

            return when {
                daysRemaining < 0 -> ContextCompat.getColor(context, R.color.expired_tint)
                daysRemaining < 3 -> ContextCompat.getColor(context, R.color.near_expiry_tint)
                else -> ContextCompat.getColor(context, R.color.default_background)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.inventory_item, parent, false)
        return InventoryViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val currentItem = inventoryList[position]
        holder.bind(currentItem, onItemClick, onDeleteClick)
    }

    override fun getItemCount() = inventoryList.size
}