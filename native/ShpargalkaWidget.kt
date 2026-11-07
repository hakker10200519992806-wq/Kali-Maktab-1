package uz.kali.kali_oquv

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ShpargalkaWidget : AppWidgetProvider() {

    data class Item(val sec: String, val cmd: String, val desc: String)

    companion object {
        private const val ACT_NEXT = "uz.kali.kali_oquv.SHP_NEXT"
        private const val ACT_PREV = "uz.kali.kali_oquv.SHP_PREV"
        private const val ACT_SEC = "uz.kali.kali_oquv.SHP_SEC"
        private const val PREFS = "shpargalka"
        private const val STEP_MS = 30L * 60L * 1000L

        @Volatile
        private var cache: List<Item>? = null

        private fun items(ctx: Context): List<Item> {
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

        private fun current(ctx: Context, n: Int): Int {
            val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            if (!sp.contains("ts")) {
                sp.edit().putInt("pos", 0).putLong("ts", now).apply()
            }
            val pos = sp.getInt("pos", 0)
            val ts = sp.getLong("ts", now)
            val steps = ((now - ts) / STEP_MS).toInt()
            return Math.floorMod(pos + steps, n)
        }

        private fun save(ctx: Context, pos: Int) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("pos", pos)
                .putLong("ts", System.currentTimeMillis())
                .apply()
        }

        private fun bcast(ctx: Context, act: String, code: Int): PendingIntent {
            val i = Intent(ctx, ShpargalkaWidget::class.java).setAction(act)
            return PendingIntent.getBroadcast(
                ctx, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            val list = items(ctx)
            if (list.isEmpty() || ids.isEmpty()) return
            val p = current(ctx, list.size)
            val cur = list[p]
            var first = p
            while (first > 0 && list[first - 1].sec == cur.sec) first--
            var last = p
            while (last < list.size - 1 && list[last + 1].sec == cur.sec) last++

            val rv = RemoteViews(ctx.packageName, R.layout.widget_shpargalka)
            rv.setTextViewText(R.id.shp_title, cur.sec)
            rv.setTextViewText(R.id.shp_count, "${p - first + 1}/${last - first + 1}")
            rv.setTextViewText(R.id.shp_cmd, "$ " + cur.cmd)
            rv.setTextViewText(R.id.shp_desc, cur.desc)

            rv.setOnClickPendingIntent(R.id.shp_prev, bcast(ctx, ACT_PREV, 1))
            rv.setOnClickPendingIntent(R.id.shp_next, bcast(ctx, ACT_NEXT, 2))
            rv.setOnClickPendingIntent(R.id.shp_head, bcast(ctx, ACT_SEC, 3))

            val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            if (launch != null) {
                val open = PendingIntent.getActivity(
                    ctx, 4, launch,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                rv.setOnClickPendingIntent(R.id.shp_cmd, open)
                rv.setOnClickPendingIntent(R.id.shp_desc, open)
            }
            mgr.updateAppWidget(ids, rv)
        }
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        render(ctx, mgr, ids)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val act = intent.action ?: return
        if (act != ACT_NEXT && act != ACT_PREV && act != ACT_SEC) return
        val list = items(ctx)
        val n = list.size
        if (n == 0) return
        val p = current(ctx, n)
        val np = when (act) {
            ACT_NEXT -> Math.floorMod(p + 1, n)
            ACT_PREV -> Math.floorMod(p - 1, n)
            else -> {
                val sec = list[p].sec
                var q = p
                do {
                    q = (q + 1) % n
                } while (list[q].sec == sec && q != p)
                q
            }
        }
        save(ctx, np)
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, ShpargalkaWidget::class.java))
        render(ctx, mgr, ids)
    }
}
