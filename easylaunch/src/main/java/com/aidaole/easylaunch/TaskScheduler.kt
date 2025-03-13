package com.aidaole.easylaunch

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 任务调度器
 * 用于调度和执行任务
 */
internal class TaskScheduler {
    companion object {
        private const val TAG = "TaskScheduler"
    }

    /**
     * 任务图
     */
    private val taskGraph = TaskGraph()

    /**
     * 协程作用域
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * 是否正在运行
     */
    private val isRunning = AtomicBoolean(false)

    /**
     * 待执行的任务队列
     */
    private val pendingTasks = ConcurrentLinkedQueue<TaskNode>()

    /**
     * 正在执行的任务数量
     */
    private val runningTaskCount = AtomicInteger(0)
    
    /**
     * 任务完成回调
     */
    private var taskCompletionCallback: ((Task) -> Unit)? = null
    
    /**
     * 任务执行时间记录
     */
    private val taskExecutionTimes = mutableMapOf<String, Long>()

    /**
     * 添加任务
     */
    fun addTask(task: Task) {
        if (isRunning.get()) {
            Log.w(TAG, "任务调度器正在运行，无法添加任务")
            return
        }

        taskGraph.addTask(task)
    }
    
    /**
     * 设置任务完成回调
     */
    fun setTaskCompletionCallback(callback: (Task) -> Unit) {
        this.taskCompletionCallback = callback
    }
    
    /**
     * 获取所有任务
     */
    fun getAllTasks(): List<Class<out Task>> {
        return taskGraph.getAllNodes().map { it.task.javaClass }
    }

    /**
     * 启动任务调度器
     */
    suspend fun start() {
        if (isRunning.getAndSet(true)) {
            Log.w(TAG, "任务调度器已经在运行")
            return
        }

        try {
            // 清空任务执行时间记录
            taskExecutionTimes.clear()
            
            // 构建任务依赖关系
            taskGraph.buildDependencies()
            
            // 初始化待执行任务队列
            pendingTasks.clear()
            pendingTasks.addAll(taskGraph.getRootNodes())

            // 开始执行任务
            executeAllTasks()
            
            // 打印所有任务执行时间
            printTaskExecutionTimes()
        } finally {
            isRunning.set(false)
            taskGraph.reset()
        }
    }

    /**
     * 执行所有任务
     */
    private suspend fun executeAllTasks() {
        // 当还有待执行的任务或者正在执行的任务时，继续执行
        while (pendingTasks.isNotEmpty() || runningTaskCount.get() > 0) {
            // 获取所有可以执行的任务
            val tasksToExecute = mutableListOf<TaskNode>()
            while (pendingTasks.isNotEmpty()) {
                val task = pendingTasks.poll() ?: break
                tasksToExecute.add(task)
            }

            if (tasksToExecute.isEmpty()) {
                // 如果没有可执行的任务，但还有正在执行的任务，等待一下
                if (runningTaskCount.get() > 0) {
                    withContext(Dispatchers.IO) {
                        Thread.sleep(10)
                    }
                }
                continue
            }

            // 并行执行所有可执行的任务
            val deferreds = tasksToExecute.map { taskNode ->
                scope.async {
                    executeTask(taskNode)
                }
            }

            // 等待所有任务执行完成
            deferreds.awaitAll()
        }
    }

    /**
     * 执行单个任务
     */
    private suspend fun executeTask(taskNode: TaskNode) {
        val task = taskNode.task
        val taskStartTime = System.currentTimeMillis()

        try {
            // 更新任务状态
            taskNode.status = TaskStatus.RUNNING
            runningTaskCount.incrementAndGet()

            Log.d(TAG, "开始执行任务: ${task.name}")

            // 根据任务配置选择执行方式
            if (task.isAsync) {
                // 异步任务，在指定的调度器上执行
                withContext(task.dispatcher) {
                    task.execute()
                }
            } else {
                // 同步任务，在主线程执行
                withContext(Dispatchers.Main) {
                    task.execute()
                }
            }

            // 计算任务执行时间
            val taskEndTime = System.currentTimeMillis()
            val executionTime = taskEndTime - taskStartTime
            taskExecutionTimes[task.name] = executionTime
            
            // 更新任务状态为已完成
            taskNode.status = TaskStatus.FINISHED
            Log.d(TAG, "任务执行完成: ${task.name}, 执行时长: ${executionTime}ms, 开始总时长: ${(taskEndTime - EasyLaunch.getInstance().startTime)}ms")
            
            // 调用任务完成回调
            taskCompletionCallback?.invoke(task)

            // 将子任务加入待执行队列
            taskNode.children.forEach { childNode ->
                if (childNode.isReady) {
                    pendingTasks.offer(childNode)
                }
            }
        } catch (e: Exception) {
            // 更新任务状态为失败
            taskNode.status = TaskStatus.FAILED
            Log.e(TAG, "任务执行失败: ${task.name}", e)
        } finally {
            runningTaskCount.decrementAndGet()
        }
    }
    
    /**
     * 打印所有任务执行时间
     */
    private fun printTaskExecutionTimes() {
        if (taskExecutionTimes.isEmpty()) {
            return
        }
        
        val totalTime = taskExecutionTimes.values.sum()
        val sortedTasks = taskExecutionTimes.entries.sortedByDescending { it.value }
        
        val sb = StringBuilder()
        sb.appendLine("======== 任务执行时间统计 ========")
        sb.appendLine("总执行时间: ${totalTime}ms")
        sb.appendLine("各任务执行时间:")
        
        sortedTasks.forEachIndexed { index, entry ->
            val percentage = String.format("%.1f", entry.value * 100.0 / totalTime)
            sb.appendLine("${index + 1}. ${entry.key}: ${entry.value}ms (${percentage}%)")
        }
        
        sb.appendLine("================================")
        Log.i(TAG, sb.toString())
    }

    /**
     * 重置任务调度器
     */
    fun reset() {
        if (isRunning.get()) {
            Log.w(TAG, "任务调度器正在运行，无法重置")
            return
        }

        taskGraph.reset()
        taskExecutionTimes.clear()
    }

    /**
     * 清空任务调度器
     */
    fun clear() {
        if (isRunning.get()) {
            Log.w(TAG, "任务调度器正在运行，无法清空")
            return
        }

        taskGraph.clear()
        taskExecutionTimes.clear()
    }
} 