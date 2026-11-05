package gt.edu.usac.gtvs

import android.app.Activity
import android.app.Dialog
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.JavascriptInterface
import android.widget.*
import android.view.View
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.OffsetDateTime
import java.util.UUID

/** Formularios y navegación nativos; Leaflet se utiliza solo como superficie de mapa. */
class MainActivity:Activity(){
 private val api=ApiClient()
 private lateinit var root:FrameLayout
 private lateinit var sheet:LinearLayout
 private var reportCoordinate:TextView?=null
 private lateinit var panel:LinearLayout
 private lateinit var map:WebView
 private lateinit var status:TextView
 private var origin:JSONArray?=null;private var destination:JSONArray?=null;private var reportPoint:JSONArray?=null
 private var pick="destination";private var tab="rutas";private var loading=false
 private var key:String?=null;private var payloadText=""
 private var authDialog:Dialog?=null
 private val navy=Color.rgb(19,63,91);private val green=Color.rgb(20,118,93)
 private fun surface(color:Int,radius:Float)=android.graphics.drawable.GradientDrawable().apply{setColor(color);cornerRadius=radius*resources.displayMetrics.density}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun column()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
 private fun text(t:String,size:Float=14f,bold:Boolean=false)=TextView(this).apply{text=t;textSize=size;setTextColor(navy);typeface=android.graphics.Typeface.create("sans-serif",if(bold)android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL);setPadding(0,dp(5),0,dp(6));if(bold)setTypeface(null,android.graphics.Typeface.BOLD)}
 private fun button(t:String,fn:()->Unit)=Button(this).apply{text=t;isAllCaps=false;typeface=android.graphics.Typeface.create("sans-serif-medium",0);textSize=12f;setTextColor(Color.WHITE);backgroundTintList=android.content.res.ColorStateList.valueOf(green);setOnClickListener{if(!loading)fn()}}
 private fun field(h:String)=EditText(this).apply{hint=h;textSize=14f;typeface=android.graphics.Typeface.create("sans-serif",0);setTextColor(navy);setHintTextColor(Color.rgb(102,124,136));setPadding(dp(12),dp(7),dp(12),dp(7));setSingleLine()}
 override fun onCreate(state:Bundle?){super.onCreate(state)
  root=FrameLayout(this).apply{setBackgroundColor(Color.rgb(241,246,244))}
  root.setOnApplyWindowInsetsListener{v,i->@Suppress("DEPRECATION") v.setPadding(0,i.systemWindowInsetTop,0,i.systemWindowInsetBottom);i}
  map=WebView(this);map.settings.javaScriptEnabled=true;map.settings.allowFileAccess=false;map.settings.allowContentAccess=false;map.settings.userAgentString="GTVS/2.0 academic-demo (github.com/RSebXC/GTVS-2027)"
  map.addJavascriptInterface(MapBridge(),"GTVSMap")
  map.webViewClient=object:WebViewClient(){override fun onPageFinished(view:WebView?,url:String?){refreshContext()};override fun shouldOverrideUrlLoading(view:WebView?,request:WebResourceRequest?)=true}
  val html=assets.open("map.html").bufferedReader().use{it.readText()}
  map.loadDataWithBaseURL("https://rsebxc.github.io/GTVS-2027/",html,"text/html","UTF-8",null)
  root.addView(map,FrameLayout.LayoutParams(-1,-1))
  val header=column().apply{setPadding(dp(14),dp(8),dp(14),dp(8));background=surface(Color.rgb(27,43,51),0f)}
  val identity=LinearLayout(this).apply{gravity=android.view.Gravity.CENTER_VERTICAL}
  identity.addView(ImageView(this).apply{setImageResource(R.drawable.logo_gtvs);contentDescription="Logotipo GTVS"},LinearLayout.LayoutParams(dp(38),dp(38)))
  identity.addView(text("  GTVS",22f,true).apply{setTextColor(Color.WHITE)},LinearLayout.LayoutParams(0,-2,1f))
  identity.addView(text("MAPA REAL\nPRUEBA ACADÉMICA",9f).apply{setTextColor(Color.rgb(227,203,148))});header.addView(identity)
  header.addView(text("Incidentes ficticios · Ninguna ruta garantiza seguridad",10f).apply{setTextColor(Color.rgb(221,231,232))})
  root.addView(header,FrameLayout.LayoutParams(-1,-2,android.view.Gravity.TOP))
  sheet=column().apply{setPadding(dp(16),0,dp(16),dp(5));background=surface(Color.rgb(249,251,250),24f);elevation=dp(10).toFloat()}
  val handle=text("━",20f).apply{gravity=android.view.Gravity.CENTER;contentDescription="Arrastrar para ajustar el panel"}
  var initialY=0f;var initialHeight=0
  handle.setOnTouchListener{_,event->when(event.actionMasked){android.view.MotionEvent.ACTION_DOWN->{initialY=event.rawY;initialHeight=sheet.height;true};android.view.MotionEvent.ACTION_MOVE->{val h=(initialHeight+initialY-event.rawY).toInt().coerceIn(dp(200),(root.height*.78f).toInt().coerceAtLeast(dp(200)));sheet.layoutParams=FrameLayout.LayoutParams(-1,h,android.view.Gravity.BOTTOM);map.evaluateJavascript("window.gtvsSheet="+h/resources.displayMetrics.density,null);true};else->true}}
  sheet.addView(handle,LinearLayout.LayoutParams(-1,dp(28)))
  status=text("Busca un destino o selecciona puntos en el mapa.",10f);sheet.addView(status)
  val scroll=ScrollView(this);panel=column();scroll.addView(panel);sheet.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val tabs=LinearLayout(this).apply{setPadding(0,dp(7),0,0)};for((title,id) in listOf("↗ Rutas" to "rutas","＋ Reportar" to "reporte","○ Cuenta" to "cuenta")){tabs.addView(button(title){tab=id;render()},LinearLayout.LayoutParams(0,dp(46),1f).apply{setMargins(dp(2),0,dp(2),0)})};sheet.addView(tabs)
  val height=(resources.displayMetrics.heightPixels*.47f).toInt();root.addView(sheet,FrameLayout.LayoutParams(-1,height,android.view.Gravity.BOTTOM))
  setContentView(root);root.requestApplyInsets();render()
 }
 private fun request(path:String,body:JSONObject?=null,key:String?=null,done:(JSONObject)->Unit){loading=true;status.text="Consultando GTVS…";api.call(path,body,key){data,error->runOnUiThread{if(isFinishing||isDestroyed)return@runOnUiThread;loading=false;if(error!=null)status.text=error else if(data!=null){status.text="Servicio compartido conectado · demostración";done(data)}}}}
 inner class MapBridge{@JavascriptInterface fun point(lon:Double,lat:Double){runOnUiThread{if(!lon.isFinite()||!lat.isFinite()||lon !in -90.69..-90.35||lat !in 14.45..14.8){status.text="Fuera del ámbito de demostración";return@runOnUiThread};val p=JSONArray().put(lon).put(lat);when(pick){"origin"->origin=p;"report"->reportPoint=p;else->destination=p};map.evaluateJavascript("setPoint(${JSONObject.quote(pick)},${p})",null);status.text="${if(pick=="report")"Reporte" else if(pick=="origin")"Origen" else "Destino"}: %.5f, %.5f".format(lat,lon);if(tab=="reporte")reportCoordinate?.text="Punto seleccionado: $reportPoint" else render()}}}
 private fun render(){panel.removeAllViews();when(tab){"reporte"->reportScreen();"cuenta"->accountScreen();else->routeScreen()}}
 private fun routeScreen(){panel.addView(text("¿A dónde quieres ir?",22f,true))
  fun searchRow(label:String,mode:String,value:JSONArray?){val row=LinearLayout(this);val q=field(if(value==null)label else "${value.optDouble(1)}, ${value.optDouble(0)}");row.addView(q,LinearLayout.LayoutParams(0,-2,1f));row.addView(button("Buscar"){val query=q.text.toString();if(query.length<3){status.text="Escribe al menos tres caracteres";return@button};request("/search?q="+URLEncoder.encode(query,"UTF-8")){data->val list=data.getJSONArray("places");if(list.length()==0){status.text="Sin resultados. Marca un punto en el mapa."}else{val labels=(0 until list.length()).map{list.getJSONObject(it).getString("name")}.toTypedArray();AlertDialog.Builder(this).setTitle(label).setItems(labels){_,i->val p=list.getJSONObject(i).getJSONArray("coordinate");if(mode=="origin")origin=p else destination=p;map.evaluateJavascript("setPoint(${JSONObject.quote(mode)},$p)",null);render()}.show()}}});panel.addView(row);panel.addView(button("Marcar $label en el mapa"){pick=mode;status.text="Toca el mapa para marcar $label"})}
  searchRow("Origen","origin",origin);searchRow("Destino","destination",destination)
  val result=column();panel.addView(button("Comparar alternativas"){if(origin==null||destination==null){status.text="Selecciona ambos puntos";return@button};request("/routes",JSONObject().put("origin",origin).put("destination",destination)){data->result.removeAllViews();val routes=data.getJSONArray("routes");map.evaluateJavascript("showRoutes($routes,0)",null);for(i in 0 until routes.length()){val r=routes.getJSONObject(i);val card=column().apply{setPadding(dp(12),dp(12),dp(12),dp(12));setBackgroundColor(Color.WHITE)};val profiles=r.getJSONArray("profiles");val names=(0 until profiles.length()).map{when(profiles.getString(it)){"rapida"->"Rápida";"equilibrada"->"Equilibrada";else->"Menor riesgo estimado"}};card.addView(text(names.joinToString(" · "),16f,true));card.addView(text("${kotlin.math.ceil(r.getDouble("duration")/60).toInt()} min · %.1f km".format(r.getDouble("distance")/1000)));val risk=r.getJSONObject("risk");card.addView(text("Índice ilustrativo: %.2f · %d incidentes ficticios".format(risk.getDouble("value"),risk.getInt("uniqueIncidents")),11f));card.addView(button("Ver recorrido"){map.evaluateJavascript("showRoutes($routes,$i)",null)});result.addView(card)};result.addView(text("Cobertura real desconocida. Se comparan candidatos de OSRM; no se garantiza el mínimo global.",11f))}});panel.addView(result)
 }
 private fun reportScreen(){panel.addView(text("Reporte ficticio",22f,true));if(api.token==null){panel.addView(text("Inicia sesión desde Cuenta para registrar un aporte."));panel.addView(button("Iniciar sesión"){login()});return}
  panel.addView(button("Marcar ubicación del reporte"){pick="report";status.text="Toca el mapa para ubicar el ejemplo"});reportCoordinate=text(if(reportPoint==null)"Ubicación pendiente" else "Punto: $reportPoint",11f);panel.addView(reportCoordinate)
  val type=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Robo o ataque ficticio","Obstrucción ficticia","Cierre ficticio","Otro contexto ficticio"))};panel.addView(type)
  val severity=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,(1..5).map{"Gravedad demostrativa $it"});setSelection(2)};panel.addView(severity)
  val time=field("Ocurrencia ISO 8601 con zona o vacía");panel.addView(time);panel.addView(button("Usar hora actual de prueba"){time.setText(OffsetDateTime.now().toString())})
  val description=field("Descripción inventada sin datos personales").apply{setSingleLine(false);minLines=3;filters=arrayOf(android.text.InputFilter.LengthFilter(1500))};panel.addView(description)
  val confirm=CheckBox(this).apply{text="Confirmo que es una simulación ficticia"};panel.addView(confirm)
  panel.addView(button("Enviar para revisión"){if(reportPoint==null||!confirm.isChecked){status.text="Marca una ubicación y confirma la condición ficticia";return@button};val kinds=listOf("robo_ataque","obstruccion","cierre","otro");val body=JSONObject().put("type",kinds[type.selectedItemPosition]).put("coordinate",reportPoint).put("description",description.text.toString()).put("severity",severity.selectedItemPosition+1).put("occurredAt",time.text.toString().trim().ifEmpty{null}?:JSONObject.NULL).put("synthetic",true);val serial=body.toString();if(serial!=payloadText){payloadText=serial;key=UUID.randomUUID().toString()};request("/reports",body,key){data->status.text="Reporte ${data.getJSONObject("report").getString("id").take(8)} guardado como pendiente";key=null;payloadText="";description.setText("")}})
 }
 private fun accountScreen(){panel.addView(text("Tu cuenta GTVS",22f,true));panel.addView(text("La administración se realiza desde la web. El teléfono y el navegador consultan la misma API y base de datos."));panel.addView(button(if(api.token==null)"Iniciar sesión o crear cuenta" else "Renovar sesión"){login()});panel.addView(button("Cerrar sesión local"){api.token=null;status.text="Token retirado de la memoria de la app";render()});panel.addView(text("La sesión de la app se mantiene en memoria y su token vence. Si expira, inicia sesión nuevamente. No se incluyen credenciales de Postgres en la APK.",11f));panel.addView(button("Actualizar incidentes del mapa"){refreshContext()})}
 @Suppress("SetJavaScriptEnabled") private fun login(){
  val dialog=Dialog(this);authDialog=dialog;val web=WebView(this);web.settings.javaScriptEnabled=true;web.settings.domStorageEnabled=true;web.settings.allowFileAccess=false;web.settings.allowContentAccess=false
  web.addJavascriptInterface(object{@JavascriptInterface fun acceptToken(token:String){if(token.length !in 50..10000)return;runOnUiThread{val previous=api.token;api.token=token;api.call("/me"){data,error->runOnUiThread{if(error!=null||data==null){api.token=previous;status.text=error?:"No se pudo validar la sesión"}else{dialog.dismiss();status.text="Cuenta conectada. Ya puedes reportar.";render()}}}}}},"GTVSMobile")
  web.webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(view:WebView?,request:WebResourceRequest?):Boolean{if(request?.isForMainFrame==false)return false;val uri=request?.url?:return true;return uri.scheme!="https"||uri.host!="rsebxc.github.io"||!uri.path.orEmpty().startsWith("/GTVS-2027/")}}
  dialog.setContentView(web);dialog.window?.setLayout(-1,-1);dialog.setOnDismissListener{web.removeJavascriptInterface("GTVSMobile");web.destroy()};dialog.show();dialog.window?.setLayout(-1,-1);web.loadUrl(BuildConfig.AUTH_WEB_URL)
 }
 private fun refreshContext(){request("/incidents"){i->request("/restrictions"){r->map.evaluateJavascript("showContext(${i.getJSONArray("incidents")},${r.getJSONArray("restrictions")})",null)}}}
 @Deprecated("Deprecated in Java") override fun onBackPressed(){if(authDialog?.isShowing==true){authDialog?.dismiss();return};if(tab!="rutas"){tab="rutas";render();return};super.onBackPressed()}
 override fun onDestroy(){authDialog?.dismiss();map.removeJavascriptInterface("GTVSMap");map.destroy();api.close();super.onDestroy()}
}
