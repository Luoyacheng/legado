package io.legado.app.utils

import android.annotation.SuppressLint
import android.text.TextUtils.isEmpty
import android.util.Base64
import io.legado.app.help.DirectLinkUpload
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.abs
import kotlin.random.Random


@Suppress("unused", "MemberVisibilityCanBePrivate")
object StringUtils {
    private const val HOUR_OF_DAY = 24
    private const val DAY_OF_YESTERDAY = 2
    private const val TIME_UNIT = 60
    private val ChnMap = chnMap
    private val wordCountFormatter by lazy {
        DecimalFormat("#.#")
    }

    private val chnMap: HashMap<Char, Int>
        get() {
            val map = HashMap<Char, Int>()
            var cnStr = "零一二三四五六七八九十"
            var c = cnStr.toCharArray()
            for (i in 0..10) {
                map[c[i]] = i
            }
            cnStr = "〇壹贰叁肆伍陆柒捌玖拾"
            c = cnStr.toCharArray()
            for (i in 0..10) {
                map[c[i]] = i
            }
            map['两'] = 2
            map['百'] = 100
            map['佰'] = 100
            map['千'] = 1000
            map['仟'] = 1000
            map['万'] = 10000
            map['亿'] = 100000000
            return map
        }

    /**
     * 将日期转换成昨天、今天、明天
     */
    fun dateConvert(source: String, pattern: String): String {
        val format = SimpleDateFormat(pattern, Locale.getDefault())
        val calendar = Calendar.getInstance()
        kotlin.runCatching {
            val date = format.parse(source) ?: return ""
            val curTime = calendar.timeInMillis
            calendar.time = date
            //将MISC 转换成 sec
            val difSec = abs((curTime - date.time) / 1000)
            val difMin = difSec / 60
            val difHour = difMin / 60
            val difDate = difHour / 60
            val oldHour = calendar.get(Calendar.HOUR)
            //如果没有时间
            if (oldHour == 0) {
                //比日期:昨天今天和明天
                return when {
                    difDate == 0L -> "今天"
                    difDate < DAY_OF_YESTERDAY -> "昨天"
                    else -> {
                        @SuppressLint("SimpleDateFormat")
                        val convertFormat = SimpleDateFormat("yyyy-MM-dd")
                        convertFormat.format(date)
                    }
                }
            }

            return when {
                difSec < TIME_UNIT -> difSec.toString() + "秒前"
                difMin < TIME_UNIT -> difMin.toString() + "分钟前"
                difHour < HOUR_OF_DAY -> difHour.toString() + "小时前"
                difDate < DAY_OF_YESTERDAY -> "昨天"
                else -> {
                    @SuppressLint("SimpleDateFormat")
                    val convertFormat = SimpleDateFormat("yyyy-MM-dd")
                    convertFormat.format(date)
                }
            }
        }.onFailure {
            it.printOnDebug()
        }
        return ""
    }

    /**
     * 首字母大写
     */
    @SuppressLint("DefaultLocale")
    fun toFirstCapital(str: String): String {
        return str.substring(0, 1).uppercase(Locale.getDefault()) + str.substring(1)
    }

    /**
     * 将文本中的半角字符，转换成全角字符
     */
    fun halfToFull(input: String): String {
        val c = input.toCharArray()
        for (i in c.indices) {
            if (c[i].code == 32)
            //半角空格
            {
                c[i] = 12288.toChar()
                continue
            }
            //根据实际情况，过滤不需要转换的符号
            //if (c[i] == 46) //半角点号，不转换
            // continue;

            if (c[i].code in 33..126)
            //其他符号都转换为全角
                c[i] = (c[i].code + 65248).toChar()
        }
        return String(c)
    }

    /**
     * 字符串全角转换为半角
     */
    fun fullToHalf(input: String): String {
        val c = input.toCharArray()
        for (i in c.indices) {
            if (c[i].code == 12288)
            //全角空格
            {
                c[i] = 32.toChar()
                continue
            }

            if (c[i].code in 65281..65374)
                c[i] = (c[i].code - 65248).toChar()
        }
        return String(c)
    }

    /**
     * 中文大写数字转数字
     */
    fun chineseNumToInt(chNum: String): Int {
        var result = 0
        var tmp = 0
        var billion = 0
        val cn = chNum.toCharArray()

        // "一零二五" 形式
        if (cn.size > 1 && chNum.matches("^[〇零一二三四五六七八九壹贰叁肆伍陆柒捌玖]$".toRegex())) {
            for (i in cn.indices) {
                cn[i] = (48 + ChnMap[cn[i]]!!).toChar()
            }
            return Integer.parseInt(String(cn))
        }

        // "一千零二十五", "一千二" 形式
        return kotlin.runCatching {
            for (i in cn.indices) {
                val tmpNum = ChnMap[cn[i]]!!
                when {
                    tmpNum == 100000000 -> {
                        result += tmp
                        result *= tmpNum
                        billion = billion * 100000000 + result
                        result = 0
                        tmp = 0
                    }

                    tmpNum == 10000 -> {
                        result += tmp
                        result *= tmpNum
                        tmp = 0
                    }

                    tmpNum >= 10 -> {
                        if (tmp == 0)
                            tmp = 1
                        result += tmpNum * tmp
                        tmp = 0
                    }

                    else -> {
                        tmp = if (i >= 2 && i == cn.size - 1 && ChnMap[cn[i - 1]]!! > 10)
                            tmpNum * ChnMap[cn[i - 1]]!! / 10
                        else
                            tmp * 10 + tmpNum
                    }
                }
            }
            result += tmp + billion
            result
        }.getOrDefault(-1)
    }

    /**
     * 字符串转数字
     */
    fun stringToInt(str: String?): Int {
        if (str != null) {
            val num = fullToHalf(str).replace("\\s+".toRegex(), "")
            return kotlin.runCatching {
                Integer.parseInt(num)
            }.getOrElse {
                chineseNumToInt(num)
            }
        }
        return -1
    }

    /**
     * 是否包含数字
     */
    fun isContainNumber(company: String): Boolean {
        val p = Pattern.compile("[0-9]+")
        val m = p.matcher(company)
        return m.find()
    }

    /**
     * 是否数字
     */
    fun isNumeric(str: String): Boolean {
        val pattern = Pattern.compile("-?[0-9]+")
        val isNum = pattern.matcher(str)
        return isNum.matches()
    }

    fun wordCountFormat(words: Int): String {
        var wordsS = ""
        if (words > 0) {
            if (words > 10000) {
                val df = wordCountFormatter
                wordsS = df.format(words * 1.0f / 10000f.toDouble()) + "万字"
            } else {
                wordsS = words.toString() + "字"
            }
        }
        return wordsS
    }

    fun wordCountFormat(wc: String?): String {
        if (wc == null) return ""
        var wordsS = ""
        if (isNumeric(wc)) {
            val words: Int = wc.toInt()
            if (words > 0) {
                if (words > 10000) {
                    val df = wordCountFormatter
                    wordsS = df.format(words * 1.0f / 10000f.toDouble()) + "万字"
                } else {
                    wordsS = words.toString() + "字"
                }
            }
        } else {
            wordsS = wc
        }
        return wordsS
    }

    /**
     * 移除字符串首尾空字符的高效方法(利用ASCII值判断,包括全角空格)
     */
    fun trim(s: String): String {
        if (isEmpty(s)) return ""
        var start = 0
        val len = s.length
        var end = len - 1
        while (start < end && (s[start].code <= 0x20 || s[start] == '　')) {
            ++start
        }
        while (start < end && (s[end].code <= 0x20 || s[end] == '　')) {
            --end
        }
        ++end
        return if (start > 0 || end < len) s.substring(start, end) else s
    }

    /**
     * 重复字符串
     */
    fun repeat(str: String, n: Int): String {
        val stringBuilder = StringBuilder()
        for (i in 0 until n) {
            stringBuilder.append(str)
        }
        return stringBuilder.toString()
    }

    /**
     * 移除UTF头
     */
    fun removeUTFCharacters(data: String?): String? {
        if (data == null) return null
        val p = Pattern.compile("\\\\u(\\p{XDigit}{4})")
        val m = p.matcher(data)
        val buf = StringBuffer(data.length)
        while (m.find()) {
            val ch = Integer.parseInt(m.group(1)!!, 16).toChar().toString()
            m.appendReplacement(buf, Matcher.quoteReplacement(ch))
        }
        m.appendTail(buf)
        return buf.toString()
    }

    /**
     * 压缩字符串
     */
    fun compress(str: String): Result<String> {
        return kotlin.runCatching {
            if (str.isEmpty()) {
                return@runCatching str
            }
            val out = ByteArrayOutputStream()
            var gzip: GZIPOutputStream? = null
            return@runCatching try {
                gzip = GZIPOutputStream(out)
                gzip.write(str.toByteArray())
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            } finally {
                gzip?.runCatching {
                    close()
                }
                out.runCatching {
                    close()
                }
            }
        }
    }

    /**
     * 解压字符串
     */
    @Throws(IOException::class)
    fun unCompress(str: String): Result<String> {
        return kotlin.runCatching {
            val outputStream = ByteArrayOutputStream()
            var inputStream: ByteArrayInputStream? = null
            var ginZip: GZIPInputStream? = null
            return@runCatching try {
                val compressed = Base64.decode(str, Base64.NO_WRAP)
                inputStream = ByteArrayInputStream(compressed)
                ginZip = GZIPInputStream(inputStream)
                ginZip.copyTo(outputStream)
                outputStream.toString()
            } finally {
                ginZip?.runCatching {
                    close()
                }
                inputStream?.runCatching {
                    close()
                }
                outputStream.runCatching {
                    close()
                }
            }
        }
    }


    private val shibbolethMappings =  mapOf(
        "https://" to listOf("#L:"),
        "." to listOf("电", "店", "垫", "殿","。"),
        "%" to listOf("白", "百", "拜", "摆", "💯"),
        "/"  to listOf("杠", "刚", "钢", "岗", "🎹"),
        "zip" to listOf("压", "亚", "呀", "牙", "🦆"),
        "json"  to listOf("串", "穿", "船", "传", "🚢"),
        "4" to listOf("四", "是", "时", "丝", "🕓"),
        "5" to listOf("五", "武", "误", "勿", "🕔"),
        "6" to listOf("六", "刘", "留", "陆", "🕕"),
        "0" to listOf("零", "另", "玲", "灵", "⏰"),
        "com" to listOf("🛜1", "🌐1", "🌏1"),
        "cn" to listOf("🛜2", "🌐2", "🌏2"),
        "net" to listOf("🛜3", "🌐3", "🌏3"),
        "org" to listOf("🛜7", "🌐7", "🌏7"),
        "xyz" to listOf("🛜8", "🌐8", "🌏8"),
        "me" to listOf("🛜9", "🌐9", "🌏9"),
    )

    private val reverseMappings by lazy {
        shibbolethMappings.flatMap { (original, replacements) ->
            replacements.map { replacement -> replacement to original }
        }.toMap()
    }

    const val BOOK_SOURCE = "sy"
    const val RSS_SOURCE = "dy"
    const val DICT_RULE = "zd"
    const val REPLACE_RULE = "jh"
    const val TOC_RULE = "ml"
    const val TTS_RULE = "ld"


    /**
     * 链接转口令
     */
    fun toShibboleth(url:String, type:String, time: Long = System.currentTimeMillis()): String {
        val random = Random(time)
        val sortedKeys = shibbolethMappings.keys.sortedByDescending { it.length }
        val result = StringBuilder()
        var i = 0
        while (i < url.length) {
            var matchKey: String? = null
            var matchLen = 0

            // 找出所有匹配的 key，选最长的
            for (key in sortedKeys) {
                if (url.startsWith(key, i)) {
                    if (key.length > matchLen) {
                        matchKey = key
                        matchLen = key.length
                    }
                }
            }

            if (matchKey != null) {
                val replacements = shibbolethMappings[matchKey]!!
                result.append(replacements[random.nextInt(replacements.size)])
                i += matchLen
            } else {
                result.append(url[i])
                i++
            }
        }

        val expiryDate = DirectLinkUpload.getExpiryDate()
        val expiresAt = if (expiryDate < 1) 0 else {
            time + expiryDate.toLong() * 60 * 60 * 24 * 1000
        }
        return "复制口令到阅读导入$result！$type©${expiresAt.toString().take(7)}¥Sigma^"
    }

    /**
     * 解口令
     */
    fun unShibboleth(txt:String): Triple<String, String, String> {
        val p1 = txt.indexOf("#L:").takeIf { it > 0 } ?: 0
        val p2 = txt.indexOf("！", p1)
        val url = if (p2 > 0) {
            txt.substring(p1, p2)
        } else {
            txt.substring(p1)
        }
        var customWord = ""
        val type = if (p2 > 0) {
            val p3 = txt.indexOf("©", p2)
            if (p3 > 1) {
                val p4 = txt.indexOf("¥", p3)
                if (p4 > 1) {
                    val expiresAt = txt.substring(p3 + 1, p4).toLong() * 1000 * 1000
                    if (expiresAt > 1 && expiresAt < System.currentTimeMillis()) {
                        //已过期
                    }
                    val p5 = txt.indexOf("^", p4)
                    if (p5 > 1) {
                        customWord = txt.substring(p4 + 1, p5)
                    }
                }
                txt.substring(p2 + 1, p3)
            } else {
                ""
            }

        } else {
            ""
        }
        var result = url
        reverseMappings.forEach { (replacement, original) ->
            result = result.replace(replacement, original)
        }
        return Triple(result, type, customWord)
    }

}
