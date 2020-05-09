package com.lanhnh.airquality.screen.air

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lanhnh.airquality.R
import com.lanhnh.airquality.data.Air
import kotlinx.android.synthetic.main.item.view.*

class AirAdapter(
    private val context: Context,
    private var listData: MutableList<Air>,
    private var onItemClick: ((Air?) -> Unit)?
) : RecyclerView.Adapter<AirAdapter.Companion.AirItem>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AirItem {
        val view = LayoutInflater.from(context).inflate(R.layout.item, parent, false)
        return AirItem(view, onItemClick)
    }

    fun setData(data: MutableList<Air>) {
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }

    public fun insert(data: Air, position: Int = FIRST) {
        listData.add(position, data)
        notifyItemRangeInserted(position, 1)
    }

    override fun getItemCount(): Int {
        return listData.size
    }

    override fun onBindViewHolder(holder: AirItem, position: Int) {
        holder.bindData(listData[position])
    }

    companion object {
        const val FIRST = 0
        const val WARNING_DUST = 2
        const val WARNING_AIR_QUALITY = 2

        class AirItem(itemView: View, private var onItemClick: ((Air?) -> Unit)?) :
            RecyclerView.ViewHolder(itemView) {

            fun bindData(item: Air) {
                itemView.run {
                    tv_time.text = item.time
                    tv_dust.text = item.dust.toString()
                    tv_air_quality.text = item.airQuality.toString()
                    ll_bg_item.background =
                        if (item.dust >= WARNING_DUST || item.airQuality >= WARNING_AIR_QUALITY)
                            ContextCompat.getDrawable(context, R.drawable.bg_orange)
                        else ContextCompat.getDrawable(context, R.drawable.bg_purple)
                    setOnClickListener {
                        onItemClick?.invoke(item)
                    }
                }
            }
        }
    }
}
