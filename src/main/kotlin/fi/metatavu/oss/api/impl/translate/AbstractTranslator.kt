package fi.metatavu.oss.api.impl.translate

import fi.metatavu.oss.api.metadata.DBMetadata
import fi.metatavu.oss.api.metadata.MetadataTranslator
import fi.metatavu.oss.api.model.Metadata
import javax.inject.Inject

/**
 * Abstract translator class
 *
 * @author Jari Nykänen
 */
abstract class AbstractTranslator<E: DBMetadata, R> {

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    /**
     * Translates metadata
     *
     * @param entity entity
     * @return rest metadata
     */
    protected suspend fun translateMetadata(entity: E): Metadata {
        return metadataTranslator.translate(entity)
    }

    abstract suspend fun translate(entity: E): R

    /**
     * Translates list of entities
     *
     * @param entities list of entities to translate
     * @return List of translated entities
     */
    open suspend fun translate(entities: List<E>): List<R> {
        return entities.mapNotNull { translate(it) }
    }
}