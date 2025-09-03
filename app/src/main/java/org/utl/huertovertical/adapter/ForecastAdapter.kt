package org.utl.huertovertical.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.utl.huertovertical.R
import org.utl.huertovertical.data.DailyForecast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ForecastAdapter(private val forecastList: List<DailyForecast>) :
    RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {

    class ForecastViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tvForecastDay)
        val tvDescription: TextView = itemView.findViewById(R.id.tvForecastDescription)
        val tvIcon: ImageView = itemView.findViewById(R.id.tvForecastIcon)
        val tvMaxTemp: TextView = itemView.findViewById(R.id.tvForecastMaxTemp)
        val tvMinTemp: TextView = itemView.findViewById(R.id.tvForecastMinTemp)
        val tvPop: TextView = itemView.findViewById(R.id.tvForecastPop)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val currentItem = forecastList[position]

        val dayFormat = SimpleDateFormat("EEE, MMM dd", Locale("es", "ES"))
        holder.tvDay.text = dayFormat.format(Date(currentItem.dt * 1000L)).capitalize(Locale("es", "ES"))

        // descripcion del clima
        holder.tvDescription.text = currentItem.weather.firstOrNull()?.description?.capitalize(Locale("es", "ES")) ?: "N/A"

        val iconName = currentItem.weather.firstOrNull()?.icon
        if (iconName != null) {
            val iconResourceId = holder.itemView.context.resources.getIdentifier("icon_$iconName", "drawable", holder.itemView.context.packageName)
            if (iconResourceId != 0) {
                holder.tvIcon.setImageResource(iconResourceId)
            } else {
                holder.tvIcon.setImageResource(R.drawable.ic_weather_placeholder)
            }
        } else {
            holder.tvIcon.setImageResource(R.drawable.ic_weather_placeholder)
        }

        // Temperaturas
        holder.tvMaxTemp.text = "${currentItem.temp.max.toInt()}°C"
        holder.tvMinTemp.text = "${currentItem.temp.min.toInt()}°C"

        // Probabilidad de precipitacion
        val popPercentage = (currentItem.pop * 100).toInt()
        holder.tvPop.text = "Prob. Lluvia: $popPercentage%"
    }

    override fun getItemCount(): Int {
        return forecastList.size
    }
}