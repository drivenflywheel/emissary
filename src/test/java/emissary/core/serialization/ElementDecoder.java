package emissary.core.serialization;

import org.jdom2.Element;

/**
 * Interface for decoding an element value.
 */
interface ElementDecoder {
    /**
     * Decodes an XML element.
     *
     * @param element to decode.
     * @return the decoded element value.
     */
    Object decode(Element element);

    /**
     * Returns the class of the key for a mapped value or null for a non-mapped value.
     *
     * @return the class of the key for a mapped value or null for a non-mapped value.
     */
    Class<?> getKeyClass();

    /**
     * Returns the class of the value, whether mapped or non-mapped.
     *
     * @return the class of the value, whether mapped or non-mapped.
     */
    Class<?> getValueClass();
}
