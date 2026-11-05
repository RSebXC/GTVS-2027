package gt.edu.usac.rutascontexto

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.view.View
import org.json.JSONObject

class NetworkView(context: Context) : View(context) {
    var scenario: JSONObject? = null
        set(value) { field = value; invalidate() }
    var selected: Set<String> = emptySet()
        set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    init { contentDescription = "Esquema de nueve puntos ficticios. No es un mapa de calles reales." }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(245,249,250))
        val data = scenario ?: return
        val nodes = data.getJSONArray("nodos")
        val points = mutableMapOf<String, Pair<Float, Float>>()
        for (i in 0 until nodes.length()) {
            val n = nodes.getJSONObject(i)
            points[n.getString("id")] = Pair(n.getDouble("x").toFloat()/720*width, n.getDouble("y").toFloat()/520*height)
        }
        val edges = data.getJSONArray("tramos")
        for (i in 0 until edges.length()) {
            val e = edges.getJSONObject(i)
            val a = points[e.getString("origen")] ?: continue
            val b = points[e.getString("destino")] ?: continue
            val chosen = e.getString("id") in selected
            paint.color = if (chosen) Color.rgb(23,108,104) else Color.rgb(188,204,212)
            paint.strokeWidth = if (chosen) 8f else 5f
            paint.pathEffect = if (e.getBoolean("cobertura")) null else DashPathEffect(floatArrayOf(12f,10f),0f)
            canvas.drawLine(a.first,a.second,b.first,b.second,paint)
        }
        paint.pathEffect = null
        for ((name,p) in points) {
            paint.color = Color.rgb(20,54,75)
            canvas.drawCircle(p.first,p.second,17f*resources.displayMetrics.density/1.5f,paint)
            paint.color=Color.WHITE; paint.textSize=16f*resources.displayMetrics.scaledDensity
            paint.textAlign=Paint.Align.CENTER
            canvas.drawText(name,p.first,p.second-(paint.ascent()+paint.descent())/2,paint)
        }
    }
}
