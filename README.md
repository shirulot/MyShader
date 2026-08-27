# MyShader

一个用于学习 Android OpenGL ES 2.0 与 GLSL ES 1.00 的小型项目。

项目把每个练习拆成独立的片元着色器文件，通过首页 RecyclerView 选择不同 Demo。目标是从纹理采样、颜色处理开始，逐步理解美白、肤色权重、邻域采样、边缘保护和双边滤波等基础。

## 运行方式

使用 Android Studio 打开项目后运行，或在项目根目录执行：

```bash
./gradlew :app:assembleDebug
```

设备需要支持 OpenGL ES 2.0。

## 使用方式

1. 首页按分组展示 Demo，组头默认收起。
2. 进入任意 Demo 后，可以切换“原练习图”和“新的人像”。
3. 基础组不显示滑条；美白组只显示美白滑条；磨皮组与当前 `main.frag` 入口只显示磨皮滑条。暖色滑条默认隐藏。范围均为 `0.00–1.00`，默认值为 `0.00`。
4. 只有声明了 `whitenStrength` 或 `blurStrength` 的片元着色器会响应对应滑条；未声明时画面保持不变。
5. 底部控制面板可收起；收起时面板下滑且测试图片不再被遮挡，点击底部“展开”后面板上滑恢复。
6. 暖色滑条当前仅用于 UI 交互和数值展示，尚未传入 Shader，因此拖动不会改变画面。需要展示时，在 `ShaderDemoCatalog` 对应的 `ShaderDemo` 上添加 `showWarmthStrengthControl = true`。

## 练习内容

### 基础（首页“基础”组）

- Demo 1：固定红色
- Demo 2：UV 红绿渐变
- Demo 3：采样坐标偏移
- Demo 4：整体变暗
- Demo 5：整体提亮

原图直通（`main.frag`）保留为首页独立条目，不属于任何分组。

### 美白（首页“美白”组）

- Demo 6：右半屏混白
- Demo 7：条件混白
- Demo 8：左右效果对比
- Demo 9：黑白 Mask
- Demo 10：连续权重 Mask

### 磨皮

- Demo 11：两点横向柔化
- Demo 12：三点横向柔化
- Demo 13：肤色权重磨皮
- Demo 14：边缘保护磨皮
- Demo 15：九点边缘保护磨皮
- Demo 16：九点边缘保护磨皮
- Demo 17：双边滤波磨皮

### 局部色差调试

- Demo 18：局部色差灰度图
- Demo 19：暖色磨皮
- Demo 20：局部暖色调试
- Demo 21：暖色强度参数

## 目录说明

```text
app/src/main/assets/main.frag   # 当前练习中的主片元 Shader
app/src/main/assets/main.vert   # 共用主顶点 Shader
app/src/main/assets/shaders/    # 按 basic、whitening、skin_smoothing、local_color_difference_debug 分类的 Demo
app/src/main/java/.../          # Android 页面、列表与 OpenGL 渲染器
app/src/main/res/drawable-nodpi # 两张本地测试人像
```

`main.frag` 是持续编写新练习的工作文件。需要归档时，将它完整复制为新的 `demo_xx_*.frag`，放入对应分类目录，再在 `ShaderDemoCatalog` 中添加首页条目；保留工作文件中的代码和注释。

## 渲染链路

`MainActivity` 选择 Demo → `ShaderDemoActivity` 创建 `ShaderSurfaceView` → `ShaderRenderer` 编译 Shader、上传图片纹理并调用 `glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)` → GPU 执行片元着色器。
