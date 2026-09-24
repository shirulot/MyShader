package com.shirulot.myshader

/** 为教学用闭合唇形插入曲线中点；保留全部原始点，不改变旧 Demo 的输入。 */
object LipContourSmoother {
    const val SOURCE_POINT_COUNT = 20
    const val OUTPUT_POINT_COUNT = SOURCE_POINT_COUNT * 2

    /** 使用均匀 Catmull–Rom 的 t=0.5 位置，每段输出原点和一个曲线中点。 */
    fun smooth(points: FloatArray): FloatArray {
        require(points.size == SOURCE_POINT_COUNT * 2)
        val result = FloatArray(OUTPUT_POINT_COUNT * 2)
        for (i in 0 until SOURCE_POINT_COUNT) {
            // 首尾按闭合轮廓衔接，四个点依次为前一点、起点、终点、后一点。
            val previous = (i + SOURCE_POINT_COUNT - 1) % SOURCE_POINT_COUNT
            val next = (i + 1) % SOURCE_POINT_COUNT
            val following = (i + 2) % SOURCE_POINT_COUNT
            for (axis in 0..1) {
                val p0 = points[previous * 2 + axis]
                val p1 = points[i * 2 + axis]
                val p2 = points[next * 2 + axis]
                val p3 = points[following * 2 + axis]
                result[i * 4 + axis] = p1
                // 曲线中点通常偏离原线段；直线中点无法消除折角。
                result[i * 4 + 2 + axis] = (-p0 + 9f * p1 + 9f * p2 - p3) / 16f
            }
        }
        return result
    }
}
