package uz.kali.kali_oquv

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews

class ShpargalkaWidget : AppWidgetProvider() {

    companion object {
        private const val ACT_NEXT = "uz.kali.kali_oquv.SHP_NEXT"

        private fun bcast(ctx: Context, act: String, code: Int): PendingIntent {
            val i = Intent(ctx, ShpargalkaWidget::class.java).setAction(act)
            return PendingIntent.getBroadcast(
                ctx, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val secs = ShpData.sections(ctx)
            if (secs.isEmpty()) return
            val si = ShpData.secIndex(ctx)

            val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            val tplFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)

            for (id in ids) {
                val rv = RemoteViews(ctx.packageName, R.layout.widget_shpargalka)
                rv.setTextViewText(R.id.shp_title, secs[si])
                rv.setTextViewText(R.id.shp_count, "${si + 1}/${secs.size}")

                val svc = Intent(ctx, ShpargalkaService::class.java)
                svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                svc.data = Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME))
                rv.setRemoteAdapter(R.id.shp_list, svc)

                val next = bcast(ctx, ACT_NEXT, 1)
                rv.setOnClickPendingIntent(R.id.shp_head, next)
                rv.setOnClickPendingIntent(R.id.shp_bar, next)

                if (launch != null) {
                    rv.setPendingIntentTemplate(
                        R.id.shp_list,
                        PendingIntent.getActivity(ctx, 4, launch, tplFlags)
                    )
                }
                mgr.updateAppWidget(id, rv)
            }
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.shp_list)
        }
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        render(ctx, mgr, ids)
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action != ACT_NEXT) return
        ShpData.setSec(ctx, ShpData.secIndex(ctx) + 1)
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, ShpargalkaWidget::class.java))
        render(ctx, mgr, ids)
    }
}
