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
3. 基础组不显示滑条；美白组中 Demo 6–8、10 显示美白滑条，Demo 9 仅输出二值 mask、不显示滑条；Demo 11–13 使用固定采样权重、不显示滑条，Demo 14–17 只显示磨皮；Demo 18 不显示滑条，Demo 19–20 只显示磨皮，Demo 21–28 显示磨皮和暖色补正；Demo 29 显示暖色补正和饱和度，Demo 30–47 不显示滑条，Demo 48–49 只显示“黑眼圈”滑条，范围为 `0.00–0.15`；当前 `main.frag` 也只显示“黑眼圈”滑条，默认值为 `0.12`。美白范围为 `0.00–0.15`，磨皮和暖色补正范围为 `0.00–1.00`，饱和度范围为 `0.00–0.30`，默认值均为 `0.00`。
4. 只有当参数既通过 SeekBar 传给对应 uniform，又在片元着色器的最终输出计算链路中实际使用时，才显示对应滑条；仅声明 uniform 或仅上传但未参与最终输出时，画面保持不变且不显示滑条。
5. 底部控制面板可收起；收起时面板下滑且测试图片不再被遮挡，点击底部“展开”后面板上滑恢复。
6. 控件是否显示按 Shader 的最终输出判断，而不是只看 uniform 声明；例如 Demo 18 虽声明 `blurStrength`，但最终输出未使用磨皮结果，所以不显示。Demo 29 的“暖色补正”和“饱和度”滑条分别上传 `warmthStrength`、`saturationStrength`，范围为 `0.00–1.00` 和 `0.00–0.30`。

## 练习内容

### 基础（首页“基础”组）

- Demo 1：固定红色
- Demo 2：UV 红绿渐变
- Demo 3：采样坐标偏移
- Demo 4：整体变暗
- Demo 5：整体提亮

当前练习入口（`main.frag`）保留为首页独立条目，不属于任何分组；“黑眼圈”滑条以 `0.00–0.15` 控制下眼区域的提亮幅度，不显示单独的提亮强度滑条。

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

### 亮度与色度

- Demo 22：亮度与色度四分屏
- Demo 23：中间亮度暖色调
- Demo 24：中间亮度权重调试
- Demo 25：暖色综合权重调试
- Demo 26：色度增强
- Demo 27：暖色调色结果
- Demo 28：暖色/饱和度范围
- Demo 29：饱和度独立拖动

## 图像输入与颜色管理

- Demo 30：UV 旋转与前摄镜像
- Demo 31：UV 方向标记调试
- Demo 32：YUV 色度采样
- Demo 33：RGB/YUV 往返转换
- Demo 34：Gamma 亮度对比
- Demo 35：白平衡暖色校正
- Demo 36：白平衡与曝光补偿
- Demo 37：RGB 通道四栏调试（模拟偏色图与 R、G、B 通道灰度）

## 第五章 人脸分析与区域遮罩

- Demo 38：矩形区域遮罩（四个 step 边界限定 UV 中央区域）
- Demo 39：矩形羽化遮罩（smoothstep 平滑过渡四条边）
- Demo 40：羽化矩形局部提亮（遮罩混合原图与固定增加 0.25 的 RGB 提亮结果）
- Demo 41：HSV 肤色候选遮罩（H、S 与 V 的阴影、高光保护共同限定候选权重）
- Demo 42：肤色与区域联合遮罩（HSV 肤色候选权重与羽化矩形区域权重相乘）
- Demo 43：手工人脸框遮罩（由固定中心和宽高计算矩形边界，不使用 SDK 人脸数据）
- Demo 44：像素坐标转 UV 标记（模拟像素坐标除以设定图片尺寸，在原图上标记红点）
- Demo 45：左眼椭圆距离调试（手工中心与横纵半径生成由黑到白的距离灰度图）
- Demo 46：双眼椭圆羽化遮罩（左右眼距离羽化后取较大权重）
- Demo 47：人脸五官保护肤色遮罩（人脸框内筛选肤色并排除眼睛、嘴唇区域）
- Demo 48：黑眼圈区域提亮（在人脸框和肤色候选范围内使用双眼下方羽化权重，黑眼圈强度范围为 0.00–0.15）
- Demo 49：SDK 双眼区域黑眼圈提亮（使用 SDK 双眼中心与轮廓半径生成下眼区域，黑眼圈强度范围为 0.00–0.15）

## 目录说明

```text
app/src/main/assets/main.frag   # 当前练习中的主片元 Shader
app/src/main/assets/main.vert   # 共用主顶点 Shader
    app/src/main/assets/shaders/    # 按 basic、whitening、skin_smoothing、local_color_difference_debug、brightness_and_chroma、image_input_and_color_management、face_analysis_and_mask 分类的 Demo
app/src/main/java/.../          # Android 页面、列表与 OpenGL 渲染器
app/src/main/res/drawable-nodpi # 两张本地测试人像
```

`main.frag` 是持续编写新练习的工作文件。需要归档时，以其中的 `void main()` 为主体，只保留 `main()` 实际引用或调用的声明、常量、函数及其注释，放入对应分类目录，再在 `ShaderDemoCatalog` 中添加首页条目。

## 渲染链路

`MainActivity` 选择 Demo → `ShaderDemoActivity` 创建 `ShaderSurfaceView` → `ShaderRenderer` 编译 Shader、上传图片纹理并调用 `glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)` → GPU 执行片元着色器。
