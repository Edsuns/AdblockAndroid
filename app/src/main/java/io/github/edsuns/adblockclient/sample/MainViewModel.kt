package io.github.edsuns.adblockclient.sample

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.edsuns.adfilter.MatchedRule
import timber.log.Timber

/**
 * Created by Edsuns@qq.com on 2021/2/27.
 */
class MainViewModel : ViewModel() {

    private val _blockingInfoMap = HashMap<String, BlockingInfo>()

    val blockingInfoMap = MutableLiveData(_blockingInfoMap)

    var currentPageUrl: String = ""

    fun logRequest(matchedRule: MatchedRule) {
        val data = blockingInfoMap.value ?: HashMap()
        val blockingInfo = data[currentPageUrl] ?: BlockingInfo()
        data[currentPageUrl] = blockingInfo
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