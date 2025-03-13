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
        private set(value) { field = value }
        
    /**
     * 已完成的任务集合
     */
    private val completedTasks = mutableSetOf<Class<out Task>>()
    
    /**
     * 任务完成监听器
     * key: 监听的任务类列表
     * value: 任务完成时的回调
     */
    private val taskListeners = mutableListOf<Pair<List<Class<out Task>>, () -> Unit>>()

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
     * 添加任务完成监听器
     * 当指定的所有任务都完成时，会调用监听器
     * @param tasks 要监听的任务类列表
     * @param listener 任务完成时的回调
     */
    fun addTasksCompletedListener(tasks: List<Class<out Task>>, listener: () -> Unit): EasyLaunch {
        // 检查任务是否已经完成
        if (completedTasks.containsAll(tasks)) {
            // 如果所有任务都已完成，直接调用监听器
            listener.invoke()
        } else {
            // 否则，添加到监听器列表
            taskListeners.add(Pair(tasks, listener))
        }
        return this
    }
    
    /**
     * 添加单个任务完成监听器
     * 当指定的任务完成时，会调用监听器
     * @param taskClass 要监听的任务类
     * @param listener 任务完成时的回调
     */
    fun addTaskCompletedListener(taskClass: Class<out Task>, listener: () -> Unit): EasyLaunch {
        return addTasksCompletedListener(listOf(taskClass), listener)
    }
    
    /**
     * 添加所有任务完成监听器
     * 当所有任务都完成时，会调用监听器
     * @param listener 所有任务完成时的回调
     */
    fun addAllTasksCompletedListener(listener: () -> Unit): EasyLaunch {
        // 获取所有任务
        val allTasks = taskScheduler.getAllTasks()
        
        // 如果没有任务，直接调用监听器
        if (allTasks.isEmpty()) {
            listener.invoke()
            return this
        }
        
        // 添加监听器
        return addTasksCompletedListener(allTasks, listener)
    }
    
    /**
     * 检查任务是否已完成
     * @param taskClass 要检查的任务类
     * @return 如果任务已完成，返回true；否则返回false
     */
    fun isTaskCompleted(taskClass: Class<out Task>): Boolean {
        return completedTasks.contains(taskClass)
    }
    
    /**
     * 检查所有指定的任务是否已完成
     * @param tasks 要检查的任务类列表
     * @return 如果所有指定的任务都已完成，返回true；否则返回false
     */
    fun areTasksCompleted(tasks: List<Class<out Task>>): Boolean {
        return completedTasks.containsAll(tasks)
    }
    
    /**
     * 检查所有任务是否已完成
     * @return 如果所有任务已完成，返回true；否则返回false
     */
    fun areAllTasksCompleted(): Boolean {
        val allTasks = taskScheduler.getAllTasks()
        return allTasks.isNotEmpty() && completedTasks.containsAll(allTasks)
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
                
                // 清空已完成任务集合
                completedTasks.clear()
                
                // 设置任务完成回调
                taskScheduler.setTaskCompletionCallback { task ->
                    // 添加到已完成任务集合
                    completedTasks.add(task.javaClass)
                    
                    // 检查监听器
                    checkListeners()
                }
                
                taskScheduler.start()
                Log.d(TAG, "所有启动任务执行完成")
            } catch (e: Exception) {
                Log.e(TAG, "启动任务执行失败", e)
            }
        }
    }
    
    /**
     * 检查监听器
     * 检查是否有监听器的所有任务都已完成
     */
    private fun checkListeners() {
        // 创建一个新的列表，避免并发修改异常
        val listenersToRemove = mutableListOf<Pair<List<Class<out Task>>, () -> Unit>>()
        
        // 检查每个监听器
        taskListeners.forEach { (tasks, listener) ->
            if (completedTasks.containsAll(tasks)) {
                // 如果所有任务都已完成，调用监听器
                listener.invoke()
                // 添加到待移除列表
                listenersToRemove.add(Pair(tasks, listener))
            }
        }
        
        // 移除已调用的监听器
        taskListeners.removeAll(listenersToRemove)
    }

    /**
     * 重置
     * 重置所有任务状态，但不清空任务
     */
    fun reset(): EasyLaunch {
        taskScheduler.reset()
        completedTasks.clear()
        taskListeners.clear()
        return this
    }

    /**
     * 清空
     * 清空所有任务
     */
    fun clear(): EasyLaunch {
        taskScheduler.clear()
        completedTasks.clear()
        taskListeners.clear()
        return this
    }
} 