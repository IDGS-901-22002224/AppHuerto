package org.utl.huertovertical

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import org.utl.huertovertical.databinding.ActivityDashboardBinding
import java.nio.charset.StandardCharsets
import java.util.UUID

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private var mqttClient: = null
    private val mqttServer = ""
    private val mqttUser = ""
    private val mqttPass = ""
    private val mqttHumidityTopic = "huerto/humidity"
    private val mqttTemperatureTopic = "huerto/temperature"
    private val mqttWaterLevelTopic = "huerto/water_level"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CoroutineScope(Dispatchers.IO).launch {
            initializeMqttClient()
        }

        setupButtonClickListeners()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeMqttClient() {
        mqttClient = MqttClient.builder()
            .useMqttVersion5()
            .serverHost(mqttServer.removePrefix("ssl://").split(":")[0])
            .serverPort(8883)
            .sslWithDefaultConfig()
            .identifier(UUID.randomUUID().toString())
            .buildAsync()

        mqttClient?.connectWith()
            ?.simpleAuth()
            ?.username(mqttUser)
            ?.password(mqttPass.toByteArray(StandardCharsets.UTF_8))
            ?.applySimpleAuth()
            ?.send()
            ?.whenComplete { connAck, throwable ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (throwable == null) {
                        Toast.makeText(this@DashboardActivity, "Conectado a MQTT", Toast.LENGTH_SHORT).show()
                        subscribeToTopics()
                    } else {
                        Toast.makeText(this@DashboardActivity, "Error de conexión: ${throwable.message}", Toast.LENGTH_SHORT).show()
                        Log.e("MQTT", "Conexión fallida: ${throwable.message}")
                    }
                }
            }
    }

    private fun subscribeToTopics() {
        // Topic a huerto/humidity
        mqttClient?.subscribeWith()
            ?.topicFilter(mqttHumidityTopic)
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                CoroutineScope(Dispatchers.Main).launch {
                    val topic = publish.topic.toString()
                    val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                    try {
                        val value = payload.toFloat()
                        if (topic == mqttHumidityTopic) {
                            binding.tvHumedadPorcentaje.text = "$value%"
                            updateHumidityStatus(value)
                        }
                    } catch (e: NumberFormatException) {
                        Log.e("MQTT", "Error al convertir payload para $topic: $payload")
                    }
                }
            }
            ?.send()
            ?.whenComplete { subAck, throwable ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (throwable == null) {
                        Log.d("MQTT", "Suscripción a $mqttHumidityTopic exitosa")
                    } else {
                        Log.e("MQTT", "Error al suscribirse a $mqttHumidityTopic: ${throwable.message}")
                    }
                }
            }

        // Topic a huerto/temperature
        mqttClient?.subscribeWith()
            ?.topicFilter(mqttTemperatureTopic)
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                CoroutineScope(Dispatchers.Main).launch {
                    val topic = publish.topic.toString()
                    val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                    try {
                        val value = payload.toFloat()
                        if (topic == mqttTemperatureTopic) {
                            binding.tvTemperatura.text = "$value°C"
                            updateTemperatureStatus(value)
                        }
                    } catch (e: NumberFormatException) {
                        Log.e("MQTT", "Error al convertir payload para $topic: $payload")
                    }
                }
            }
            ?.send()
            ?.whenComplete { subAck, throwable ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (throwable == null) {
                        Log.d("MQTT", "Suscripción a $mqttTemperatureTopic exitosa")
                    } else {
                        Log.e("MQTT", "Error al suscribirse a $mqttTemperatureTopic: ${throwable.message}")
                    }
                }
            }

        // Topic a huerto/water_level
        mqttClient?.subscribeWith()
            ?.topicFilter(mqttWaterLevelTopic)
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                CoroutineScope(Dispatchers.Main).launch {
                    val topic = publish.topic.toString()
                    val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                    try {
                        val value = payload.toFloat()
                        if (topic == mqttWaterLevelTopic) {
                            binding.tvAguaPorcentaje.text = "$value%"
                            updateWaterLevelStatus(value)
                        }
                    } catch (e: NumberFormatException) {
                        Log.e("MQTT", "Error al convertir payload para $topic: $payload")
                    }
                }
            }
            ?.send()
            ?.whenComplete { subAck, throwable ->
                CoroutineScope(Dispatchers.Main).launch {
                    if (throwable == null) {
                        Log.d("MQTT", "Suscripción a $mqttWaterLevelTopic exitosa")
                    } else {
                        Log.e("MQTT", "Error al suscribirse a $mqttWaterLevelTopic: ${throwable.message}")
                    }
                }
            }
    }

    private fun updateHumidityStatus(humidity: Float) {
        binding.tvHumedadEstado.text = when {
            humidity < 50 -> "Baja"
            humidity in 50.0..80.0 -> "Óptima"
            else -> "Alta"
        }
    }

    private fun updateTemperatureStatus(temperature: Float) {
        binding.tvTemperaturaEstado.text = when {
            temperature < 18 -> "Fría"
            temperature in 18.0..28.0 -> "Ideal"
            else -> "Alta"
        }
    }

    private fun updateWaterLevelStatus(waterLevel: Float) {
        binding.tvAguaEstado.text = when {
            waterLevel < 30 -> "Bajo"
            waterLevel in 30.0..70.0 -> "Medio"
            else -> "Alto"
        }
    }

    private fun setupButtonClickListeners() {
        binding.btnRiegoManual.setOnClickListener {
            // Navegar a la actividad de riego manual
            val intent = Intent(this@DashboardActivity, activity_irrigation_control::class.java)
            startActivity(intent)
        }
        binding.btnVerEstantes.setOnClickListener {
            // Navegar a la actividad de ver estantes
            val intent = Intent(this@DashboardActivity, activity_shelves::class.java)
            startActivity(intent)
        }
        binding.btnHistorial.setOnClickListener {
            // Navegar a la actividad de historial
            val intent = Intent(this@DashboardActivity, activity_history_analysis::class.java)
            startActivity(intent)
        }
        binding.btnClima.setOnClickListener {
            // Navegar a la actividad de clima
            val intent = Intent(this@DashboardActivity, activity_weather::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient?.disconnect()
    }
}
