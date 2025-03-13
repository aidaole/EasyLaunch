package com.aidaole.easylaunch

import android.util.Log

/**
 * 任务图类
 * 用于管理所有任务节点和检测环
 */
internal class TaskGraph {
    companion object {
        private const val TAG = "TaskGraph"
    }
    
    /**
     * 所有任务节点
     */
    private val nodes = mutableMapOf<String, TaskNode>()
    
    /**
     * 添加任务
     */
    fun addTask(task: Task) {
        val taskName = task.javaClass.name
        if (nodes.containsKey(taskName)) {
            return
        }
        
        val node = TaskNode(task)
        nodes[taskName] = node
        
        // 处理依赖关系
        task.dependencies.forEach { dependencyClass ->
            val dependencyName = dependencyClass.name
            val dependencyNode = nodes[dependencyName] ?: run {
                Log.w(TAG, "依赖任务 $dependencyName 未找到，请确保先添加依赖任务")
                return@forEach
            }
            
            // 建立父子关系
            dependencyNode.addChild(node)
            node.addParent(dependencyNode)
        }
        
        // 检测是否有环
        if (hasCycle()) {
            throw IllegalStateException("任务图中存在环，请检查任务依赖关系")
        }
    }
    
    /**
     * 获取所有没有依赖的任务节点（入度为0的节点）
     */
    fun getRootNodes(): List<TaskNode> {
        return nodes.values.filter { it.parents.isEmpty() }
    }
    
    /**
     * 获取所有任务节点
     */
    fun getAllNodes(): List<TaskNode> {
        return nodes.values.toList()
    }
    
    /**
     * 获取所有就绪的任务节点
     * 就绪的任务节点是指所有父节点都已完成的节点
     */
    fun getReadyNodes(): List<TaskNode> {
        return nodes.values.filter { 
            it.status == TaskStatus.IDLE && it.isReady 
        }
    }
    
    /**
     * 检测是否有环
     * 使用DFS算法检测
     */
    private fun hasCycle(): Boolean {
        val visited = mutableSetOf<TaskNode>()
        val inStack = mutableSetOf<TaskNode>()
        
        for (node in nodes.values) {
            if (hasCycleDFS(node, visited, inStack)) {
                return true
            }
        }
        
        return false
    }
    
    private fun hasCycleDFS(
        node: TaskNode,
        visited: MutableSet<TaskNode>,
        inStack: MutableSet<TaskNode>
    ): Boolean {
        // 如果节点已经在栈中，说明有环
        if (inStack.contains(node)) {
            return true
        }
        
        // 如果节点已经访问过，且不在栈中，说明没有环
        if (visited.contains(node)) {
            return false
        }
        
        // 标记节点为已访问，并加入栈中
        visited.add(node)
        inStack.add(node)
        
        // 递归检查所有子节点
        for (child in node.children) {
            if (hasCycleDFS(child, visited, inStack)) {
                return true
            }
        }
        
        // 回溯时从栈中移除节点
        inStack.remove(node)
        
        return false
    }
    
    /**
     * 重置所有任务状态
     */
    fun reset() {
        nodes.values.forEach { it.status = TaskStatus.IDLE }
    }
    
    /**
     * 清空任务图
     */
    fun clear() {
        nodes.clear()
    }
} 