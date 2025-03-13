package com.aidaole.easylaunch

/**
 * 任务节点类
 * 用于构建有向无环图
 */
internal class TaskNode(
    val task: Task,
    val children: MutableList<TaskNode> = mutableListOf(),
    val parents: MutableList<TaskNode> = mutableListOf()
) {
    /**
     * 任务状态
     */
    var status: TaskStatus = TaskStatus.IDLE
    
    /**
     * 任务是否已完成
     */
    val isFinished: Boolean
        get() = status == TaskStatus.FINISHED
    
    /**
     * 任务是否可以执行
     * 只有当所有父任务都已完成时，当前任务才可以执行
     */
    val isReady: Boolean
        get() = parents.all { it.isFinished }
    
    /**
     * 添加子节点
     */
    fun addChild(child: TaskNode) {
        if (!children.contains(child)) {
            children.add(child)
        }
    }
    
    /**
     * 添加父节点
     */
    fun addParent(parent: TaskNode) {
        if (!parents.contains(parent)) {
            parents.add(parent)
        }
    }
    
    override fun toString(): String {
        return "TaskNode(task=${task.name}, status=$status, parents=${parents.map { it.task.name }}, children=${children.map { it.task.name }})"
    }
}

/**
 * 任务状态枚举
 */
enum class TaskStatus {
    /**
     * 初始状态
     */
    IDLE,
    
    /**
     * 正在执行
     */
    RUNNING,
    
    /**
     * 已完成
     */
    FINISHED,
    
    /**
     * 执行失败
     */
    FAILED
} 