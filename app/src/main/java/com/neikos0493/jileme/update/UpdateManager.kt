package com.neikos0493.jileme.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.neikos0493.jileme.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class UpdateInfo(val versionCode: Long, val versionName: String, val apkUrl: String, val releaseNotes: String="", val forceUpdate: Boolean=false)

class UpdateManager(private val context: Context) {
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val connection=URL("https://github.com/Neikos0493/bill-app/releases/latest/download/version.json").openConnection() as HttpURLConnection
        connection.connectTimeout=12000; connection.readTimeout=12000; connection.instanceFollowRedirects=true
        connection.setRequestProperty("Accept","application/json")
        connection.inputStream.bufferedReader().use { Json { ignoreUnknownKeys=true }.decodeFromString(UpdateInfo.serializer(),it.readText()) }
    }
    fun isNewer(info: UpdateInfo)=info.versionCode>context.packageManager.getPackageInfo(context.packageName,0).longVersionCode
    suspend fun download(info: UpdateInfo, onProgress: (Int)->Unit): File = withContext(Dispatchers.IO) {
        val dir=context.getExternalFilesDir("updates") ?: File(context.filesDir,"updates").apply { mkdirs() }
        val target=File(dir,"jileme-${info.versionName}.apk")
        val connection=URL(info.apkUrl).openConnection() as HttpURLConnection
        connection.connectTimeout=15000; connection.readTimeout=30000; connection.instanceFollowRedirects=true
        val total=connection.contentLengthLong
        connection.inputStream.use { input -> target.outputStream().buffered().use { output ->
            val buffer=ByteArray(64*1024); var read: Int; var count=0L; var last=-1
            while(input.read(buffer).also { read=it }!=-1) {
                output.write(buffer,0,read); count+=read
                val progress=if(total>0) (count*100/total).toInt() else 0
                if(progress!=last) { onProgress(progress); last=progress }
            }
        } }
        target
    }
    fun installIntent(file: File): Intent {
        val uri= FileProvider.getUriForFile(context,context.packageName+".files",file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    fun canInstall()=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) context.packageManager.canRequestPackageInstalls() else true
    fun permissionIntent(): Intent = if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:${context.packageName}")) else Intent()
}
