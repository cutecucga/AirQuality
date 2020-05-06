package com.lanhnh.airquality.screen.air

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lanhnh.airquality.R
import com.lanhnh.airquality.data.Air
import kotlinx.android.synthetic.main.item.view.*

class AirAdapter(
    private val context: Context,
    private var listData: MutableList<Air>,
    private var onItemClick: ((Air?) -> Unit)?
) :
    RecyclerView.Adapter<AirAdapter.Companion.AirItem>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AirItem {
        val view = LayoutInflater.from(context).inflate(R.layout.item, parent, false)
        return AirItem(view, onItemClick)
    }

    override fun getItemCount(): Int {
        return listData.size
    }

    override fun onBindViewHolder(holder: AirItem, position: Int) {
        holder.bindData(listData[position])
    }

    companion object {
        class AirItem(itemView: View, private var onItemClick: ((Air?) -> Unit)?) :
            RecyclerView.ViewHolder(itemView) {

            fun bindData(item: Air) {
                itemView.run {
                    tv_time.text = item.time
                    tv_dust.text = item.dust.toString()
                    tv_air_quality.text = item.airQuality.toString()

                    setOnClickListener {
                        onItemClick?.invoke(item)
                    }
                }
            }
        }
    }
}
