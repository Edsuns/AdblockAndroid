package io.github.edsuns.adblockclient

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import io.github.edsuns.adblockclient.databinding.ActivityMainBinding
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.FilterViewModel

class MainActivity : AppCompatActivity(), WebViewClientListener {
    private lateinit var viewModel: FilterViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView

    private val blockedCountMap: HashMap<String, HashSet<String>> = hashMapOf()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = AdFilter.get().viewModel

        val menuButton = binding.menuButton
        menuButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        webView = binding.webView
        webView.webViewClient = WebClient(this)
        webView.webChromeClient = ChromeClient(this)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // Zooms out the content to fit on screen by width. For example, showing images.
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        val urlText = binding.urlEditText
        urlText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO
                || event.keyCode == KeyEvent.KEYCODE_ENTER
                && event.action == KeyEvent.ACTION_DOWN
            ) {
                webView.loadUrl(smartUrlFilter(urlText.text.toString()))
                webView.requestFocus()
                hideKeyboard(urlText)
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        viewModel.isEnabled.observe(this, {
            binding.countText.text =
                if (it) getString(R.string.count_none) else getString(R.string.off)
        })
    }

    private fun smartUrlFilter(url: String): String {
        return URLUtil.guessUrl(url)
    }

    private fun hideKeyboard(view: View) {
        view.context.getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showKeyboard(view: View) {
        view.context.getSystemService(InputMethodManager::class.java)
            ?.toggleSoftInputFromWindow(view.windowToken, 0, 0)
    }

    override fun onPageStarted(url: String?, favicon: Bitmap?) {
        if (viewModel.isEnabled.value == true)
            runOnUiThread {
                val pageUrl = webView.url
                val blockedSet = blockedCountMap[pageUrl] ?: hashSetOf()
                binding.countText.text = blockedSet.size.toString()
            }
    }

    override fun progressChanged(newProgress: Int) {
        runOnUiThread {
            binding.loadProgress.progress = newProgress
            binding.urlEditText.setText(webView.url)
        }
    }

    override fun requestBlocked(url: String) {
        runOnUiThread {
            val pageUrl = webView.url
            val blockedSet = blockedCountMap[pageUrl] ?: hashSetOf()
            blockedSet.add(url)
            blockedCountMap[pageUrl] = blockedSet
            binding.countText.text = blockedSet.size.toString()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        super.onBackPressed()
    }
}