package demo.xm.com.demo.down2.runnable

import android.os.Environment
import demo.xm.com.demo.down2.DownErrorType
import demo.xm.com.demo.down2.log.BKLog
import demo.xm.com.demo.down2.utils.CommonUtil
import demo.xm.com.demo.down2.utils.FileUtil
import demo.xm.com.demo.down2.utils.FileUtil.del
import demo.xm.com.demo.down2.utils.FileUtil.getSizeUnit
import demo.xm.com.demo.down2.utils.FileUtil.mergeFiles
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 任务多个线程下载（分段）
 */
class MultiRunnable : Runnable {
    companion object {
        const val TAG = "MultiRunnable"
    }

    var threadName = "MultiRunnable"
    var url = ""
    private var total = 0
    var threadNum = 3
    private var downRunnables: ArrayList<SingleRunnable> = ArrayList()
    private var exit = AtomicBoolean(false)
    var listener: MultiRunnable.OnListener? = null
    private var subThreadCompleteCount = 0
    private var pool = ThreadPoolExecutor(threadNum, threadNum, 30, TimeUnit.SECONDS, ArrayBlockingQueue(2000))

    override fun run() {
        exit.set(false) //应用退出标志，用来停止线程的
        down()
    }

    private fun down() {
        /*执行下载操作*/
        //1 获取资源的大小
        iniConn()
        val lump = total / threadNum
        BKLog.d(TAG, "分成$threadNum lump -> $lump B ${getSizeUnit(lump.toLong())}M，总大小${getSizeUnit(total.toLong())} M $total B")
        //2 分配子线程数量,并开始下载

        //创建临时文件夹与文件
        val files = ArrayList<File>()
        val rafs = ArrayList<RandomAccessFile>()
        for (i in 0..(threadNum - 1)) {
            val path = Environment.getExternalStorageDirectory().absolutePath
            val dir = "xmDown/${CommonUtil.getFileName(url)}Temp"
            val fileName = "$i.temp"
            val file = FileUtil.createNewFile(path, dir, fileName)
            val raf = RandomAccessFile(file, "rw")
            raf.seek(file.length())
            files.add(file)
            rafs.add(raf)
        }


        for (i in 0..(threadNum - 1)) {
            val file = files[i]
            val length = file.length()
            val startIndex = if (length == (lump * (i + 1) - 1).toLong()) {
                BKLog.d(TAG, "${file.name} 块下载完成")
                return
            } else {
                length + i * lump
            }
            val endIndex = if (i == (threadNum - 1)) {
                total
            } else {
                lump * (i + 1) - 1
            }


            val singleRunnable = SingleRunnable()
            singleRunnable.url = url
            singleRunnable.threadName = "MultiRunnable_SingleRunnable_$i"
            singleRunnable.raf = rafs[i]
            singleRunnable.rangeStartIndex = startIndex.toInt()
            singleRunnable.rangeEndIndex = endIndex
            singleRunnable.process = startIndex
            singleRunnable.listener = object : SingleRunnable.OnListener {
                override fun onProcess(singleRunnable: SingleRunnable, process: Long, total: Long, present: Float) {
                    BKLog.i(TAG, "${singleRunnable.threadName} process$process total$total")
                }

                override fun onComplete(singleRunnable: SingleRunnable, total: Long) {
                    BKLog.d(TAG, "${singleRunnable.threadName} onComplete total${getSizeUnit(total)}")
                    subThreadCompleteCount++
                }

                override fun onError(singleRunnable: SingleRunnable, type: DownErrorType, s: String) {
                    BKLog.d(TAG, "${singleRunnable.threadName} onError type$type msg$s")
                    listener?.onError(this@MultiRunnable, type, s)
                }
            }
            pool.submit(singleRunnable)
//            Thread(singleRunnable).start()
            downRunnables.add(singleRunnable)
        }

        //3 获取下载的进度
        present()

        //4 下载完成
        complete()
    }

    private fun iniConn() {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.doInput = true
        total = conn.contentLength
    }

    private fun present() {
        /*获取下载进度,单位百分比*/
        var process = 0L
        var runing = false
        while (!runing) {
            runing = true
            for (downRunnable in downRunnables) {
                if (downRunnable.runing()) {
                    runing = false
                }
                process += downRunnable.process
                listener?.onProcess(this, process, total.toLong(), (process * 100 / total).toFloat())
            }
        }
    }

    private fun complete() {
        /*完成下载*/
        if (!exit.get()) {
            while (subThreadCompleteCount != threadNum) {
                continue
            }
            val path = Environment.getExternalStorageDirectory().absolutePath
            val outFile = FileUtil.createNewFile(path, "xmDown", CommonUtil.getFileName(url))
            val inFile = File(path + File.separator + "xmDown/${CommonUtil.getFileName(url)}Temp")
            mergeFiles(outFile, inFile)
            listener?.onComplete(this, total.toLong())
            del(inFile)
            exit()
        }
    }

    fun exit() {
        /*用户退出停止一切线程操作*/
        exit.set(true)
        for (downRunnable in downRunnables) {
            downRunnable.exit()
//            pool = null
        }
    }

    /**
     * 下载监听
     */
    interface OnListener {
        fun onProcess(multiRunnable: MultiRunnable, process: Long, total: Long, present: Float)
        fun onComplete(multiRunnable: MultiRunnable, total: Long)
        fun onError(multiRunnable: MultiRunnable, type: DownErrorType, s: String)
    }
}

