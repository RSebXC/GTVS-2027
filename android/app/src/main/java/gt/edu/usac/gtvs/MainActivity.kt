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
 private lateinit var root:LinearLayout
 private lateinit var panel:LinearLayout
 private lateinit var map:WebView
 private lateinit var status:TextView
 private var origin:JSONArray?=null;private var destination:JSONArray?=null;private var reportPoint:JSONArray?=null
 private var pick="destination";private var tab="rutas";private var loading=false
 private var key:String?=null;private var payloadText=""
 private var authDialog:Dialog?=null
 private val navy=Color.rgb(19,63,91);private val green=Color.rgb(20,118,93)
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun column()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
 private fun text(t:String,size:Float=14f,bold:Boolean=false)=TextView(this).apply{text=t;textSize=size;setTextColor(navy);setPadding(0,dp(5),0,dp(6));if(bold)setTypeface(null,android.graphics.Typeface.BOLD)}
 private fun button(t:String,fn:()->Unit)=Button(this).apply{text=t;isAllCaps=false;setTextColor(Color.WHITE);backgroundTintList=android.content.res.ColorStateList.valueOf(green);setOnClickListener{if(!loading)fn()}}
 private fun field(h:String)=EditText(this).apply{hint=h;textSize=14f;setSingleLine()}
 override fun onCreate(state:Bundle?){super.onCreate(state)
  root=column().apply{setBackgroundColor(Color.rgb(246,249,250))}
  root.setOnApplyWindowInsetsListener{v,i->@Suppress("DEPRECATION") v.setPadding(dp(16),i.systemWindowInsetTop+dp(8),dp(16),i.systemWindowInsetBottom);i}
  val identity=LinearLayout(this).apply{gravity=android.view.Gravity.CENTER_VERTICAL};identity.addView(ImageView(this).apply{setImageResource(R.drawable.logo_gtvs);contentDescription="Logotipo GTVS"},LinearLayout.LayoutParams(dp(56),dp(56)));identity.addView(text("  GTVS",27f,true));root.addView(identity);root.addView(text("Guatemala Tránsito y Vialidad Segura",11f))
  root.addView(text("DEMOSTRACIÓN · Mapa real, incidentes ficticios. Ninguna ruta garantiza seguridad.",11f).apply{setBackgroundColor(Color.rgb(249,245,233));setPadding(dp(10),dp(7),dp(10),dp(7))})
  map=WebView(this);map.settings.javaScriptEnabled=true;map.settings.allowFileAccess=false;map.settings.allowContentAccess=false;map.settings.userAgentString="GTVS/2.0 academic-demo (github.com/RSebXC/GTVS-2027)"
  map.addJavascriptInterface(MapBridge(),"GTVSMap")
  map.webViewClient=object:WebViewClient(){override fun onPageFinished(view:WebView?,url:String?){refreshContext()};override fun shouldOverrideUrlLoading(view:WebView?,request:WebResourceRequest?)=true}
  val html=assets.open("map.html").bufferedReader().use{it.readText()}
  map.loadDataWithBaseURL("https://rsebxc.github.io/GTVS-2027/",html,"text/html","UTF-8",null)
  root.addView(map,LinearLayout.LayoutParams(-1,0,1.1f))
  status=text("Selecciona puntos en el mapa o busca un lugar.",11f);root.addView(status)
  val scroll=ScrollView(this);panel=column();scroll.addView(panel);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val tabs=LinearLayout(this);for((title,id) in listOf("Rutas" to "rutas","Reportar" to "reporte","Cuenta" to "cuenta")){tabs.addView(button(title){tab=id;render()},LinearLayout.LayoutParams(0,dp(50),1f))};root.addView(tabs)
  setContentView(root);root.requestApplyInsets();render()
 }
 private fun request(path:String,body:JSONObject?=null,key:String?=null,done:(JSONObject)->Unit){loading=true;status.text="Consultando GTVS…";api.call(path,body,key){data,error->runOnUiThread{if(isFinishing||isDestroyed)return@runOnUiThread;loading=false;if(error!=null)status.text=error else if(data!=null){status.text="Servicio compartido conectado · demostración";done(data)}}}}
 inner class MapBridge{@JavascriptInterface fun point(lon:Double,lat:Double){runOnUiThread{if(!lon.isFinite()||!lat.isFinite()||lon !in -90.69..-90.35||lat !in 14.45..14.8){status.text="Fuera del ámbito de demostración";return@runOnUiThread};val p=JSONArray().put(lon).put(lat);when(pick){"origin"->origin=p;"report"->reportPoint=p;else->destination=p};map.evaluateJavascript("setPoint(${JSONObject.quote(pick)},${p})",null);status.text="${if(pick=="report")"Reporte" else if(pick=="origin")"Origen" else "Destino"}: %.5f, %.5f".format(lat,lon);render()}}}
 private fun render(){panel.removeAllViews();when(tab){"reporte"->reportScreen();"cuenta"->accountScreen();else->routeScreen()}}
 private fun routeScreen(){panel.addView(text("¿A dónde quieres ir?",22f,true))
  fun searchRow(label:String,mode:String,value:JSONArray?){val row=LinearLayout(this);val q=field(if(value==null)label else "${value.optDouble(1)}, ${value.optDouble(0)}");row.addView(q,LinearLayout.LayoutParams(0,-2,1f));row.addView(button("Buscar"){val query=q.text.toString();if(query.length<3){status.text="Escribe al menos tres caracteres";return@button};request("/search?q="+URLEncoder.encode(query,"UTF-8")){data->val list=data.getJSONArray("places");if(list.length()==0){status.text="Sin resultados. Marca un punto en el mapa."}else{val labels=(0 until list.length()).map{list.getJSONObject(it).getString("name")}.toTypedArray();AlertDialog.Builder(this).setTitle(label).setItems(labels){_,i->val p=list.getJSONObject(i).getJSONArray("coordinate");if(mode=="origin")origin=p else destination=p;map.evaluateJavascript("setPoint(${JSONObject.quote(mode)},$p)",null);render()}.show()}}});panel.addView(row);panel.addView(button("Marcar $label en el mapa"){pick=mode;status.text="Toca el mapa para marcar $label"})}
  searchRow("Origen","origin",origin);searchRow("Destino","destination",destination)
  val result=column();panel.addView(button("Comparar alternativas"){if(origin==null||destination==null){status.text="Selecciona ambos puntos";return@button};request("/routes",JSONObject().put("origin",origin).put("destination",destination)){data->result.removeAllViews();val routes=data.getJSONArray("routes");map.evaluateJavascript("showRoutes($routes,0)",null);for(i in 0 until routes.length()){val r=routes.getJSONObject(i);val card=column().apply{setPadding(dp(12),dp(12),dp(12),dp(12));setBackgroundColor(Color.WHITE)};val profiles=r.getJSONArray("profiles");val names=(0 until profiles.length()).map{when(profiles.getString(it)){"rapida"->"Rápida";"equilibrada"->"Equilibrada";else->"Menor riesgo estimado"}};card.addView(text(names.joinToString(" · "),16f,true));card.addView(text("${kotlin.math.ceil(r.getDouble("duration")/60).toInt()} min · %.1f km".format(r.getDouble("distance")/1000)));val risk=r.getJSONObject("risk");card.addView(text("Índice ilustrativo: %.2f · %d incidentes ficticios".format(risk.getDouble("value"),risk.getInt("uniqueIncidents")),11f));card.addView(button("Ver recorrido"){map.evaluateJavascript("showRoutes($routes,$i)",null)});result.addView(card)};result.addView(text("Cobertura real desconocida. Se comparan candidatos de OSRM; no se garantiza el mínimo global.",11f))}});panel.addView(result)
 }
 private fun reportScreen(){panel.addView(text("Reporte ficticio",22f,true));if(api.token==null){panel.addView(text("Inicia sesión desde Cuenta para registrar un aporte."));panel.addView(button("Iniciar sesión"){login()});return}
  panel.addView(button("Marcar ubicación del reporte"){pick="report";status.text="Toca el mapa para ubicar el ejemplo"});panel.addView(text(if(reportPoint==null)"Ubicación pendiente" else "Punto: $reportPoint",11f))
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
  web.addJavascriptInterface(object{@JavascriptInterface fun acceptToken(token:String){if(token.length !in 50..10000)return;runOnUiThread{api.token=token;request("/me"){dialog.dismiss();status.text="Cuenta conectada. Ya puedes reportar.";render()}}}},"GTVSMobile")
  web.webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(view:WebView?,request:WebResourceRequest?):Boolean{if(request?.isForMainFrame==false)return false;val uri=request?.url?:return true;return uri.scheme!="https"||uri.host!="rsebxc.github.io"||!uri.path.orEmpty().startsWith("/GTVS-2027/")}}
  dialog.setContentView(web);dialog.window?.setLayout(-1,-1);dialog.setOnDismissListener{web.removeJavascriptInterface("GTVSMobile");web.destroy()};dialog.show();dialog.window?.setLayout(-1,-1);web.loadUrl(BuildConfig.AUTH_WEB_URL)
 }
 private fun refreshContext(){request("/incidents"){i->request("/restrictions"){r->map.evaluateJavascript("showContext(${i.getJSONArray("incidents")},${r.getJSONArray("restrictions")})",null)}}}
 @Deprecated("Deprecated in Java") override fun onBackPressed(){if(authDialog?.isShowing==true){authDialog?.dismiss();return};if(tab!="rutas"){tab="rutas";render();return};super.onBackPressed()}
 override fun onDestroy(){authDialog?.dismiss();map.removeJavascriptInterface("GTVSMap");map.destroy();api.close();super.onDestroy()}
}
