import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapitest.R
import com.example.myapitest.model.CarDetails
import com.squareup.picasso.Picasso

class CarAdapter(
    private val cars: List<CarDetails>,
    private val itemClickListener: (CarDetails) -> Unit,
) : RecyclerView.Adapter<CarAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
        val tvModel: TextView = view.findViewById(R.id.tvModel)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
        val tvLicense: TextView = view.findViewById(R.id.tvLicense)
        val tvLat: TextView = view.findViewById(R.id.tvLat)
        val tvLong: TextView = view.findViewById(R.id.tvLong)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int = cars.size

    override fun onBindViewHolder(holder: ItemViewHolder, index: Int) {
        val car = cars[index]
        holder.itemView.setOnClickListener { itemClickListener.invoke(car) }

        holder.tvModel.text = car.name
        holder.tvYear.text = car.year
        holder.tvLicense.text = car.licence
        holder.tvLat.text = "Lat: %.4f".format(car.place.lat)
        holder.tvLong.text = "Long: %.4f".format(car.place.long)

        Picasso.get()
            .load(car.imageUrl)
            .placeholder(R.drawable.downloading)
            .error(R.drawable.directions_car)
            .into(holder.ivImage)
    }
}
