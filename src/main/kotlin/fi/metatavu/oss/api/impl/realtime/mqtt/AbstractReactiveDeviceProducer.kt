package fi.metatavu.oss.api.impl.realtime.mqtt

import java.util.*

/**
 * Abstract base class for reactive MQTT message producers producing messages to devices
 */
abstract class AbstractReactiveDeviceProducer<T>: AbstractReactiveProducer<T>() {

    /**
     * Sends a create message to given device
     *
     * @param deviceId device id
     * @param message message
     */
    abstract fun sendCreateMessage(deviceId: UUID, message: T)

    /**
     * Sends an update message to given device
     *
     * @param deviceId device id
     * @param message message
     */
    abstract fun sendUpdateMessage(deviceId: UUID, message: T)

    /**
     * Sends a delete message to given device
     *
     * @param deviceId device id
     * @param message message
     */
    abstract fun sendDeleteMessage(deviceId: UUID, message: T)

    /**
     * Gets MQTT Topic name based on device id
     */
    protected fun getMqttTopicName(deviceId: UUID): String {
        return "$mqttBaseTopic/$environment/$deviceId"
    }
}