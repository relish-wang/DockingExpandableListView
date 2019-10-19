package wang.relish.widget.delv

import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter

import androidx.annotation.LayoutRes

/**
 * @author qianxin, relish
 * @since 20161121
 */
abstract class DockingExpandableListViewAdapter<Group : IGroup<Child>, Child>(protected var mData: List<Group>) : BaseExpandableListAdapter() {

    override fun getGroupCount(): Int {
        return mData.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        val group = mData[groupPosition]
        val children = group.children()
        return children.size
    }

    override fun getGroup(groupPosition: Int): Group {
        return mData[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Child? {
        val group = mData[groupPosition]
        val children = group.children()
        return children[childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return 0
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    @LayoutRes
    abstract fun dockingHeaderResId(): Int

    /**
     * dockingHeader默认使用group
     */
    fun getDockingHeaderView(groupPosition: Int, isExpanded: Boolean, convertView: View, parent: ViewGroup): View {
        return getGroupView(groupPosition, isExpanded, convertView, parent)
    }
}
