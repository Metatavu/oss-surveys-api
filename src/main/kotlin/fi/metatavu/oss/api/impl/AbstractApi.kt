package fi.metatavu.oss.api.impl

import fi.metatavu.oss.api.impl.crypto.CryptoController
import fi.metatavu.oss.api.impl.devices.DeviceController
import fi.metatavu.oss.api.model.Error
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.*
import javax.enterprise.context.RequestScoped
import javax.inject.Inject
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.core.SecurityContext
import javax.ws.rs.core.HttpHeaders

/**
 * Abstract base class for all API services
 *
 * @author Jari Nykänen
 */
@RequestScoped
abstract class AbstractApi {

    @Inject
    private lateinit var cryptoController: CryptoController

    @Inject
    private lateinit var deviceController: DeviceController

    @Inject
    private lateinit var jsonWebToken: JsonWebToken

    @Context
    private lateinit var securityContext: SecurityContext

    @Context
    lateinit var httpHeaders: HttpHeaders

    /**
     * Returns logged user id
     *
     * @return logged user id
     */
    protected val loggedUserId: UUID?
        get() {
            if (jsonWebToken.subject != null) {
                return UUID.fromString(jsonWebToken.subject)
            }

            return null
        }

    /**
     * Checks whether incoming request from Device has authorized device key as a header
     *
     * @param deviceId device id
     * @return whether device key is authorized
     */
    protected suspend fun isAuthorizedDevice(deviceId: UUID): Boolean {
        val deviceKeyHeader = httpHeaders.requestHeaders[deviceKeyHeader]
        if (deviceKeyHeader.isNullOrEmpty()) {
            return false
        }
        val token = deviceKeyHeader.firstOrNull() ?: return false
        val privateKey = cryptoController.loadPrivateKeyBase64(token) ?: return false
        val deviceKey = deviceController.getDeviceKey(deviceId) ?: return false
        val publicKey = cryptoController.loadPublicKey(deviceKey) ?: return false
        val challenge = cryptoController.signUUID(
            privateKey = privateKey,
            id = deviceId
        ) ?: return false

        return cryptoController.verifyUUID(
            publicKey = publicKey,
            signature = challenge,
            id = deviceId
        )
    }

    /**
     * Constructs ok response
     *
     * @param entity payload
     * @return response
     */
    protected fun createOk(entity: Any?): Response {
        return Response
            .status(Response.Status.OK)
            .entity(entity)
            .build()
    }

    /**
     * Constructs ok response with total count header
     *
     * @param entity payload
     * @return response
     */
    protected fun createOk(entity: Any?, count: Long): Response {
        return Response
            .status(Response.Status.OK)
            .entity(entity)
            .header("Total-Count", count.toString())
            .header("Access-Control-Expose-Headers", "Total-Count")
            .build()
    }

    /**
     * Constructs ok response
     *
     * @return response
     */
    protected fun createOk(): Response {
        return Response
            .status(Response.Status.OK)
            .build()
    }

    /**
     * Constructs created response
     *
     * @return response
     */
    protected fun createCreated(entity: Any?): Response {
        return Response
            .status(Response.Status.CREATED)
            .entity(entity)
            .build()
    }

    /**
     * Constructs accepted response
     *
     * @param entity payload
     * @return response
     */
    protected fun createAccepted(entity: Any?): Response {
        return Response
            .status(Response.Status.ACCEPTED)
            .entity(entity)
            .build()
    }

    /**
     * Constructs no content response
     *
     * @return response
     */
    protected fun createNoContent(): Response {
        return Response
            .status(Response.Status.NO_CONTENT)
            .build()
    }

    /**
     * Constructs bad request response
     *
     * @param message message
     * @return response
     */
    protected fun createBadRequest(message: String): Response {
        return createError(Response.Status.BAD_REQUEST, message)
    }

    /**
     * Constructs not found response
     *
     * @param message message
     * @return response
     */
    protected fun createNotFound(message: String): Response {
        return createError(Response.Status.NOT_FOUND, message)
    }

    /**
     * Constructs not found response
     *
     * @return response
     */
    protected fun createNotFound(): Response {
        return Response
            .status(Response.Status.NOT_FOUND)
            .build()
    }
    /**
     * Constructs not found response
     *
     * @param message message
     * @return response
     */
    protected fun createConflict(message: String): Response {
        return createError(Response.Status.CONFLICT, message)
    }

    /**
     * Constructs not implemented response
     *
     * @param message message
     * @return response
     */
    protected fun createNotImplemented(message: String): Response {
        return createError(Response.Status.NOT_IMPLEMENTED, message)
    }

    /**
     * Constructs internal server error response
     *
     * @param message message
     * @return response
     */
    protected fun createInternalServerError(message: String): Response {
        return createError(Response.Status.INTERNAL_SERVER_ERROR, message)
    }

    /**
     * Constructs forbidden response
     *
     * @param message message
     * @return response
     */
    protected fun createForbidden(message: String): Response {
        return createError(Response.Status.FORBIDDEN, message)
    }

    /**
     * Constructs unauthorized response
     *
     * @param message message
     * @return response
     */
    protected fun createUnauthorized(message: String): Response {
        return createError(Response.Status.UNAUTHORIZED, message)
    }

    /**
     * Creates not found response with given parameters
     *
     * @param target target of the find method
     * @param id ID of the target
     */
    protected fun createNotFoundWithMessage(target: String, id: UUID): Response {
        return createNotFound("$target with ID $id could not be found")
    }

    /**
     * Translates REST parameters first/max results into index range
     *
     * @param firstResult first result
     * @param maxResults max results
     * @return index range
     */
    protected fun firstMaxToRange(
        firstResult: Int?,
        maxResults: Int?
    ): Pair<Int, Int> {
        val first = firstResult ?: 0
        val max = maxResults ?: 10

        return first to max + first - 1
    }

    /**
     * Constructs an error response
     *
     * @param status status code
     * @param message message
     *
     * @return error response
     */
    private fun createError(status: Response.Status, message: String): Response {
        val entity = Error(
            message = message,
            code = status.statusCode
        )

        return Response
            .status(status)
            .entity(entity)
            .build()
    }

    companion object {
        const val NOT_FOUND_MESSAGE = "Not found"
        const val UNAUTHORIZED = "Unauthorized"
        const val FORBIDDEN = "Forbidden"
        const val MISSING_REQUEST_BODY = "Missing request body"

        const val SURVEY = "Survey"
        const val DEVICE_REQUEST = "Device Request"
        const val DEVICE = "Device"
        const val PAGE = "Page"
        const val DEVICE_SURVEY = "Device Survey"
        const val LAYOUT = "Layout"

        const val deviceKeyHeader = "X-DEVICE-KEY"
    }

}
