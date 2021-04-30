package dev.hbeck.alt.text.storage

import dev.hbeck.alt.text.proto.AltTextCoordinate
import dev.hbeck.alt.text.proto.InternalAltText


interface AltTextMutator {
    fun writeAltText(altText: InternalAltText): Boolean
    fun deleteAltText(coordinate: AltTextCoordinate): Boolean
}
