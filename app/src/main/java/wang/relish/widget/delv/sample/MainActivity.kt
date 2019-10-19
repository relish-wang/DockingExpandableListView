package wang.relish.widget.delv.sample

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xinxin.dockingexpandablelistview.R
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_item.view.*
import wang.relish.widget.delv.DockingExpandableListViewAdapter
import wang.relish.widget.delv.IGroup
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Serializable

/**
 * @author relsih
 * @since20191019
 */
class MainActivity : AppCompatActivity() {


    private val mData = ArrayList<Catalog>()
    private lateinit var mAdapter: CatalogAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        docking_list_view.setGroupIndicator(null)
        docking_list_view.overScrollMode = View.OVER_SCROLL_NEVER
        mAdapter = CatalogAdapter(mData)
        docking_list_view.setAdapter(mAdapter)
        docking_list_view.setOnGroupClickListener { parent, _, groupPosition, _ ->
            if (parent.isGroupExpanded(groupPosition)) {
                parent.collapseGroup(groupPosition)
            } else {
                parent.expandGroup(groupPosition)
            }
            true
        }
        docking_list_view.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val title = mData[groupPosition].items[childPosition].name
            Toast.makeText(this, title, Toast.LENGTH_SHORT).show()
            true
        }
        loadData()
    }

    private fun loadData() {

        Observable.create(ObservableOnSubscribe<List<Catalog>> { emitter ->
            val data = getData(this@MainActivity)
            emitter.onNext(data)
            emitter.onComplete()
        }
        ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<List<Catalog>> {
                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onNext(t: List<Catalog>) {
                        mData.clear()
                        mData.addAll(t)
                        mAdapter.notifyDataSetChanged()
                    }

                    override fun onError(e: Throwable) {
                    }

                    override fun onComplete() {
                    }

                })
    }

    /////////////////////////////////// 适配器相关 ///////////////////////////////////

    private class CatalogAdapter(
            data: List<Catalog>
    ) : DockingExpandableListViewAdapter<Catalog, Catalog.Item>(data) {

        override fun dockingHeaderResId(): Int {
            return R.layout.item_catalog
        }

        companion object {
            val NUMBER = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
        }

        override fun getGroupView(
                groupPosition: Int,
                isExpanded: Boolean,
                convertView: View?,
                parent: ViewGroup
        ): View? {
            var convertViewLocal = convertView
            if (groupPosition < 0) return null
            if (convertViewLocal == null) {
                convertViewLocal = LayoutInflater.from(parent.context).inflate(
                        R.layout.item_catalog, parent, false)
            }
            val title = "${NUMBER[groupPosition]}、${mData[groupPosition].name}"
            convertViewLocal!!.tv_name.text = title
            return convertViewLocal
        }

        override fun getChildView(
                groupPosition: Int,
                childPosition: Int,
                isLastChild: Boolean,
                convertView: View?,
                parent: ViewGroup
        ): View? {
            var convertViewLocal = convertView
            val children = mData[groupPosition].items
            if (childPosition < 0 || childPosition > children.size) return null
            if (convertViewLocal == null) {
                convertViewLocal = LayoutInflater.from(parent.context).inflate(
                        R.layout.item_item, parent, false)
            }
            val title = "${groupPosition + 1}.${childPosition + 1} ${children[childPosition].name}"
            convertViewLocal!!.tv_name.text = title
            return convertViewLocal
        }

    }

    /////////////////////////////////// 数据相关 ///////////////////////////////////

    data class Catalog(
            val name: String,
            val items: List<Item>
    ) : IGroup<Catalog.Item>, Serializable {

        override fun children(): List<Item> {
            return items
        }

        companion object {
            private const val serialVersionUID = 1L
        }

        data class Item(
                val name: String
        ) : Serializable {

            companion object {
                private const val serialVersionUID = 1L
            }
        }
    }

    private fun getData(context: Context): List<Catalog> {
        val json = getJsonFromAssets(context, "data.json")
        return Gson().fromJson(json, object : TypeToken<List<Catalog>>() {}.type)
    }

    private fun getJsonFromAssets(
            context: Context,
            @Suppress("SameParameterValue") hostFilePath: String
    ): String? {
        val builder: StringBuilder
        try {
            val stream = context.assets.open(hostFilePath)
            val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
            builder = StringBuilder()
            var str: String
            do {
                try {
                    str = reader.readLine()
                } catch (e: IllegalStateException) {
                    break// 读到文件末尾了
                }
                if (str == null) break
                builder.append(str)
            } while (true)
            reader.close()
            return builder.toString()
        } catch (e: IOException) {
            return null
        }
    }
}