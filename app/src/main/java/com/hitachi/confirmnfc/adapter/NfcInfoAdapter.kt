package com.hitachi.confirmnfc.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hitachi.confirmnfc.databinding.ItemNfcInfoBinding
import com.hitachi.confirmnfc.model.MatchedItem

class NfcInfoAdapter :
    ListAdapter<MatchedItem, NfcInfoAdapter.VH>(DIFF) {

    class VH(val binding: ItemNfcInfoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNfcInfoBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.binding.item = getItem(position)
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MatchedItem>() {
            override fun areItemsTheSame(old: MatchedItem, new: MatchedItem) =
                old.codeValue == new.codeValue

            override fun areContentsTheSame(old: MatchedItem, new: MatchedItem) =
                old == new
        }
    }
}