/*
 * Copyright (c) 2017 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.edsuns.adblockclient

import android.webkit.WebResourceRequest
import java.util.*


enum class ResourceType(val filterOption: Int) {

    UNKNOWN(0),
    SCRIPT(1),
    IMAGE(2),
    CSS(4),
    SUBDOCUMENT(0x40),
    FONT(0x80000),
    MEDIA(0x100000);

    companion object {

        /**
         * A coarse approach to guessing the resource type from a request
         * to assist the tracker matcher
         */
        fun from(webResourceRequest: WebResourceRequest): ResourceType {

            var headerResult: ResourceType? = null

            val acceptHeader = webResourceRequest.requestHeaders?.get("Accept")
            if (acceptHeader != null) {
                headerResult = from(acceptHeader)
            }

            return headerResult ?: UrlResourceTypeDetector.detect(webResourceRequest) ?: UNKNOWN
        }

        private fun from(acceptHeader: String): ResourceType? {
            if (acceptHeader.contains("image/")) {
                return IMAGE
            }
            if (acceptHeader.contains("/css")) {
                return CSS
            }
            if (acceptHeader.contains("javascript")) {
                return SCRIPT
            }
            if (acceptHeader.contains("text/html")) {
                return SUBDOCUMENT
            }
            if (acceptHeader.contains("font/")) {
                return FONT
            }
            if (acceptHeader.contains("audio/") || acceptHeader.contains("video/")
                || acceptHeader.contains("application/ogg")
            ) {
                return MEDIA
            }
            return null
        }
    }
}

object UrlResourceTypeDetector {

    private val EXTENSIONS_JS = arrayOf("js")
    private val EXTENSIONS_CSS = arrayOf("css")
    private val EXTENSIONS_FONT = arrayOf("ttf", "woff", "woff2")
    private val EXTENSIONS_HTML = arrayOf("htm", "html")

    // listed https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types
    private val EXTENSIONS_IMAGE = arrayOf(
        "png", "jpg", "jpe", "jpeg", "bmp", "gif", "apng", "cur", "jfif",
        "ico", "pjpeg", "pjp", "svg", "tif", "tiff", "webp"
    )

    // video files listed here https://en.wikipedia.org/wiki/Video_file_format
    // audio files listed here https://en.wikipedia.org/wiki/Audio_file_format
    private val EXTENSIONS_MEDIA = arrayOf(
        "webm", "mkv", "flv", "vob", "ogv", "drc", "mng", "avi", "mov", "gifv", "qt", "wmv", "yuv",
        "rm", "rmvb", "asf", "amv", "mp4", "m4p", "mp2", "mpe", "mpv", "mpg", "mpeg", "m2v", "m4v",
        "svi", "3gp", "3g2", "mxf", "roq", "nsv", "8svx", "aa", "aac", "aax", "act", "aiff", "alac",
        "amr", "ape", "au", "awb", "cda", "dct", "dss", "dvf", "flac", "gsm", "iklax", "ivs", "m4a",
        "m4b", "mmf", "mogg", "mp3", "mpc", "msv", "nmf", "oga", "ogg", "opus", "ra", "raw", "rf64",
        "sln", "tta", "voc", "vox", "wav", "wma", "wv"
    )

    private val extensionTypeMap: MutableMap<String, ResourceType> = HashMap<String, ResourceType>()

    init {
        mapExtensions(EXTENSIONS_JS, ResourceType.SCRIPT)
        mapExtensions(EXTENSIONS_CSS, ResourceType.CSS)
        mapExtensions(EXTENSIONS_FONT, ResourceType.FONT)
        mapExtensions(EXTENSIONS_HTML, ResourceType.SUBDOCUMENT)
        mapExtensions(EXTENSIONS_IMAGE, ResourceType.IMAGE)
        mapExtensions(EXTENSIONS_MEDIA, ResourceType.MEDIA)
    }

    private fun mapExtensions(extensions: Array<String>, contentType: ResourceType) {
        for (extension in extensions) {
            // all comparisons are in lower case, force that the extensions are in lower case
            extensionTypeMap[extension.toLowerCase(Locale.ROOT)] = contentType
        }
    }

    fun detect(request: WebResourceRequest?): ResourceType? {
        val path = request?.url?.path ?: return null
        val lastIndexOfDot = path.lastIndexOf('.')
        if (lastIndexOfDot > -1) {
            val fileExtension = path.substring(lastIndexOfDot + 1)
            return extensionTypeMap[fileExtension.toLowerCase(Locale.ROOT)]
        }
        return null
    }
}