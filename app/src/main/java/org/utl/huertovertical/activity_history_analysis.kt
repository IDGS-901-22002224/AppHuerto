package org.utl.huertovertical

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
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
import org.json.JSONArray
import java.nio.charset.StandardCharsets
import java.util.*

class activity_history_analysis : AppCompatActivity() {

    private lateinit var mqttClient: Mqtt5AsyncClient
    private val mqttServer = "09fcbcef2c4b4ab19b4e9afb6519bb50.s1.eu.hivemq.cloud"
    private val mqttUser = "hivemq.webclient.1753930553970"
    private val mqttPass = "#4<TYu25AOzMbpgR9v;>"
    private val substrateHumidityTopic = "huerto/substrate_humidity"
    private val temperatureTopic = "huerto/temperature"

    private val substrateHumidityData = mutableListOf<Float>()
    private val temperatureData = mutableListOf<Float>()
    private val timestamps = mutableListOf<String>()

    private lateinit var substrateHumidityChart: WebView
    private lateinit var temperatureChart: WebView
    private lateinit var humidityText: TextView
    private lateinit var temperatureText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history_analysis)

        substrateHumidityChart = findViewById(R.id.substrateHumidityChart)
        temperatureChart = findViewById(R.id.temperatureChart)
        humidityText = findViewById(R.id.humidityText)
        temperatureText = findViewById(R.id.temperatureText)

        setupWebView(substrateHumidityChart, "Humedad Sustrato")
        setupWebView(temperatureChart, "Temperatura Ambiente")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        CoroutineScope(Dispatchers.IO).launch {
            initializeMqttClient()
        }
    }

    private fun setupWebView(webView: WebView, chartLabel: String) {
        webView.settings.javaScriptEnabled = true
        webView.loadDataWithBaseURL(
            null,
            """
                <!DOCTYPE html>
                <html>
                <body>
                    <canvas id="${chartLabel.replace(" ", "")}" style="width: 100%; height: 100%;"></canvas>
                    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                    <script>
                        var ctx = document.getElementById('${chartLabel.replace(" ", "")}').getContext('2d');
                        var chart = new Chart(ctx, {
                            type: 'line',
                            data: {
                                labels: [],
                                datasets: [{
                                    label: '$chartLabel',
                                    data: [],
                                    borderColor: '${if (chartLabel == "Humedad Sustrato") "#2196F3" else "#FF9800"}',
                                    backgroundColor: '${if (chartLabel == "Humedad Sustrato") "rgba(33, 150, 243, 0.2)" else "rgba(255, 152, 0, 0.2)"}',
                                    borderWidth: 2,
                                    fill: true
                                }]
                            },
                            options: {
                                responsive: true,
                                maintainAspectRatio: false,
                                scales: {
                                    y: {
                                        beginAtZero: ${chartLabel == "Humedad Sustrato"},
                                        suggestedMax: ${if (chartLabel == "Humedad Sustrato") 100 else 30}
                                    }
                                }
                            }
                        });
                        function updateChart(data, labels) {
                            chart.data.labels = labels;
                            chart.data.datasets[0].data = data;
                            chart.update();
                        }
                        window.updateChartFromAndroid = updateChart;
                    </script>
                </body>
                </html>
            """.trimIndent(),
            "text/html",
            "UTF-8",
            null
        )
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
                        Log.d("MQTT", "Conectado a MQTT en History Analysis")
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
                        val value = payload.toFloat()
                        updateData(value, substrateHumidityData)
                        updateChart("HumedadSustrato", substrateHumidityData, substrateHumidityChart)
                        humidityText.text = "${value.toInt()}%"
                    } catch (e: NumberFormatException) {
                        Log.e("MQTT", "Error al convertir payload para $substrateHumidityTopic: $payload")
                    }
                }
            }
            ?.send()

        mqttClient.subscribeWith()
            ?.topicFilter(temperatureTopic)
            ?.qos(MqttQos.AT_LEAST_ONCE)
            ?.callback { publish ->
                val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val value = payload.toFloat()
                        updateData(value, temperatureData)
                        updateChart("TemperaturaAmbiente", temperatureData, temperatureChart)
                        temperatureText.text = "${value.toInt()}°C"
                    } catch (e: NumberFormatException) {
                        Log.e("MQTT", "Error al convertir payload para $temperatureTopic: $payload")
                    }
                }
            }
            ?.send()
    }

    private fun updateData(value: Float, dataList: MutableList<Float>) {
        if (dataList.isEmpty() || dataList.last() != value) {
            dataList.add(value)
            timestamps.add(Date().toString().substring(11, 19))
            if (dataList.size > 20) {
                dataList.removeAt(0)
                timestamps.removeAt(0)
            }
            Log.d("DataUpdate", "Añadido $value a ${dataList.size} puntos")
        }
    }

    private fun updateChart(chartId: String, data: List<Float>, webView: WebView) {
        val jsonData = JSONArray(data.map { it.toString() })
        val jsonLabels = JSONArray(timestamps)

        webView.loadUrl("javascript:updateChartFromAndroid($jsonData, $jsonLabels)")
        Log.d("ChartUpdate", "Actualizando $chartId con datos: $data, labels: $timestamps")
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient.disconnect()
    }
}