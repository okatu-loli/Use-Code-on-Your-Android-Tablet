package com.dev.a8080

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CustomWebView(context: Context) : WebView(context) {
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        requestLayout()
    }
}

class TipTimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}

class MainActivity : AppCompatActivity() {
    private var originalPaddingBottom = 0
    private lateinit var myWebView: WebView
    private lateinit var progressBar: View
    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private var lastLoadedUrl: String = "http://127.0.0.1:8080"
    private var isPageLoadedSuccessfully = false

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 1
        private const val REQUIRED_PERMISSIONS_REQUEST_CODE = 2

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    @SuppressLint("UseCompatLoadingForDrawables", "ResourceAsColor")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutoutMode = window.attributes.layoutInDisplayCutoutMode
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                window.attributes.layoutInDisplayCutoutMode = cutoutMode
            }
        }
        window.decorView.apply {
            systemUiVisibility =
                if ((context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {
                    window.setBackgroundDrawableResource(R.drawable.white)
                    window.navigationBarColor = getColor(R.color.white)
                    window.statusBarColor = getColor(R.color.white)
                    setBackgroundColor(getColor(R.color.white))
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                } else {
                    window.setBackgroundDrawableResource(R.drawable.black)
                    window.navigationBarColor = getColor(R.color.black)
                    window.statusBarColor = getColor(R.color.black)
                    setBackgroundColor(getColor(R.color.black))
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("SetJavaScriptEnabled", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.apply {
            systemUiVisibility =
                if ((context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {
                    window.setBackgroundDrawableResource(R.drawable.white)
                    window.navigationBarColor = getColor(R.color.white)
                    window.statusBarColor = getColor(R.color.white)
                    setBackgroundColor(getColor(R.color.white))
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                } else {
                    window.setBackgroundDrawableResource(R.drawable.black)
                    window.navigationBarColor = getColor(R.color.black)
                    window.statusBarColor = getColor(R.color.black)
                    setBackgroundColor(getColor(R.color.black))
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                }
        }

        @RequiresApi(Build.VERSION_CODES.R) if (!Environment.isExternalStorageManager()) {
            val builder = MaterialAlertDialogBuilder(
                this
            )
            builder.setTitle("请求所有文件访问权限").setIcon(R.drawable.database_2_line)
                .setMessage("Android WebView 可能调用文件，是否给予所有文件访问权限？")
                .setPositiveButton("前往权限设置") { _, _ ->
                    var intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:" + this.packageName)
                    val rmes = 100
                    startActivityForResult(intent, rmes)
                }.setNeutralButton("拒绝") { _, _ -> }
            val dialog = builder.create()
            dialog.show()

        }
        //隐藏导航栏
        window.decorView.apply {
            systemUiVisibility =
                if ((context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                } else {
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                }
        }

        //强制横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        myWebView = findViewById(R.id.webview)
        var rootLayout: ViewGroup? = findViewById(R.id.rootLayout)
        progressBar = findViewById(R.id.progressBar)


        //权限申请
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            val requestCode = 100
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestCode
            )
        }


        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            val requestCode = 100
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), requestCode
            )
        }
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_DENIED
        ) {
            val requestCode = 100
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), requestCode
            )
        }
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_DENIED
        ) {
            val requestCode = 100
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), requestCode
                )
            }
        }
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_DENIED
        ) {
            val requestCode = 100
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_MEDIA_VIDEO), requestCode
                )
            }
        }
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_DENIED
        ) {
            val requestCode = 100
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), requestCode
                )
            }
        }

        //WebView的设置
        //myWebView.clearCache(true)
        myWebView.webViewClient = WebViewClient()
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true
        myWebView.settings.safeBrowsingEnabled = false
        myWebView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        myWebView.settings.databaseEnabled = true
        myWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        myWebView.settings.pluginState = WebSettings.PluginState.ON
        myWebView.settings.allowFileAccess = true
        myWebView.setBackgroundColor(0)
        myWebView.background.alpha = 0
        myWebView.settings.loadWithOverviewMode = true
        myWebView.settings.javaScriptCanOpenWindowsAutomatically = true
        myWebView.settings.allowUniversalAccessFromFileURLs = true
        myWebView.settings.loadsImagesAutomatically = true
        myWebView.settings.defaultTextEncodingName = "utf-8"
        myWebView.settings.allowContentAccess = true
        myWebView.settings.forceDark = WebSettings.FORCE_DARK_AUTO
        originalPaddingBottom = myWebView.paddingBottom

        myWebView.setDownloadListener { url, _, _, _, _ ->
            val builder = MaterialAlertDialogBuilder(
                this
            )
            builder.setTitle("下载文件？").setIcon(R.drawable.download_line).setMessage(url)
                .setPositiveButton("下载") { _, _ ->
                    //系统下载
                    val request = DownloadManager.Request(Uri.parse(url))
                    val downloadManager =
                        getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    downloadManager.enqueue(request)
                }.setNeutralButton("拒绝") { _, _ -> }
            val dialog = builder.create()
            dialog.show()
        }



        myWebView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java", ReplaceWith("false"))
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            @SuppressLint("QueryPermissionsNeeded")
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()

                val uri = Uri.parse(url)

                val lastLoadedUri = Uri.parse(lastLoadedUrl)
                val lastLoadedHost = lastLoadedUri.host

                //检测是否127.0.0.1 和用户输入的url
                val host = uri.host
                if (host != null && (host == "127.0.0.1" || host.endsWith(".127.0.0.1") || host == "localhost" || host == lastLoadedHost)) {
                    view.loadUrl(url)
                } else {
                    val host = Uri.parse(lastLoadedUrl).host
//                    Log.d("WebViewDebug", "Host: $host")
                    val builder = MaterialAlertDialogBuilder(
                        this@MainActivity,
                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
                    )
                    builder.setTitle("打开外链？").setMessage(url)
                        .setIcon(R.drawable.share_circle_line).setPositiveButton("打开") { _, _ ->
                            val customTabsIntent: CustomTabsIntent =
                                CustomTabsIntent.Builder().build()
                            customTabsIntent.launchUrl(this@MainActivity, Uri.parse(url))
                        }.setNeutralButton("拒绝") { _, _ -> }
                    val dialog = builder.create()
                    dialog.show()
                }
                return true
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                isPageLoadedSuccessfully = false
                showUrlInputDialog()
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?, handler: SslErrorHandler?, error: SslError?
            ) {
                handler?.proceed()//信任所有SSL证书
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                myWebView.requestLayout()
                isPageLoadedSuccessfully = true
            }
        }
        myWebView.settings.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; arm64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36"


        //适配状态栏、异形屏和软键盘高度
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout!!) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            rootLayout.setPadding(0, statusBarHeight, 0, 0)
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val cutoutLeft = rootLayout.rootWindowInsets.displayCutout?.safeInsetLeft ?: 0
            val cutoutRight = rootLayout.rootWindowInsets.displayCutout?.safeInsetRight ?: 0

            if (imeVisible) {
                val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                rootLayout.setPadding(cutoutLeft, statusBarHeight, cutoutRight, imeHeight)
            } else {
                rootLayout.setPadding(cutoutLeft, statusBarHeight, cutoutRight, 0)
            }
            WindowInsetsCompat.Builder(insets).build()
        }
        myWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                    progressBar.bringToFront()
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }


            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                uploadMessage = filePathCallback!!
                openFileChooser()
                return true
            }

        }

        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        lastLoadedUrl = prefs.getString("LastURL", "http://127.0.0.1:8080") ?: "http://127.0.0.1:8080"

        myWebView.loadUrl(lastLoadedUrl)

        //软键盘动画
        ViewCompat.setWindowInsetsAnimationCallback(
            rootLayout,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                var startBottom = 0f

                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    startBottom = rootLayout.bottom.toFloat()
                }

                var endBottom = 0f

                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat
                ): WindowInsetsAnimationCompat.BoundsCompat {
                    endBottom = rootLayout.bottom.toFloat()

                    return bounds
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeAnimation = runningAnimations.find {
                        it.typeMask and WindowInsetsCompat.Type.ime() != 0
                    } ?: return insets

                    myWebView.translationY =
                        (startBottom - endBottom) * (1 - imeAnimation.interpolatedFraction)

                    return insets
                }
            })
        // 显示URL输入弹窗
        // 延迟执行，以检查页面是否加载成功
        myWebView.postDelayed({
            if (!isPageLoadedSuccessfully) {
                showUrlInputDialog()
            }
        }, 4000)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }


    // 打开文件选择器
    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*" // 可根据需要进行文件类型的限制
        }
        startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
    }

    // 处理文件选择结果
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                data?.let { intentData ->
                    uploadMessage?.onReceiveValue(
                        WebChromeClient.FileChooserParams.parseResult(
                            resultCode, intentData
                        )
                    )
                }
            } else {
                uploadMessage?.onReceiveValue(null)
            }
            uploadMessage = null
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    // 检查是否具有所需权限
    private fun hasRequiredPermissions(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    // 请求必要的权限
    private fun requestRequiredPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS_REQUEST_CODE
        )
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == REQUIRED_PERMISSIONS_REQUEST_CODE) {
            var allPermissionsGranted = true
            grantResults.forEach { result ->
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                }
            }
            if (allPermissionsGranted) {
                // 权限已授予
                // 打开文件选择器
                openFileChooser()
            } else {
                // 权限被拒绝
                // 处理权限被拒绝的情况
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun showUrlInputDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            hint = "输入url"
            // 设置内边距
            val paddingInPixels = dpToPx(32)
            setPadding(paddingInPixels, paddingInPixels, paddingInPixels, paddingInPixels)
        }
        builder.setView(input)
        builder.setPositiveButton("加载") { dialog, _ ->
            var url = input.text.toString().trim()
            // 检查URL是否包含协议部分，如果没有，则默认添加"http://"
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
//            Log.d("WebViewDebug", "URL: $url")
            if (url.isNotEmpty()) {
                lastLoadedUrl = url // 更新lastLoadedUrl的值
                myWebView.loadUrl(url)
                // 将新的URL保存到SharedPreferences
                val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                prefs.edit().putString("LastURL", url).apply()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("取消") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }


    // 辅助函数，将dp单位转换为像素单位
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }
}


