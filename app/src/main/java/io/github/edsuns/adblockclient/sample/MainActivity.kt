package io.github.edsuns.adblockclient.sample

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import io.github.edsuns.adblockclient.sample.databinding.ActivityMainBinding
import io.github.edsuns.adfilter.AdFilter
import io.github.edsuns.adfilter.FilterViewModel
import io.github.edsuns.adfilter.MatchedRule
import io.github.edsuns.smoothprogress.SmoothProgressAnimator
import timber.log.Timber

class MainActivity : AppCompatActivity(), WebViewClientListener {
    private lateinit var viewModel: FilterViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView
    private lateinit var progressAnimator: SmoothProgressAnimator

    private val blockedCountMap: HashMap<String, HashSet<String>> = hashMapOf()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = AdFilter.get().viewModel

        val popupMenu = PopupMenu(
            this,
            binding.menuButton,
            Gravity.NO_GRAVITY,
            R.attr.actionOverflowMenuStyle,
            0
        )
        popupMenu.inflate(R.menu.menu_main)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menuRefresh -> webView.reload()
                R.id.menuForward -> webView.goForward()
                R.id.menuSettings ->
                    startActivity(Intent(this, SettingsActivity::class.java))
                else -> finish()
            }
            true
        }
        val menuForward = popupMenu.menu.findItem(R.id.menuForward)

        binding.menuButton.setOnClickListener {
            menuForward.isVisible = webView.canGoForward()
            popupMenu.show()
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
        AdFilter.get().setupWebView(webView)

        progressAnimator = SmoothProgressAnimator(binding.loadProgress)

        val urlText = binding.urlEditText
        urlText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO
                || event.keyCode == KeyEvent.KEYCODE_ENTER
                && event.action == KeyEvent.ACTION_DOWN
            ) {
                val urlIn = urlText.text.toString()
                webView.loadUrl(
                    urlIn.smartUrlFilter() ?: URLUtil.composeSearchUrl(
                        urlIn,
                        "https://www.bing.com/search?q={}",
                        "{}"
                    )
                )
                webView.requestFocus()
                urlText.hideKeyboard()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        viewModel.isEnabled.observe(this, {
            binding.countText.text =
                if (it) getString(R.string.count_none) else getString(R.string.off)
        })
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
            progressAnimator.progress = newProgress
            binding.urlEditText.setText(webView.url)
        }
    }

    override fun requestBlocked(rule: MatchedRule) {
        runOnUiThread {
            val requestUrl = rule.resourceUrl.stripParamsAndAnchor()
            val pageUrl = webView.url
            val blockedSet = blockedCountMap[pageUrl] ?: hashSetOf()
            blockedSet.add(requestUrl)
            pageUrl?.let { blockedCountMap[pageUrl] = blockedSet }
            binding.countText.text = blockedSet.size.toString()
            Timber.v("Web request $requestUrl blocked by rule \"${rule.rule}\"")
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