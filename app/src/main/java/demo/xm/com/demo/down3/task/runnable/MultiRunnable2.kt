package demo.xm.com.demo.down3.task.runnable

import android.os.Environment
import demo.xm.com.demo.down3.enum_.DownErrorType
import demo.xm.com.demo.down3.utils.BKLog
import demo.xm.com.demo.down3.utils.CommonUtil
import demo.xm.com.demo.down3.utils.CommonUtil.getFileName
import demo.xm.com.demo.down3.utils.FileUtil
import demo.xm.com.demo.down3.utils.FileUtil.del
import demo.xm.com.demo.down3.utils.FileUtil.getSizeUnit
import demo.xm.com.demo.down3.utils.FileUtil.mergeFiles
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 任务多个线程下载（分段）
 */
class MultiRunnable2 : BaseRunnable() {

    companion object {
        const val TAG = "MultiRunnable2"
    }

    var threadNum = 0   //限定线程数量
    private var pool: ExecutorService? = null
    private var subThreadCompleteCount = 0 //下载线程完成集合数量
    private var downRunnables: ArrayList<SingleRunnable2> = ArrayList() //线程集合

    init {
        pool = ThreadPoolExecutor(2, 2, 20, TimeUnit.SECONDS, ArrayBlockingQueue(2000)) //线程池
    }

    override fun down() {
        /*执行下载操作*/
        //1 获取资源的大小
        val conn = iniConn()
        total = conn.contentLength.toLong()

        val lump = total / threadNum
        BKLog.d(TAG, "分成$threadNum lump -> $lump B ${getSizeUnit(lump.toLong())}M，总大小${getSizeUnit(total.toLong())} M $total B")
        //2 分配子线程数量,并开始下载

        //创建临时文件夹与文件
        val files = ArrayList<File>()
        val rafs = ArrayList<RandomAccessFile>()
        for (i in 0..(threadNum - 1)) {
            val dir = "$dir/${CommonUtil.getFileName(url)}_Temp"
            val fileName = "$i.temp"
            val file = FileUtil.createNewFile(path, dir, fileName)
            val raf = RandomAccessFile(file, "rw")
            raf.seek(file.length())
            files.add(file)
            rafs.add(raf)
        }

        //线程启动
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

            val singleRunnable = SingleRunnable2()
            singleRunnable.url = url
            singleRunnable.threadName = "$name MultiRunnable_SingleRunnable_$i"
            singleRunnable.raf = rafs[i]
            singleRunnable.rangeStartIndex = startIndex
            singleRunnable.rangeEndIndex = endIndex
            singleRunnable.process = files[i].length()
            singleRunnable.listener = object : BaseRunnable.OnListener {

                override fun onProcess(singleRunnable: BaseRunnable, process: Long, total: Long, present: Float) {
                    //3 获取下载的进度
                    callbackProcess(singleRunnable, process, total, present)
                }

                override fun onComplete(singleRunnable: BaseRunnable, total: Long) {
                    //4 下载完成
                    callbackComplete(singleRunnable, total)
                }

                override fun onError(singleRunnable: BaseRunnable, type: DownErrorType, s: String) {
                    //下载失败
                    callbackError(singleRunnable, type, s)
                }

                private fun callbackProcess(singleRunnable: BaseRunnable, process: Long, total: Long, present: Float) {
                    //Thread.sleep(1000)
                    this@MultiRunnable2.process = 0
                    for (downRunnable in downRunnables) {
                        this@MultiRunnable2.process += downRunnable.process
                    }
                    //this@MultiRunnable2.process += process
//                    listener?.onProcess(this@MultiRunnable2, process, this@MultiRunnable2.total, (process * 100 / total).toFloat())
                    listener?.onProcess(this@MultiRunnable2, this@MultiRunnable2.process, this@MultiRunnable2.total, (this@MultiRunnable2.process * 100 / this@MultiRunnable2.total).toFloat())
                }

                private fun callbackComplete(singleRunnable: BaseRunnable, total: Long) {
                    subThreadCompleteCount++
                    if (subThreadCompleteCount == threadNum) {
                        runing(false)
                        exit()
                        val path = Environment.getExternalStorageDirectory().absolutePath
                        val outFile = FileUtil.createNewFile(path, dir, getFileName(url))
                        val inFile = File(path + File.separator + "$dir/${getFileName(url)}_Temp")
                        mergeFiles(outFile, inFile)
                        del(inFile)
                        listener?.onComplete(this@MultiRunnable2, this@MultiRunnable2.total)
                    }
                }

                private fun callbackError(singleRunnable: BaseRunnable, type: DownErrorType, s: String) {
                    listener?.onError(this@MultiRunnable2, type, s)
                }
            }
            pool?.submit(singleRunnable)
            downRunnables.add(singleRunnable)
        }
    }

    override fun exit() {
        /*用户退出停止一切线程操作*/
        runing(false)
        for (downRunnable in downRunnables) {
            downRunnable.exit()
        }
    }
}
