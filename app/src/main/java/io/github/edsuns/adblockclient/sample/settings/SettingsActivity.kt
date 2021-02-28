package io.github.edsuns.adblockclient.sample.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import io.github.edsuns.adblockclient.sample.R
import io.github.edsuns.adblockclient.sample.databinding.ActivitySettingsBinding
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
                val urlEdit: EditText = dialogView.findViewById(R.id.filterUrlEdit)
                val url = urlEdit.text.toString()
                if (urlEdit.text.isNotBlank() && URLUtil.isNetworkUrl(url)) {
                    val filter = viewModel.addFilter("", url)
                    viewModel.download(filter.id)
                } else {
                    Toast.makeText(
                        this,
                        R.string.invalid_url, Toast.LENGTH_SHORT
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
        val urlEdit: EditText = dialogView.findViewById(R.id.filterUrlEdit)
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
        menuInflater.inflate(R.menu.menu_settings, menu)
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

        var selectedFilter: Filter? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
            val itemView = layoutInflater.inflate(R.layout.filter_item, parent, false)
            return FilterViewHolder(this, itemView)
        }

        override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
            filterList[position].let { filter ->
                holder.filterName.text = if (filter.name.isBlank()) filter.url else filter.name
                holder.filterUrl.text = filter.url
                holder.switch.isChecked = filter.isEnabled
                holder.switch.isEnabled = filter.filtersCount > 0
                holder.filterUpdateTime.text = when (filter.downloadState) {
                    DownloadState.ENQUEUED -> getString(R.string.waiting)
                    DownloadState.DOWNLOADING -> getString(R.string.downloading)
                    DownloadState.INSTALLING -> getString(R.string.installing)
                    DownloadState.FAILED -> getString(R.string.failed_to_download)
                    DownloadState.CANCELLED -> getString(R.string.cancelled)
                    else -> {
                        if (filter.hasDownloaded())
                            dateFormatter.format(Date(filter.updateTime))
                        else getString(R.string.not_downloaded)
                    }
                }
                holder.filtersCount.text =
                    if (filter.hasDownloaded()) filter.filtersCount.toString() else ""
                holder.itemView.setOnClickListener {
                    selectedFilter = filter
                    val items = resources.getTextArray(R.array.filter_detail_items)
                    if (filter.downloadState.isRunning) {
                        items[2] = getString(R.string.cancel)
                    }
                    AlertDialog.Builder(this@SettingsActivity)
                        .setItems(items, this)
                        .show()
                }
            }
        }

        override fun getItemCount(): Int = filterList.size
        override fun onClick(dialog: DialogInterface?, which: Int) {
            selectedFilter?.let {
                when (which) {
                    0 -> {
                        val renameDialogView: View = layoutInflater.inflate(
                            R.layout.dialog_rename_filter,
                            LinearLayout(this@SettingsActivity)
                        )
                        val renameEdit: EditText = renameDialogView.findViewById(R.id.renameEdit)
                        renameEdit.setText(it.name)
                        AlertDialog.Builder(this@SettingsActivity)
                            .setTitle(R.string.rename_filter)
                            .setMessage(it.url)
                            .setView(renameDialogView)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                viewModel.renameFilter(it.id, renameEdit.text.toString())
                            }
                            .show()
                    }
                    1 -> {
                        val clipboardManager =
                            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("text", it.url)
                        clipboardManager.setPrimaryClip(clipData)
                        Toast.makeText(
                            this@SettingsActivity,
                            R.string.url_copied,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    2 -> {
                        if (it.downloadState.isRunning)
                            viewModel.cancelDownload(it.id)
                        else
                            viewModel.download(it.id)
                    }
                    3 -> viewModel.removeFilter(it.id)
                    else -> return
                }
            }
        }
    }

    inner class FilterViewHolder(private val adapter: FilterListAdapter, itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val filterName: TextView by lazy { itemView.findViewById(R.id.filterName) }
        val filterUrl: TextView by lazy { itemView.findViewById(R.id.filterUrl) }
        val filterUpdateTime: TextView by lazy { itemView.findViewById(R.id.filterUpdateTime) }
        val filtersCount: TextView by lazy { itemView.findViewById(R.id.filtersCount) }
        val switch: SwitchCompat by lazy { itemView.findViewById(R.id.filterSwitch) }

        init {
            switch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setFilterEnabled(adapter.data[adapterPosition].id, isChecked, false)
            }
        }
    }
}