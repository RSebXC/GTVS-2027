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
 private var originLabel="";private var destinationLabel="";private var lastRoutes:JSONArray?=null
 private var pick="destination";private var tab="rutas";private var loading=false
 private var key:String?=null;private var payloadText=""
 private var authDialog:Dialog?=null
 private val navy=Color.rgb(20,47,77);private val green=Color.rgb(19,136,119)
 private fun surface(color:Int,radius:Float)=android.graphics.drawable.GradientDrawable().apply{setColor(color);cornerRadius=radius*resources.displayMetrics.density}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun column()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
 private fun text(t:String,size:Float=14f,bold:Boolean=false)=TextView(this).apply{text=t;textSize=size;setTextColor(navy);typeface=android.graphics.Typeface.create("sans-serif",if(bold)android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL);setPadding(0,dp(5),0,dp(6));if(bold)setTypeface(null,android.graphics.Typeface.BOLD)}
 private fun button(t:String,fn:()->Unit)=Button(this).apply{text=t;isAllCaps=false;typeface=android.graphics.Typeface.create("sans-serif-medium",0);textSize=13f;minimumHeight=dp(44);val primary=t=="Comparar alternativas"||t=="Enviar para revisión";setTextColor(if(primary)Color.WHITE else navy);background=surface(if(primary)green else Color.rgb(231,241,254),10f);setPadding(dp(10),dp(7),dp(10),dp(7));setOnClickListener{if(!loading)fn()}}
 private fun field(h:String)=EditText(this).apply{background=surface(Color.rgb(239,246,253),12f);minimumHeight=dp(48);hint=h;textSize=14f;typeface=android.graphics.Typeface.create("sans-serif",0);setTextColor(navy);setHintTextColor(Color.rgb(102,124,136));setPadding(dp(12),dp(7),dp(12),dp(7));setSingleLine()}
 override fun onCreate(state:Bundle?){super.onCreate(state)
  root=FrameLayout(this).apply{setBackgroundColor(Color.rgb(241,246,244))}
  root.setOnApplyWindowInsetsListener{v,i->@Suppress("DEPRECATION") v.setPadding(0,i.systemWindowInsetTop,0,i.systemWindowInsetBottom);i}
  map=WebView(this);map.settings.javaScriptEnabled=true;map.settings.allowFileAccess=false;map.settings.allowContentAccess=false;map.settings.userAgentString="GTVS/2.0 academic-demo (github.com/RSebXC/GTVS-2027)"
  map.addJavascriptInterface(MapBridge(),"GTVSMap")
  map.webViewClient=object:WebViewClient(){override fun onPageFinished(view:WebView?,url:String?){refreshContext()};override fun shouldOverrideUrlLoading(view:WebView?,request:WebResourceRequest?)=true}
  val html=assets.open("map.html").bufferedReader().use{it.readText()}
  map.loadDataWithBaseURL("https://rsebxc.github.io/GTVS-2027/",html,"text/html","UTF-8",null)
  root.addView(map,FrameLayout.LayoutParams(-1,-1))
  val header=column().apply{setPadding(dp(14),dp(8),dp(14),dp(8));background=surface(Color.rgb(249,252,255),0f)}
  val identity=LinearLayout(this).apply{gravity=android.view.Gravity.CENTER_VERTICAL}
  identity.addView(ImageView(this).apply{setImageResource(R.drawable.logo_emblema);contentDescription="Logotipo GTVS"},LinearLayout.LayoutParams(dp(38),dp(38)))
  identity.addView(text("  GTVS",23f,true),LinearLayout.LayoutParams(0,-2,1f))
  identity.addView(text("MAPA REAL\nPRUEBA ACADÉMICA",9f).apply{setTextColor(Color.rgb(42,116,194))});header.addView(identity)
  header.addView(text("Incidentes ficticios · Ninguna ruta garantiza seguridad",10f).apply{setTextColor(Color.rgb(74,107,140))})
  root.addView(header,FrameLayout.LayoutParams(-1,-2,android.view.Gravity.TOP))
  sheet=column().apply{setPadding(dp(20),0,dp(20),dp(5));background=surface(Color.rgb(250,252,255),28f);elevation=dp(10).toFloat()}
  val handle=text("━",20f).apply{gravity=android.view.Gravity.CENTER;contentDescription="Arrastrar para ajustar el panel"}
  var initialY=0f;var initialHeight=0
  handle.setOnTouchListener{_,event->when(event.actionMasked){android.view.MotionEvent.ACTION_DOWN->{initialY=event.rawY;initialHeight=sheet.height;true};android.view.MotionEvent.ACTION_MOVE->{val h=(initialHeight+initialY-event.rawY).toInt().coerceIn(dp(200),(root.height*.78f).toInt().coerceAtLeast(dp(200)));sheet.layoutParams=FrameLayout.LayoutParams(-1,h,android.view.Gravity.BOTTOM);map.evaluateJavascript("window.gtvsSheet="+h/resources.displayMetrics.density,null);true};else->true}}
  sheet.addView(handle,LinearLayout.LayoutParams(-1,dp(28)))
  status=text("Busca un destino o selecciona puntos en el mapa.",10f);sheet.addView(status)
  val scroll=ScrollView(this);panel=column();scroll.addView(panel);sheet.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  val tabs=LinearLayout(this).apply{setPadding(0,dp(7),0,0)};for((title,id) in listOf("Rutas" to "rutas","Reportar" to "reporte","Reportes" to "reportes","Cuenta" to "cuenta")){val nav=button(title){tab=id;render()}.apply{val image=when(id){"rutas"->R.drawable.ic_nav_route;"reporte"->R.drawable.ic_nav_report;"reportes"->R.drawable.ic_nav_list;else->R.drawable.ic_nav_account};setCompoundDrawablesWithIntrinsicBounds(0,image,0,0);compoundDrawablePadding=dp(3);textSize=10f};tabs.addView(nav,LinearLayout.LayoutParams(0,dp(58),1f).apply{setMargins(dp(2),0,dp(2),0)})};sheet.addView(tabs)
  val height=(resources.displayMetrics.heightPixels*.44f).toInt();root.addView(sheet,FrameLayout.LayoutParams(-1,height,android.view.Gravity.BOTTOM))
  setContentView(root);root.requestApplyInsets();render()
 }
 private fun request(path:String,body:JSONObject?=null,key:String?=null,done:(JSONObject)->Unit){loading=true;status.text="Consultando GTVS…";api.call(path,body,key){data,error->runOnUiThread{if(isFinishing||isDestroyed)return@runOnUiThread;loading=false;if(error!=null)status.text=error else if(data!=null){status.text="Contexto actualizado · datos ficticios";done(data)}}}}
 inner class MapBridge{@JavascriptInterface fun point(lon:Double,lat:Double){runOnUiThread{if(!lon.isFinite()||!lat.isFinite()||lon !in -90.69..-90.35||lat !in 14.45..14.8){status.text="Fuera del ámbito de demostración";return@runOnUiThread};val p=JSONArray().put(lon).put(lat);when(pick){"origin"->{origin=p;originLabel="Punto elegido en mapa";lastRoutes=null};"report"->reportPoint=p;else->{destination=p;destinationLabel="Punto elegido en mapa";lastRoutes=null}};map.evaluateJavascript("setPoint(${JSONObject.quote(pick)},${p})",null);status.text="${if(pick=="report")"Reporte" else if(pick=="origin")"Origen" else "Destino"}: %.5f, %.5f".format(lat,lon);if(tab=="reporte")reportCoordinate?.text="Punto seleccionado: $reportPoint" else render()}}}
 private fun render(){panel.removeAllViews();when(tab){"reporte"->reportScreen();"cuenta"->accountScreen();"reportes"->feedScreen();else->routeScreen()}}
 private fun routeScreen(){
  panel.addView(text("Planea tu recorrido",23f,true));panel.addView(text("Todas las alternativas a la vista. Tú comparas el contexto.",12f))
  val searchCard=column().apply{setPadding(dp(12),dp(9),dp(12),dp(9));background=surface(Color.WHITE,16f)}
  fun searchRow(label:String,mode:String,value:String){val row=LinearLayout(this).apply{gravity=android.view.Gravity.CENTER_VERTICAL};val badge=text(if(mode=="origin")"A" else "B",16f,true).apply{setTextColor(if(mode=="origin")Color.rgb(34,105,242) else Color.rgb(209,35,166));gravity=android.view.Gravity.CENTER};row.addView(badge,LinearLayout.LayoutParams(dp(28),-2));val q=field(label).apply{setText(value)};row.addView(q,LinearLayout.LayoutParams(0,dp(48),1f));row.addView(button("Buscar"){val query=q.text.toString().trim();if(query.length<3){status.text="Escribe al menos tres caracteres";return@button};request("/search?q="+URLEncoder.encode(query,"UTF-8")){data->val places=data.getJSONArray("places");if(places.length()==0){status.text="Sin coincidencias. Marca el punto en el mapa."}else{val labels=(0 until places.length()).map{places.getJSONObject(it).getString("name")}.toTypedArray();AlertDialog.Builder(this).setTitle(label).setItems(labels){_,i->val p=places.getJSONObject(i).getJSONArray("coordinate");if(mode=="origin"){origin=p;originLabel=labels[i]}else{destination=p;destinationLabel=labels[i]};lastRoutes=null;map.evaluateJavascript("setPoint("+JSONObject.quote(mode)+","+p+")",null);render()}.show()}}});searchCard.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})}
  searchRow("¿Desde dónde?","origin",originLabel);searchRow("¿A dónde vas?","destination",destinationLabel);panel.addView(searchCard)
  val choices=LinearLayout(this);for((label,mode)in listOf("A · En el mapa" to "origin","B · En el mapa" to "destination")){choices.addView(button(label){pick=mode;status.text="Toca el mapa para elegir "+if(mode=="origin")"el origen" else "el destino"},LinearLayout.LayoutParams(0,dp(43),1f).apply{setMargins(dp(2),dp(8),dp(2),dp(8))})};panel.addView(choices)
  val result=column()
  fun draw(routes:JSONArray){result.removeAllViews();val palette=listOf("#2878ef","#8956ec","#009bc1","#d35c16");for(i in 0 until routes.length()){val r=routes.getJSONObject(i);val profiles=r.getJSONArray("profiles");val flags=r.getJSONObject("risk").getJSONArray("restrictions").length();val lower=(0 until profiles.length()).any{profiles.getString(it)=="menor_riesgo_estimado"};val color=Color.parseColor(if(flags>0)listOf("#ef4561","#f17c18","#b74ed2","#cf533a")[i%4] else if(lower)"#138877" else palette[i%palette.size]);val card=column().apply{setPadding(dp(14),dp(10),dp(14),dp(12));background=surface(Color.WHITE,15f)};card.addView(text("RUTA "+(i+1)+if(r.has("via"))" · DESVÍO DE PRUEBA" else "",11f,true).apply{setTextColor(color)});card.addView(text(kotlin.math.ceil(r.getDouble("duration")/60).toInt().toString()+" min · "+"%.1f km".format(r.getDouble("distance")/1000),21f,true));card.addView(text(if(flags>0)"Cruza "+flags+" sectores ficticios" else "Sin cruces de los sectores ficticios activos",12f).apply{setTextColor(color)});card.addView(text("Índice ilustrativo %.2f · %d incidentes ficticios próximos".format(r.getJSONObject("risk").getDouble("value"),r.getJSONObject("risk").getInt("uniqueIncidents")),11f));card.addView(button("Resaltar ruta "+(i+1)){map.evaluateJavascript("showRoutes("+routes+","+i+",false)",null)});result.addView(card,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)})};result.addView(text("Las rutas continúan visibles. Sin cruces no significa seguridad; el modelo no está calibrado.",11f))}
  panel.addView(button("Comparar alternativas"){if(origin==null||destination==null){status.text="Elige origen y destino";return@button};request("/routes",JSONObject().put("origin",origin).put("destination",destination)){data->val routes=data.getJSONArray("routes");lastRoutes=routes;var chosen=0;for(i in 0 until routes.length())if(routes.getJSONObject(i).getJSONArray("profiles").toString().contains("menor_riesgo_estimado"))chosen=i;map.evaluateJavascript("showRoutes("+routes+","+chosen+",true)",null);draw(routes)}})
  panel.addView(button("Ensayar un desvío ficticio"){origin=JSONArray().put(-90.541).put(14.636);destination=JSONArray().put(-90.516).put(14.636);originLabel="Origen del escenario";destinationLabel="Destino del escenario";lastRoutes=null;map.evaluateJavascript("setPoint('origin',"+origin+");setPoint('destination',"+destination+")",null);render()});panel.addView(result);lastRoutes?.let{draw(it)}
 }
 private fun reportScreen(){panel.addView(text("Reporte ficticio",22f,true));if(api.token==null){panel.addView(text("Inicia sesión desde Cuenta para registrar un aporte."));panel.addView(button("Iniciar sesión"){login()});return}
  panel.addView(button("Marcar ubicación del reporte"){pick="report";status.text="Toca el mapa para ubicar el ejemplo"});reportCoordinate=text(if(reportPoint==null)"Ubicación pendiente" else "Punto: $reportPoint",11f);panel.addView(reportCoordinate)
  val type=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Robo o ataque ficticio","Obstrucción ficticia","Cierre ficticio","Otro contexto ficticio"))};panel.addView(text("Tipo de simulación",12f,true));val category=column();val buttons=mutableListOf<Button>();val tones=listOf("#e83b5d","#ef761c","#7954e9","#2583ed");val labels=listOf("Robo / ataque","Obstrucción","Cierre","Otro contexto");for(rowIndex in 0..1){val row=LinearLayout(this);for(col in 0..1){val index=rowIndex*2+col;val choice=button(labels[index]){type.setSelection(index);for(j in buttons.indices){buttons[j].background=surface(if(j==index)Color.parseColor(tones[j]) else Color.rgb(234,242,252),12f);buttons[j].setTextColor(if(j==index)Color.WHITE else Color.parseColor(tones[j]))}};buttons.add(choice);choice.setTextColor(if(index==0)Color.WHITE else Color.parseColor(tones[index]));choice.background=surface(if(index==0)Color.parseColor(tones[index]) else Color.rgb(234,242,252),12f);row.addView(choice,LinearLayout.LayoutParams(0,dp(54),1f).apply{setMargins(dp(3),dp(4),dp(3),dp(4))})};category.addView(row)};panel.addView(category)
  val severity=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,(1..5).map{"Gravedad demostrativa $it"});setSelection(2)};panel.addView(severity)
  var occurredAt:String?=null;val date=button("Fecha del ejemplo · opcional"){};date.setOnClickListener{val now=java.time.LocalDateTime.now();android.app.DatePickerDialog(this,{_,year,month,day->android.app.TimePickerDialog(this,{_,hour,minute->val selected=java.time.LocalDateTime.of(year,month+1,day,hour,minute);occurredAt=selected.atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime().toString();date.text=selected.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))},now.hour,now.minute,true).show()},now.year,now.monthValue-1,now.dayOfMonth).show()};panel.addView(date)
  val description=field("Describe el ejemplo sin datos personales").apply{setSingleLine(false);minLines=3;filters=arrayOf(android.text.InputFilter.LengthFilter(1500))};panel.addView(description)
  val confirm=CheckBox(this).apply{text="Confirmo que es una simulación ficticia"};panel.addView(confirm)
  panel.addView(button("Enviar para revisión"){if(reportPoint==null||!confirm.isChecked){status.text="Marca una ubicación y confirma la condición ficticia";return@button};val kinds=listOf("robo_ataque","obstruccion","cierre","otro");val body=JSONObject().put("type",kinds[type.selectedItemPosition]).put("coordinate",reportPoint).put("description",description.text.toString()).put("severity",severity.selectedItemPosition+1).put("occurredAt",occurredAt?:JSONObject.NULL).put("synthetic",true);val serial=body.toString();if(serial!=payloadText){payloadText=serial;key=UUID.randomUUID().toString()};request("/reports",body,key){data->status.text="Reporte ${data.getJSONObject("report").getString("id").take(8)} guardado como pendiente";key=null;payloadText="";description.setText("")}})
 }
 private fun feedScreen(){
  panel.addView(text("Panel de reportes",22f,true));panel.addView(text("Incidentes ficticios activos. Los escenarios precargados no describen hechos reales.",12f))
  val filter=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Todos los tipos","Robo o ataque","Obstrucción","Cierre","Otro contexto"))};panel.addView(filter)
  val list=column();panel.addView(list)
  request("/incidents"){data->val items=data.getJSONArray("incidents");fun draw(){list.removeAllViews();val types=listOf("","robo_ataque","obstruccion","cierre","otro");val chosen=types[filter.selectedItemPosition];val visible=(0 until items.length()).map{items.getJSONObject(it)}.filter{chosen.isEmpty()||it.getString("type")==chosen};list.addView(text("${visible.size} registros visibles · solo simulaciones",11f));for(item in visible){val card=column().apply{setPadding(dp(12),dp(12),dp(12),dp(12));background=surface(Color.WHITE,12f)};card.addView(text(item.getString("title"),14f,true));card.addView(text(if(item.isNull("demo_batch"))"INCIDENTE FICTICIO" else "ESCENARIO PRECARGADO · FICTICIO",10f));card.addView(button("Ver en el mapa"){map.evaluateJavascript("map.setView(["+item.getDouble("latitude")+","+item.getDouble("longitude")+"],16)",null)});list.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})}};filter.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onNothingSelected(parent:AdapterView<*>?){ };override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){draw()}};draw()}
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
