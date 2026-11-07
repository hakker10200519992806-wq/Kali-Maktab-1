package uz.kali.kali_oquv

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

object ShpData {
    data class Item(val sec: String, val cmd: String, val desc: String)

    private const val PREFS = "shpargalka"

    @Volatile
    private var cache: List<Item>? = null

    fun items(ctx: Context): List<Item> {
        cache?.let { return it }
        val out = ArrayList<Item>()
        ctx.resources.openRawResource(R.raw.shpargalka)
            .bufferedReader(Charsets.UTF_8)
            .useLines { lines ->
                for (ln in lines) {
                    val p = ln.split('\t')
                    if (p.size >= 3) out.add(Item(p[0], p[1], p[2]))
                }
            }
        cache = out
        return out
    }

    fun sections(ctx: Context): List<String> = items(ctx).map { it.sec }.distinct()

    fun secIndex(ctx: Context): Int {
        val n = sections(ctx).size
        if (n == 0) return 0
        val v = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("sec", 0)
        return Math.floorMod(v, n)
    }

    fun setSec(ctx: Context, i: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("sec", i).apply()
    }

    fun rows(ctx: Context): List<Item> {
        val s = sections(ctx).getOrNull(secIndex(ctx)) ?: return emptyList()
        return items(ctx).filter { it.sec == s }
    }
}

class ShpargalkaService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(applicationContext)

    private class Factory(private val ctx: Context) : RemoteViewsFactory {
        private var rows: List<ShpData.Item> = emptyList()

        override fun onCreate() {
            rows = ShpData.rows(ctx)
        }

        override fun onDataSetChanged() {
            rows = ShpData.rows(ctx)
        }

        override fun onDestroy() {}

        override fun getCount(): Int = rows.size

        override fun getViewAt(position: Int): RemoteViews {
            val rv = RemoteViews(ctx.packageName, R.layout.shp_row)
            val r = rows.getOrNull(position) ?: return rv
            rv.setTextViewText(R.id.row_cmd, r.cmd)
            rv.setTextViewText(R.id.row_desc, r.desc)
            rv.setOnClickFillInIntent(R.id.row_root, Intent())
            return rv
        }

        override fun getLoadingView(): RemoteViews? = null

        override fun getViewTypeCount(): Int = 1

        override fun getItemId(position: Int): Long = position.toLong()

        override fun hasStableIds(): Boolean = false
    }
}
