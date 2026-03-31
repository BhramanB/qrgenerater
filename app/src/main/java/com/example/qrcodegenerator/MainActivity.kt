package com.example.qrcodegenerator

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerType: Spinner
    private lateinit var etUrl: EditText
    private lateinit var etText: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etWifiSsid: EditText
    private lateinit var etWifiPassword: EditText
    private lateinit var spinnerWifiSecurity: Spinner
    private lateinit var etContactName: EditText
    private lateinit var etContactPhone: EditText
    private lateinit var etContactEmail: EditText
    private lateinit var btnGenerate: Button
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button
    private lateinit var ivQrCode: ImageView
    private lateinit var tvPlaceholder: TextView
    private lateinit var tvQrSize: TextView
    private lateinit var seekBarSize: SeekBar
    private lateinit var spinnerFgColor: Spinner
    private lateinit var spinnerBgColor: Spinner

    private var generatedBitmap: Bitmap? = null

    private val qrTypes = arrayOf(
        "URL / Link", "Plain Text", "Email",
        "Phone Number", "WiFi Network", "Contact (vCard)"
    )

    private val colorNames = arrayOf("Black", "Dark Blue", "Dark Red", "Dark Green", "Purple", "Dark Orange")
    private val colorValues = arrayOf(
        Color.BLACK,
        Color.parseColor("#1A237E"),
        Color.parseColor("#B71C1C"),
        Color.parseColor("#1B5E20"),
        Color.parseColor("#4A148C"),
        Color.parseColor("#E65100")
    )

    private val bgColorNames = arrayOf("White", "Light Yellow", "Light Blue", "Light Green", "Light Pink")
    private val bgColorValues = arrayOf(
        Color.WHITE,
        Color.parseColor("#FFFDE7"),
        Color.parseColor("#E3F2FD"),
        Color.parseColor("#E8F5E9"),
        Color.parseColor("#FCE4EC")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        setupSpinners()
        setupListeners()
    }

    private fun initViews() {
        spinnerType        = findViewById(R.id.spinner_type)
        etUrl              = findViewById(R.id.et_url)
        etText             = findViewById(R.id.et_text)
        etEmail            = findViewById(R.id.et_email)
        etPhone            = findViewById(R.id.et_phone)
        etWifiSsid         = findViewById(R.id.et_wifi_ssid)
        etWifiPassword     = findViewById(R.id.et_wifi_password)
        spinnerWifiSecurity= findViewById(R.id.spinner_wifi_security)
        etContactName      = findViewById(R.id.et_contact_name)
        etContactPhone     = findViewById(R.id.et_contact_phone)
        etContactEmail     = findViewById(R.id.et_contact_email)
        btnGenerate        = findViewById(R.id.btn_generate)
        btnSave            = findViewById(R.id.btn_save)
        btnShare           = findViewById(R.id.btn_share)
        ivQrCode           = findViewById(R.id.iv_qr_code)
        tvPlaceholder      = findViewById(R.id.tv_placeholder)
        tvQrSize           = findViewById(R.id.tv_qr_size)
        seekBarSize        = findViewById(R.id.seekbar_size)
        spinnerFgColor     = findViewById(R.id.spinner_fg_color)
        spinnerBgColor     = findViewById(R.id.spinner_bg_color)
    }

    private fun createBlackTextAdapter(items: Array<String>): ArrayAdapter<String> {
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as? TextView)?.let {
                    it.setTextColor(Color.BLACK)
                    it.setBackgroundColor(Color.WHITE)
                }
                return v
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as? TextView)?.let {
                    it.setTextColor(Color.BLACK)
                    it.setBackgroundColor(Color.WHITE)
                }
                return v
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    private fun setupSpinners() {
        spinnerType.adapter = createBlackTextAdapter(qrTypes)

        spinnerWifiSecurity.adapter = createBlackTextAdapter(arrayOf("WPA/WPA2", "WEP", "None"))

        spinnerFgColor.adapter = createBlackTextAdapter(colorNames)

        spinnerBgColor.adapter = createBlackTextAdapter(bgColorNames)

        seekBarSize.max = 4
        seekBarSize.progress = 2
        updateSizeLabel(2)
    }

    private fun setupListeners() {
        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                updateInputFields(pos)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSizeLabel(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnGenerate.setOnClickListener { generateQRCode() }
        btnSave.setOnClickListener    { saveQRCode() }
        btnShare.setOnClickListener   { shareQRCode() }
    }

    private fun updateSizeLabel(progress: Int) {
        val sizes = arrayOf("256×256", "384×384", "512×512", "768×768", "1024×1024")
        tvQrSize.text = "Size: ${sizes[progress]}"
    }

    private fun getQRSize(): Int {
        return intArrayOf(256, 384, 512, 768, 1024)[seekBarSize.progress]
    }

    private fun updateInputFields(position: Int) {
        listOf(etUrl, etText, etEmail, etPhone, etWifiSsid,
            etWifiPassword, spinnerWifiSecurity,
            etContactName, etContactPhone, etContactEmail
        ).forEach { it.visibility = View.GONE }

        when (position) {
            0 -> etUrl.visibility = View.VISIBLE
            1 -> etText.visibility = View.VISIBLE
            2 -> etEmail.visibility = View.VISIBLE
            3 -> etPhone.visibility = View.VISIBLE
            4 -> {
                etWifiSsid.visibility = View.VISIBLE
                etWifiPassword.visibility = View.VISIBLE
                spinnerWifiSecurity.visibility = View.VISIBLE
            }
            5 -> {
                etContactName.visibility = View.VISIBLE
                etContactPhone.visibility = View.VISIBLE
                etContactEmail.visibility = View.VISIBLE
            }
        }
    }

    private fun buildQRContent(): String? {
        return when (spinnerType.selectedItemPosition) {
            0 -> {
                val url = etUrl.text.toString().trim()
                if (url.isEmpty()) { showError("Please enter a URL"); return null }
                if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            }
            1 -> {
                val text = etText.text.toString().trim()
                if (text.isEmpty()) { showError("Please enter some text"); return null }
                text
            }
            2 -> {
                val email = etEmail.text.toString().trim()
                if (email.isEmpty()) { showError("Please enter an email address"); return null }
                "mailto:$email"
            }
            3 -> {
                val phone = etPhone.text.toString().trim()
                if (phone.isEmpty()) { showError("Please enter a phone number"); return null }
                "tel:$phone"
            }
            4 -> {
                val ssid = etWifiSsid.text.toString().trim()
                if (ssid.isEmpty()) { showError("Please enter the WiFi SSID"); return null }
                val password = etWifiPassword.text.toString()
                val security = arrayOf("WPA", "WEP", "nopass")[spinnerWifiSecurity.selectedItemPosition]
                "WIFI:T:$security;S:$ssid;P:$password;;"
            }
            5 -> {
                val name = etContactName.text.toString().trim()
                if (name.isEmpty()) { showError("Please enter a contact name"); return null }
                val phone = etContactPhone.text.toString().trim()
                val email = etContactEmail.text.toString().trim()
                "BEGIN:VCARD\nVERSION:3.0\nFN:$name\nTEL:$phone\nEMAIL:$email\nEND:VCARD"
            }
            else -> null
        }
    }

    private fun generateQRCode() {
        val content = buildQRContent() ?: return
        val size    = getQRSize()
        val fgColor = colorValues[spinnerFgColor.selectedItemPosition]
        val bgColor = bgColorValues[spinnerBgColor.selectedItemPosition]

        try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN          to 2,
                EncodeHintType.CHARACTER_SET   to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) fgColor else bgColor)
                }
            }
            generatedBitmap = bitmap
            ivQrCode.setImageBitmap(bitmap)
            ivQrCode.visibility   = View.VISIBLE
            tvPlaceholder.visibility = View.GONE
            btnSave.isEnabled  = true
            btnShare.isEnabled = true
            Toast.makeText(this, "✅ QR Code generated!", Toast.LENGTH_SHORT).show()
        } catch (e: WriterException) {
            showError("Failed to generate: ${e.message}")
        }
    }

    private fun saveQRCode() {
        val bitmap = generatedBitmap ?: return
        try {
            val filename = "QRCode_${System.currentTimeMillis()}.png"
            val cv = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRCodes")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cv.clear(); cv.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(it, cv, null, null)
                }
                Toast.makeText(this, "✅ Saved to Pictures/QRCodes", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) { showError("Save failed: ${e.message}") }
    }

    private fun shareQRCode() {
        val bitmap = generatedBitmap ?: return
        try {
            val filename = "QRCode_${System.currentTimeMillis()}.png"
            val cv = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cv.clear(); cv.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(it, cv, null, null)
                }
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, it)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Share QR Code"))
            }
        } catch (e: Exception) { showError("Share failed: ${e.message}") }
    }

    private fun showError(msg: String) =
        Toast.makeText(this, "⚠️ $msg", Toast.LENGTH_SHORT).show()
}
