package io.github.edsuns.adblockclient.sample.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.edsuns.adblockclient.sample.main.blocking.BlockingInfo
import io.github.edsuns.adblockclient.sample.stripParamsAndAnchor
import io.github.edsuns.adfilter.MatchedRule
import timber.log.Timber

/**
 * Created by Edsuns@qq.com on 2021/2/27.
 */
class MainViewModel : ViewModel() {

    private val _blockingInfoMap = HashMap<String, BlockingInfo>()

    val blockingInfoMap = MutableLiveData(_blockingInfoMap)

    var currentPageUrl: MutableLiveData<String> = MutableLiveData()

    var dirtyBlockingInfo = false

    fun logRequest(matchedRule: MatchedRule) {
        val pageUrl = currentPageUrl.value ?: return
        val data = _blockingInfoMap
        if (dirtyBlockingInfo) {
            data.clear()
            dirtyBlockingInfo = false
        }
        val blockingInfo = data[pageUrl] ?: BlockingInfo()
        data[pageUrl] = blockingInfo
        if (matchedRule.shouldBlock) {
            val requestUrl = matchedRule.resourceUrl.stripParamsAndAnchor()
            blockingInfo.blockedUrlMap[requestUrl] = matchedRule.rule ?: ""
            blockingInfo.blockedRequests++
            Timber.v("Web request $requestUrl blocked by rule \"${matchedRule.rule}\"")
        }
        blockingInfo.allRequests++
        blockingInfoMap.postValue(data)
    }
}