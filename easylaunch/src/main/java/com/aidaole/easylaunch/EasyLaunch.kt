package com.aidaole.easylaunch

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * EasyLaunch
 * Android启动框架，用于优化应用启动速度
 */
class EasyLaunch private constructor() {
    companion object {
        private const val TAG = "EasyLaunch"

        /**
         * 单例实例
         */
        @Volatile
        private var INSTANCE: EasyLaunch? = null

        /**
         * 获取单例实例
         */
        fun getInstance(): EasyLaunch {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EasyLaunch().also { INSTANCE = it }
            }
        }
    }

    /**
     * 应用上下文
     */
    private lateinit var appContext: Context

    /**
     * 任务调度器
     */
    private val taskScheduler = TaskScheduler()

    /**
     * 启动过程中的异常捕获
     */
    private val exceptionHandler = CoroutineExceptionHandler { context, exception ->
        Log.w(TAG, "启动过程中发生异常", exception)
    }

    /**
     * 协程作用域
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)

    /**
     * 所有任务启动的开始时间
     */
    var startTime: Long = 0
        private set(value) { field = value}

    /**
     * 初始化
     */
    fun init(context: Context): EasyLaunch {
        appContext = context.applicationContext
        Log.d(TAG, "EasyLaunch 初始化完成")
        return this
    }

    /**
     * 添加任务
     */
    fun addTask(task: Task): EasyLaunch {
        taskScheduler.addTask(task)
        return this
    }

    /**
     * 添加多个任务
     */
    fun addTasks(tasks: List<Task>): EasyLaunch {
        tasks.forEach { taskScheduler.addTask(it) }
        return this
    }

    /**
     * 启动任务
     * 异步方式启动，不会阻塞调用线程
     */
    fun start() {
        scope.launch {
            try {
                Log.d(TAG, "开始执行启动任务")
                startTime = System.currentTimeMillis()
                taskScheduler.start()
                Log.d(TAG, "所有启动任务执行完成")
            } catch (e: Exception) {
                Log.e(TAG, "启动任务执行失败", e)
            }
        }
    }

    /**
     * 重置
     * 重置所有任务状态，但不清空任务
     */
    fun reset(): EasyLaunch {
        taskScheduler.reset()
        return this
    }

    /**
     * 清空
     * 清空所有任务
     */
    fun clear(): EasyLaunch {
        taskScheduler.clear()
        return this
    }
} 