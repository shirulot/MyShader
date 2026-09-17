package com.shirulot.myshader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** 首页只负责展示 Demo 列表，并把选中的 Shader 信息交给渲染页。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shader_list)

        val demoList = findViewById<RecyclerView>(R.id.shader_demo_list)
        demoList.layoutManager = LinearLayoutManager(this)
        demoList.adapter = ShaderDemoAdapter(
            standaloneItems = ShaderDemoCatalog.standaloneItems,
            groups = ShaderDemoCatalog.groups,
        ) { demo ->
            // 只传资源路径、展示标题和当前分类允许的控件，渲染页继续复用同一套 OpenGL 管线。
            startActivity(
                ShaderDemoActivity.createIntent(
                    context = this,
                    fragmentShaderAsset = demo.fragmentShaderAsset,
                    demoTitle = getString(demo.titleRes),
                    initialWhitenStrength = demo.initialWhitenStrength,
                    initialBrightenStrength = demo.initialBrightenStrength,
                    initialBlackCircleStrength = demo.initialBlackCircleStrength,
                    showWhitenStrengthControl = demo.showWhitenStrengthControl,
                    showBrightenStrengthControl = demo.showBrightenStrengthControl,
                    showBlackCircleStrengthControl = demo.showBlackCircleStrengthControl,
                    showBlurStrengthControl = demo.showBlurStrengthControl,
                    showWarmthStrengthControl = demo.showWarmthStrengthControl,
                    showSaturationStrengthControl = demo.showSaturationStrengthControl,
                ),
            )
        }

        val root = findViewById<android.view.View>(R.id.shader_home_root)
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = initialLeft + systemBars.left,
                top = initialTop + systemBars.top,
                right = initialRight + systemBars.right,
                bottom = initialBottom + systemBars.bottom,
            )
            insets
        }
    }
}
