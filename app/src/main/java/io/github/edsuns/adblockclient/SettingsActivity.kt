package io.github.edsuns.adblockclient

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.edsuns.adblockclient.databinding.ActivitySettingsBinding
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.DownloadState
import io.github.edsuns.adfilter.Filter
import io.github.edsuns.adfilter.FilterViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private lateinit var viewModel: FilterViewModel

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)

    private lateinit var addFilterDialog: Dialog
    private lateinit var dialogView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = AdFilter.get().viewModel

        val recyclerView = binding.filterRecyclerView
        val adapter = FilterListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter.data = viewModel.filters.value?.values!!.toList()
        viewModel.filters.observe(this, {
            adapter.data = it.values.toList()
        })

        val enable = viewModel.isEnabled.value ?: false
        binding.enableSwitch.isChecked = enable
        setVisible(recyclerView, enable)
        binding.enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.isEnabled.value = isChecked
            setVisible(recyclerView, isChecked)
        }

        dialogView = layoutInflater.inflate(R.layout.dialog_add_filter, LinearLayout(this))
        addFilterDialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_filter)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ ->
                val nameEdit: EditText = dialogView.findViewById(R.id.filterNameEdit)
                val urlEdit: EditText = dialogView.findViewById(R.id.filterUrlEdit)
                val url = urlEdit.text.toString()
                if (nameEdit.text.isNotBlank() && urlEdit.text.isNotBlank()
                    && URLUtil.isNetworkUrl(url)
                ) {
                    val filter =
                        viewModel.addFilter(nameEdit.text.toString(), url)
                    viewModel.download(filter.id)
                } else {
                    Toast.makeText(
                        this,
                        R.string.invalid_name_or_url, Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setView(dialogView)
            .create()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        val nameEdit: EditText = dialogView.findViewById(R.id.filterNameEdit)
        val urlEdit: EditText = dialogView.findViewById(R.id.filterUrlEdit)
        nameEdit.setText("")
        urlEdit.setText("")
        addFilterDialog.show()
        return true
    }

    private fun setVisible(view: View, visible: Boolean) {
        if (visible)
            view.visibility = View.VISIBLE
        else
            view.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    inner class FilterListAdapter : RecyclerView.Adapter<FilterViewHolder>(),
        DialogInterface.OnClickListener {
        private lateinit var filterList: List<Filter>

        var data
            get() = filterList
            set(value) {
                filterList = value
                notifyDataSetChanged()
            }

        var selectedId: String? = null

        private val dialog: Dialog by lazy {
            AlertDialog.Builder(this@SettingsActivity)
                .setItems(R.array.filter_detail_items, this)
                .create()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
            val itemView = layoutInflater.inflate(R.layout.filter_item, parent, false)
            return FilterViewHolder(this, itemView)
        }

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            filterList[position].let { filter ->
                holder.filterName.text = filter.name
                holder.filterUrl.text = filter.url
                holder.filterUpdateTime.text = dateFormatter.format(Date(filter.updateTime))
                holder.switch.isChecked = filter.isEnabled
                when (filter.downloadState) {
                    DownloadState.DOWNLOADING -> holder.filterDownloadState.text =
                        getString(R.string.downloading)
                    DownloadState.FAILED -> holder.filterDownloadState.text =
                        getString(R.string.failed_to_download)
                    else -> {
                        holder.switch.isEnabled = filter.hasDownloaded()
                        holder.filterDownloadState.text =
                            if (filter.hasDownloaded()) "" else getString(R.string.not_downloaded)
                    }
                }
                holder.itemView.setOnClickListener {
                    selectedId = filter.id
                    dialog.show()
                }
            }
        }

        override fun getItemCount(): Int = filterList.size
        override fun onClick(dialog: DialogInterface?, which: Int) {
            selectedId?.let {
                when (which) {
                    0 -> viewModel.download(selectedId!!)
                    1 -> viewModel.removeFilter(selectedId!!)
                }
            }
        }
    }

    inner class FilterViewHolder(private val adapter: FilterListAdapter, itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val filterName: TextView by lazy { itemView.findViewById(R.id.filterName) }
        val filterUrl: TextView by lazy { itemView.findViewById(R.id.filterUrl) }
        val filterUpdateTime: TextView by lazy { itemView.findViewById(R.id.filterUpdateTime) }
        val filterDownloadState: TextView by lazy { itemView.findViewById(R.id.downloadState) }
        val switch: SwitchCompat by lazy { itemView.findViewById(R.id.filterSwitch) }

        init {
            switch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setFilterEnabled(adapter.data[adapterPosition].id, isChecked)
            }
        }
    }
}