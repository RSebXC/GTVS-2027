package gt.edu.usac.gtvs
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ApiClient {
 private val executor=Executors.newSingleThreadExecutor()
 var token:String?=null
 fun call(path:String,body:JSONObject?=null,key:String?=null,done:(JSONObject?,String?)->Unit){executor.execute{
  var connection:HttpURLConnection?=null
  try{
   val conn=URL(BuildConfig.API_URL+path).openConnection() as HttpURLConnection;connection=conn
   conn.connectTimeout=15000;conn.readTimeout=30000
   conn.setRequestProperty("Accept","application/json")
   if(path=="/me"||path=="/reports"||path.startsWith("/admin/"))token?.let{conn.setRequestProperty("Authorization","Bearer $it")}
   if(body!=null){conn.requestMethod="POST";conn.doOutput=true;conn.setRequestProperty("Content-Type","application/json");key?.let{conn.setRequestProperty("Idempotency-Key",it)};conn.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}}
   val code=conn.responseCode
   val stream=if(code in 200..299)conn.inputStream else conn.errorStream
   val json=JSONObject(stream?.bufferedReader(Charsets.UTF_8)?.use{it.readText()}?:"{}")
   if(code !in 200..299)throw IllegalStateException(json.optString("error","No se completó la solicitud"))
   done(json,null)
  }catch(e:Exception){done(null,e.message?:"Servicio no disponible")}finally{connection?.disconnect()}
 }}
 fun close(){executor.shutdownNow()}
}
