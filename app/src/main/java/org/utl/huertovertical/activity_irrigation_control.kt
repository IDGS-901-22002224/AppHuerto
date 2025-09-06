package org.utl.huertovertical

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.*

class activity_irrigation_control : AppCompatActivity() {

    private lateinit var mqttClient: 
    private val mqttServer = ""
    private val mqttUser = ""
    private val mqttPass = ""
    private val waterPumpTopic = "control-led"
    private val waterLevelTopic = "huerto/water_level"
    private val substrateHumidityTopic = "huerto/substrate_humidity"

    private var waterLevel = 75f
    private var humidityLevel = 72f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_irrigation_control)

        val btnStartManualIrrigation = findViewById<Button>(R.id.btnStartManualIrrigation)
        val waterLevelPercentage = findViewById<TextView>(R.id.waterLevelPercentage)
        val humidityText = findViewById<TextView>(R.id.humidityText)
        val waterFillLevel = findViewById<View>(R.id.waterFillLevel)

        // Actualizar valores iniciales
        updateWaterLevelDisplay(waterLevel, waterLevelPercentage, waterFillLevel)
        humidityText.text = "${humidityLevel.toInt()}% OK"

        // Configurar boton para encender la bomba
        btnStartManualIrrigation.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                sendMqttMessage(waterPumpTopic, "1")
                Log.d("IrrigationControl", "Bomba de agua encendida")
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        CoroutineScope(Dispatchers.IO).launch {
            initializeMqttClient()
        }
    }

    private fun initializeMqttClient() {
        mqttClient = MqttClient.builder()
            .useMqttVersion5()
            .serverHost(mqttServer)
            .serverPort(8883)
            .sslWithDefaultConfig()
            .identifier(UUID.randomUUID().toString())
            .buildAsync()

        mqttClient.connectWith()
            ?.simpleAuth()
            ?.username(mqttUser)
            ?.password(mqttPass.toByteArray(StandardCharsets.UTF_8))
            ?.applySimpleAuth()
            ?.send()
            ?.whenComplete { connAck, throwable ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (throwable == null) {
                        Log.d("MQTT", "Conectado a MQTT en Irrigation Control")
                        subscribeToTopics()
                    } else {
                        Log.e("MQTT", "Error de conexión: ${throwable.message}")
                    }
                }
            }
    }

    private fun subscribeToTopics() {
        mqttClient.subscribeWith()
            ?.topicFilter(waterLevelTopic)
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        waterLevel = payload.toFloat()
                        val waterLevelPercentage = findViewById<TextView>(R.id.waterLevelPercentage)
                        val waterFillLevel = findViewById<View>(R.id.waterFillLevel)
                        updateWaterLevelDisplay(waterLevel, waterLevelPercentage, waterFillLevel)
                        Log.d("WaterLevel", "Nivel de agua actualizado a $waterLevel%")
                    } catch (e: NumberFormatException) {
                        Log.e("WaterLevel", "Error al convertir payload: $payload")
                    }
                }
            }
            ?.send()

        mqttClient.subscribeWith()
            ?.topicFilter(substrateHumidityTopic)
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        humidityLevel = payload.toFloat()
                        findViewById<TextView>(R.id.humidityText).text = "${humidityLevel.toInt()}% OK"
                        Log.d("Humidity", "Humedad actualizada a $humidityLevel%")
                    } catch (e: NumberFormatException) {
                        Log.e("Humidity", "Error al convertir payload: $payload")
                    }
                }
            }
            ?.send()
    }

    private fun updateWaterLevelDisplay(level: Float, textView: TextView, fillView: View) {
        val percentage = level.toInt().coerceIn(0, 100)
        textView.text = "$percentage%"
        val waterLevelPercentageHeader = findViewById<TextView>(R.id.waterLevelPercentageHeader)
        waterLevelPercentageHeader.text = "$percentage%"
        val layoutParams = fillView.layoutParams as ViewGroup.LayoutParams
        layoutParams.height = (100 * percentage / 100).toInt()
        fillView.layoutParams = layoutParams
    }

    private fun sendMqttMessage(topic: String, message: String) {
        mqttClient.publishWith()
            ?.topic(topic)
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.payload(message.toByteArray(StandardCharsets.UTF_8))
            ?.send()
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient.disconnect()
    }
}
