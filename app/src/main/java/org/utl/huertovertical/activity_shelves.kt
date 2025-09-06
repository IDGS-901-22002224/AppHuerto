package org.utl.huertovertical

import android.os.Bundle
import android.util.Log
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

class activity_shelves : AppCompatActivity() {

    private lateinit var mqttClient: 
    private val mqttServer = ""
    private val mqttUser = ""
    private val mqttPass = ""
    private val substrateHumidityTopic = "huerto/substrate_humidity"
    private val lightLevelTopic = "huerto/light_level"

    private var humidityLevel = 68f
    private var lightLevel = 850f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shelves)

        val humidityTextShelf1 = findViewById<TextView>(R.id.humidityTextShelf1)
        val lightTextShelf1 = findViewById<TextView>(R.id.lightTextShelf1)
        val humidityTextShelf2 = findViewById<TextView>(R.id.humidityTextShelf2)
        val lightTextShelf2 = findViewById<TextView>(R.id.lightTextShelf2)
        val humidityTextShelf3 = findViewById<TextView>(R.id.humidityTextShelf3)
        val lightTextShelf3 = findViewById<TextView>(R.id.lightTextShelf3)

        // Actualizar valores iniciales
        humidityTextShelf1.text = "${humidityLevel.toInt()}%"
        lightTextShelf1.text = "${lightLevel.toInt()} lux"
        humidityTextShelf2.text = "65%"
        lightTextShelf2.text = "780 lux"
        humidityTextShelf3.text = "71%"
        lightTextShelf3.text = "810 lux"

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
                        Log.d("MQTT", "Conectado a MQTT en Shelves")
                        subscribeToTopics()
                    } else {
                        Log.e("MQTT", "Error de conexión: ${throwable.message}")
                    }
                }
            }
    }

    private fun subscribeToTopics() {
        mqttClient.subscribeWith()
            ?.topicFilter(substrateHumidityTopic)
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        humidityLevel = payload.toFloat()
                        val humidityTextShelf1 = findViewById<TextView>(R.id.humidityTextShelf1)
                        val humidityTextShelf2 = findViewById<TextView>(R.id.humidityTextShelf2)
                        val humidityTextShelf3 = findViewById<TextView>(R.id.humidityTextShelf3)
                        humidityTextShelf1.text = "${humidityLevel.toInt()}%"
                        humidityTextShelf2.text = "${humidityLevel.toInt()}%"
                        humidityTextShelf3.text = "${humidityLevel.toInt()}%"
                        Log.d("Humidity", "Humedad actualizada a $humidityLevel%")
                    } catch (e: NumberFormatException) {
                        Log.e("Humidity", "Error al convertir payload: $payload")
                    }
                }
            }
            ?.send()

        mqttClient.subscribeWith()
            ?.topicFilter(lightLevelTopic)
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        lightLevel = payload.toFloat()
                        val lightTextShelf1 = findViewById<TextView>(R.id.lightTextShelf1)
                        val lightTextShelf2 = findViewById<TextView>(R.id.lightTextShelf2)
                        val lightTextShelf3 = findViewById<TextView>(R.id.lightTextShelf3)
                        lightTextShelf1.text = "${lightLevel.toInt()} lux"
                        lightTextShelf2.text = "${lightLevel.toInt()} lux"
                        lightTextShelf3.text = "${lightLevel.toInt()} lux"
                        Log.d("LightLevel", "Luz actualizada a $lightLevel%")
                    } catch (e: NumberFormatException) {
                        Log.e("LightLevel", "Error al convertir payload: $payload")
                    }
                }
            }
            ?.send()
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient.disconnect()
    }
}
