package gt.edu.usac.rutascontexto

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*
import org.json.JSONObject
import java.time.OffsetDateTime
import java.util.UUID

/** Interfaz Views nativas; cálculo y persistencia pertenecen al backend compartido. */
class MainActivity : Activity() {
    private val client=ApiClient()
    private lateinit var body: LinearLayout
    private lateinit var status: TextView
    private var baseUrl="http://10.0.2.2:8765"
    private var scenario: JSONObject?=null
    private var screen="rutas"
    private var requestRunning=false
    private var pendingPayload: String?=null
    private var pendingKey: String?=null
    private val ink=Color.rgb(21,48,71)
    private val teal=Color.rgb(23,108,104)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun column()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
    private fun text(value:String,size:Float=15f,bold:Boolean=false)=TextView(this).apply{
        text=value; textSize=size;setTextColor(ink);setPadding(0,dp(6),0,dp(8))
        if(bold)setTypeface(null,android.graphics.Typeface.BOLD)
    }
    private fun button(value:String, action:()->Unit)=Button(this).apply{
        text=value;isAllCaps=false;setTextColor(Color.WHITE);setBackgroundTintList(android.content.res.ColorStateList.valueOf(teal))
        setOnClickListener{if(!requestRunning)action()}
    }
    private fun spinner(values:List<String>)=Spinner(this).apply{
        adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,values)
        layoutParams=LinearLayout.LayoutParams(-1,dp(52))
    }
    private fun input(hintText:String)=EditText(this).apply{hint=hintText;setSingleLine();textSize=15f;layoutParams=LinearLayout.LayoutParams(-1,-2)}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        baseUrl=savedInstanceState?.getString("baseUrl")?:baseUrl
        val root=column().apply{setBackgroundColor(Color.rgb(244,247,248))}
        root.setOnApplyWindowInsetsListener{v,insets->
            @Suppress("DEPRECATION")
            v.setPadding(dp(20),insets.systemWindowInsetTop+dp(12),dp(20),insets.systemWindowInsetBottom)
            insets
        }
        root.addView(text("GTVS",25f,true))
        root.addView(text("PROTOTIPO ACADÉMICO · WEB + ANDROID",11f,true))
        root.addView(text("Escenario sintético. Los puntos y los incidentes son ficticios. No usar para desplazamientos reales.",13f).apply{
            setBackgroundColor(Color.rgb(251,243,221));setPadding(dp(10),dp(10),dp(10),dp(10))
        })
        val tabs=LinearLayout(this)
        for((label,key) in listOf("Rutas" to "rutas","Reportar" to "reporte","Servicio" to "servicio")){
            tabs.addView(button(label){screen=key;render()},LinearLayout.LayoutParams(0,dp(52),1f))
        }
        root.addView(tabs)
        status=text("Conecta el servicio local desde la pestaña Servicio.",12f)
        root.addView(status)
        val scroll=ScrollView(this).apply{isFillViewport=true}
        body=column();scroll.addView(body)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root);root.requestApplyInsets();render();connect()
    }
    override fun onSaveInstanceState(outState:Bundle){super.onSaveInstanceState(outState);outState.putString("baseUrl",baseUrl)}
    private fun call(path:String,payload:JSONObject?=null,key:String?=null,done:(JSONObject)->Unit){
        requestRunning=true;status.text="Consultando servicio…"
        client.request(baseUrl,path,payload,key){result,error->runOnUiThread{
            if(isFinishing||isDestroyed)return@runOnUiThread
            requestRunning=false
            if(error!=null){status.text="No se completó la solicitud: $error"}else if(result!=null){status.text="Servicio conectado · Datos sintéticos";done(result)}
        }}
    }
    private fun connect(){call("/escenario"){scenario=it;render()}}
    private fun render(){body.removeAllViews();when(screen){"servicio"->serviceScreen();"reporte"->reportScreen();else->routeScreen()}}
    private fun serviceScreen(){
        body.addView(text("Servicio compartido",21f,true))
        body.addView(text("Web y app consultan la misma API Python y la misma base SQLite. La app necesita Internet para acceder al servidor; no usa SMS, banca ni ubicación del teléfono."))
        val url=input("http://10.0.2.2:8765").apply{setText(baseUrl)};body.addView(url)
        body.addView(button("Guardar y conectar"){baseUrl=url.text.toString();connect()})
        body.addView(text("Emulador: 10.0.2.2. Teléfono USB: ejecutar adb reverse tcp:8765 tcp:8765 y usar http://127.0.0.1:8765. No se configura un servicio público.",13f))
        body.addView(text("HTTP solo está habilitado en la variante debug. La variante release exige HTTPS. Esta configuración no constituye un despliegue de producción.",13f))
    }
    private fun routeScreen(){
        body.addView(text("Comparar alternativas",21f,true))
        if(scenario==null){body.addView(text("Inicia el servidor y comprueba su dirección en Servicio."));return}
        val nodes=scenario!!.getJSONArray("nodos");val ids=(0 until nodes.length()).map{nodes.getJSONObject(it).getString("id")}
        body.addView(text("Origen",13f,true));val origin=spinner(ids);body.addView(origin)
        body.addView(text("Destino",13f,true));val destination=spinner(ids).apply{setSelection(ids.lastIndex)};body.addView(destination)
        val map=NetworkView(this).apply{scenario=this@MainActivity.scenario};body.addView(map,LinearLayout.LayoutParams(-1,dp(270)))
        body.addView(text("Esquema abstracto. Línea discontinua: información insuficiente.",11f))
        val results=column()
        body.addView(button("Calcular tres perfiles"){
            call("/rutas/calcular",JSONObject().put("origen",origin.selectedItem.toString()).put("destino",destination.selectedItem.toString()).put("perfil","todos")) {res->
                results.removeAllViews()
                val alternatives=res.getJSONArray("alternativas")
                results.addView(text("Revisión ${res.getInt("revision")} · ${alternatives.length()} alternativas distintas",13f,true))
                for(i in 0 until alternatives.length()){
                    val a=alternatives.getJSONObject(i);val edges=a.getJSONArray("tramos");val chosen=(0 until edges.length()).map{edges.getString(it)}.toSet()
                    if(i==0)map.selected=chosen
                    val profiles=a.getJSONArray("perfiles");val labels=(0 until profiles.length()).map{when(profiles.getString(it)){"rapida"->"Rápida";"equilibrada"->"Equilibrada";else->"Menor riesgo estimado"}}
                    val card=column().apply{setPadding(dp(14),dp(14),dp(14),dp(14));setBackgroundColor(Color.WHITE)}
                    card.addView(text(labels.joinToString(" · "),18f,true))
                    card.addView(text("${a.getInt("distancia_m")} m · ${a.getInt("tiempo_estimado_s")} s (valores ficticios)"))
                    card.addView(text("Costo relativo: "+if(a.isNull("costo_riesgo_relativo"))"Información insuficiente" else String.format(java.util.Locale.US,"%.3f",a.getDouble("costo_riesgo_relativo"))))
                    val route=a.getJSONArray("nodos");card.addView(text((0 until route.length()).joinToString(" → "){route.getString(it)},13f))
                    card.addView(button("Mostrar recorrido"){map.selected=chosen})
                    card.addView(text("Suficiencia: ${a.getString("suficiencia")}. No equivale a una probabilidad de victimización.",12f))
                    results.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(14)})
                }
                results.addView(text(res.getString("politica_faltantes"),12f))
            }
        });body.addView(results)
    }
    private fun reportScreen(){
        body.addView(text("Reporte ficticio",21f,true))
        if(scenario==null){body.addView(text("Conecta primero el servicio."));return}
        val edges=scenario!!.getJSONArray("tramos");val ids=(0 until edges.length()).map{edges.getJSONObject(it).getString("id")}
        body.addView(text("Tramo",13f,true));val tramo=spinner(ids);body.addView(tramo)
        body.addView(text("Gravedad ilustrativa, sin calibración",13f,true));val severity=spinner((1..5).map{it.toString()}).apply{setSelection(2)};body.addView(severity)
        body.addView(text("Fecha ISO 8601 con zona, o vacía si se desconoce",12f));val time=input("Ejemplo: 2026-11-04T15:00:00-06:00");body.addView(time)
        body.addView(button("Usar hora actual de prueba"){time.setText(OffsetDateTime.now().toString())})
        body.addView(text("Descripción inventada. No incluyas datos personales.",13f));val description=input("Describe un ejemplo ficticio").apply{setSingleLine(false);minLines=3;filters=arrayOf(android.text.InputFilter.LengthFilter(1000))};body.addView(description)
        body.addView(button("Enviar para revisión"){
            val payload=JSONObject().put("tipo","incidente_contextual").put("tramo",tramo.selectedItem.toString()).put("gravedad",severity.selectedItem.toString().toInt()).put("descripcion",description.text.toString()).put("ocurrido_en",time.text.toString().trim().ifEmpty{null}?:JSONObject.NULL)
            val serial=payload.toString();if(serial!=pendingPayload){pendingPayload=serial;pendingKey=UUID.randomUUID().toString()}
            call("/reportes",payload,pendingKey){res->
                pendingPayload=null;pendingKey=null
                status.text="${res.getString("id")} · ${res.getString("estado")}"
                description.setText("");body.addView(text("Reporte guardado como pendiente. No cambió las rutas. Revisión disponible en el cliente web.",13f,true))
            }
        })
        body.addView(text("El estado pendiente no confirma un incidente. Los reportes sin fecha pueden conservarse, pero no crean automáticamente un hecho temporal.",12f))
    }
    override fun onDestroy(){client.close();super.onDestroy()}
}
