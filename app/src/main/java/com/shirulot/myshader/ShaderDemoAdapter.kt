package com.shirulot.myshader

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** RecyclerView 适配器只绑定文案和点击事件，不持有 Activity。 */
class ShaderDemoAdapter(
    private val standaloneItems: List<ShaderDemo>,
    private val groups: List<ShaderDemoGroup>,
    private val onItemClick: (ShaderDemo) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val expandedGroupIds = groups
        .filter(ShaderDemoGroup::initiallyExpanded)
        .mapTo(mutableSetOf(), ShaderDemoGroup::titleRes)
    private val visibleItems = mutableListOf<ListItem>()

    init {
        rebuildVisibleItems()
    }

    override fun getItemViewType(position: Int): Int = when (visibleItems[position]) {
        is ListItem.Demo -> VIEW_TYPE_DEMO
        is ListItem.Group -> VIEW_TYPE_GROUP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutRes = when (viewType) {
            VIEW_TYPE_DEMO -> R.layout.item_shader_demo
            VIEW_TYPE_GROUP -> R.layout.item_shader_demo_group
            else -> error("未知列表类型：$viewType")
        }
        val itemView = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return when (viewType) {
            VIEW_TYPE_DEMO -> DemoViewHolder(itemView)
            else -> GroupViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = visibleItems[position]) {
            is ListItem.Demo -> (holder as DemoViewHolder).bind(item.demo, onItemClick)
            is ListItem.Group -> (holder as GroupViewHolder).bind(
                group = item.group,
                isExpanded = item.group.titleRes in expandedGroupIds,
                onToggle = ::toggleGroup,
            )
        }
    }

    override fun getItemCount(): Int = visibleItems.size

    private fun toggleGroup(group: ShaderDemoGroup) {
        if (group.demos.isEmpty()) return

        val groupPosition = visibleItems.indexOfFirst { item ->
            item is ListItem.Group && item.group.titleRes == group.titleRes
        }
        check(groupPosition >= 0) { "找不到分组：${group.titleRes}" }

        if (expandedGroupIds.remove(group.titleRes)) {
            visibleItems.subList(
                groupPosition + 1,
                groupPosition + 1 + group.demos.size,
            ).clear()
            notifyItemChanged(groupPosition)
            notifyItemRangeRemoved(groupPosition + 1, group.demos.size)
        } else {
            expandedGroupIds.add(group.titleRes)
            visibleItems.addAll(
                groupPosition + 1,
                group.demos.map(ListItem::Demo),
            )
            notifyItemChanged(groupPosition)
            notifyItemRangeInserted(groupPosition + 1, group.demos.size)
        }
    }

    private fun rebuildVisibleItems() {
        visibleItems.clear()
        visibleItems += standaloneItems.map(ListItem::Demo)
        groups.forEach { group ->
            visibleItems += ListItem.Group(group)
            if (group.titleRes in expandedGroupIds) {
                visibleItems += group.demos.map(ListItem::Demo)
            }
        }
    }

    class DemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.shader_demo_item_title)
        private val description: TextView = itemView.findViewById(R.id.shader_demo_item_description)

        fun bind(item: ShaderDemo, onItemClick: (ShaderDemo) -> Unit) {
            title.setText(item.titleRes)
            description.setText(item.descriptionRes)
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.shader_demo_group_title)
        private val subtitle: TextView = itemView.findViewById(R.id.shader_demo_group_subtitle)
        private val indicator: ImageView = itemView.findViewById(R.id.shader_demo_group_indicator)

        fun bind(
            group: ShaderDemoGroup,
            isExpanded: Boolean,
            onToggle: (ShaderDemoGroup) -> Unit,
        ) {
            title.setText(group.titleRes)
            val isExpandable = group.demos.isNotEmpty()
            val stateRes = when {
                !isExpandable -> R.string.shader_demo_group_pending
                isExpanded -> R.string.shader_demo_group_collapse
                else -> R.string.shader_demo_group_expand
            }
            if (isExpandable) {
                subtitle.text = itemView.resources.getQuantityString(
                    R.plurals.shader_demo_group_count,
                    group.demos.size,
                    group.demos.size,
                )
                indicator.setImageResource(
                    if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
                )
                indicator.contentDescription = itemView.context.getString(stateRes)
                indicator.visibility = View.VISIBLE
            } else {
                subtitle.setText(R.string.shader_demo_group_pending)
                indicator.visibility = View.INVISIBLE
            }
            itemView.contentDescription = itemView.context.getString(
                R.string.shader_demo_group_content_description,
                itemView.context.getString(group.titleRes),
                itemView.context.getString(stateRes),
            )
            itemView.isClickable = isExpandable
            itemView.isFocusable = isExpandable
            itemView.setOnClickListener(
                if (!isExpandable) null else View.OnClickListener { onToggle(group) },
            )
        }
    }

    private sealed interface ListItem {
        data class Demo(val demo: ShaderDemo) : ListItem
        data class Group(val group: ShaderDemoGroup) : ListItem
    }

    private companion object {
        const val VIEW_TYPE_DEMO = 0
        const val VIEW_TYPE_GROUP = 1
    }
}
