package emissary.core.serialization;

import emissary.core.channels.InMemoryChannelFactory;
import emissary.core.channels.SeekableByteChannelFactory;

import org.jdom2.Element;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static emissary.core.serialization.XmlAttributeNames.BASE64_ATTRIBUTE_NAME;
import static emissary.core.serialization.XmlAttributeNames.ENCODING_ATTRIBUTE_NAME;
import static emissary.core.serialization.XmlElementNames.VALUE_ELEMENT_NAME;

public class ElementDecoders {

    /**
     * Max width of Base64 char block.
     */
    public static final int BASE64_LINE_WIDTH = 76;

    /**
     * New line byte array to use for normalised XML
     */
    static final byte[] BASE64_NEW_LINE_BYTE = {'\n'};

    /**
     * New line string to use for normalised XML
     */
    static final String BASE64_NEW_LINE_STRING = new String(BASE64_NEW_LINE_BYTE);

    /**
     * The Base64 encoder.
     *
     * Uses same width as default, but overrides new line separator to use normalised XML separator.
     *
     * @see <a href="http://www.jdom.org/docs/apidocs/org/jdom2/output/Format.html#setLineSeparator(java.lang.String)"
     *      >Format.setLineSeparator</a>
     */
    static final Base64.Encoder BASE64_ENCODER = Base64.getMimeEncoder(BASE64_LINE_WIDTH, BASE64_NEW_LINE_BYTE);

    /**
     * The Base64 decoder.
     */
    static final Base64.Decoder BASE64_DECODER = Base64.getMimeDecoder();

    /**
     * Implementation of an XML element decoder that has a boolean value.
     */
    public static final ElementDecoder BooleanDecoder = new ElementDecoder() {
        @Override
        public Object decode(final Element element) {
            return Boolean.valueOf(element.getValue());
        }

        @Override
        public Class<?> getKeyClass() {
            return null;
        }

        @Override
        public Class<?> getValueClass() {
            return boolean.class;
        }
    };

    /**
     * Implementation of an XML element decoder that has a SeekableByteChannel value.
     */
    public static final ElementDecoder SeekableByteChannelFactoryDecoder = new ElementDecoder() {
        @Override
        public Object decode(final Element element) {
            final String elementValue = element.getValue();
            final String encoding = element.getAttributeValue(ENCODING_ATTRIBUTE_NAME);

            return InMemoryChannelFactory.create(extractBytes(encoding, elementValue));
        }

        @Override
        public Class<?> getKeyClass() {
            return null;
        }

        @Override
        public Class<?> getValueClass() {
            return SeekableByteChannelFactory.class;
        }
    };

    /**
     * Implementation of an XML element decoder that has a byte array value.
     */
    public static final ElementDecoder ByteArrayDecoder = new ElementDecoder() {
        @Override
        public Object decode(final Element element) {
            final String elementValue = element.getValue();
            final String encoding = element.getAttributeValue(ENCODING_ATTRIBUTE_NAME);

            return extractBytes(encoding, elementValue);
        }

        @Override
        public Class<?> getKeyClass() {
            return null;
        }

        @Override
        public Class<?> getValueClass() {
            return byte[].class;
        }
    };

    /**
     * Implementation of an XML element decoder that has an integer value.
     */
    public static final ElementDecoder IntegerDecoder = new ElementDecoder() {
        @Override
        public Object decode(final Element element) {
            try {
                return Integer.decode(element.getValue());
            } catch (final NumberFormatException e) {
                return null;
            }
        }

        @Override
        public Class<?> getKeyClass() {
            return null;
        }

        @Override
        public Class<?> getValueClass() {
            return int.class;
        }
    };

    /**
     * Implementation of an XML element decoder that has a string value.
     */
    public static final ElementDecoder StringDecoder = new ElementDecoder() {
        @Override
        public Object decode(final Element element) {
            final String elementValue = element.getValue();
            final String encoding = element.getAttributeValue(ENCODING_ATTRIBUTE_NAME);

            return new String(extractBytes(encoding, elementValue), StandardCharsets.UTF_8);
        }

        @Override
        public Class<?> getKeyClass() {
            return null;
        }

        @Override
        public Class<?> getValueClass() {
            return String.class;
        }
    };

    /**
     * Implementation of an XML element decoder that has a mapped value where the key is a string and the value is a byte
     * array.
     */
    public static final ElementDecoder StringByteArrayDecoder = new ElementDecoder() {
        @Override
        public Object decode(final Element element) {
            final Element childElement = element.getChild(VALUE_ELEMENT_NAME);
            final String elementValue = childElement.getValue();
            final String encoding = childElement.getAttributeValue(ENCODING_ATTRIBUTE_NAME);

            return extractBytes(encoding, elementValue);
        }

        @Override
        public Class<?> getKeyClass() {
            return String.class;
        }

        @Override
        public Class<?> getValueClass() {
            return byte[].class;
        }
    };

    /**
     * Implementation of an XML element decoder that has a mapped value where the key is a string and the value is an
     * object.
     */
    public static ElementDecoder StringObjectDecoder = new ElementDecoder() {
        @Override
        public Object decode(final Element element) {
            final Element childElement = element.getChild(VALUE_ELEMENT_NAME);
            final String elementValue = childElement.getValue();
            final String encoding = childElement.getAttributeValue(ENCODING_ATTRIBUTE_NAME);

            return new String(extractBytes(encoding, elementValue), StandardCharsets.UTF_8);
        }

        @Override
        public Class<?> getKeyClass() {
            return String.class;
        }

        @Override
        public Class<?> getValueClass() {
            return Object.class;
        }
    };

    /**
     * Return UTF8 bytes from an XML value, decoding base64 if required
     * 
     * @param encoding e.g. 'base64', otherwise it returns the bytes as they are presented
     * @param elementValue containing the data
     * @return the data from elementValue, decoded from base64 if required
     */
    private static byte[] extractBytes(final String encoding, final String elementValue) {
        if (BASE64_ATTRIBUTE_NAME.equalsIgnoreCase(encoding)) {
            final String newElementValue = elementValue.replace("\n", "");
            final byte[] bytes = newElementValue.getBytes(StandardCharsets.UTF_8);
            return BASE64_DECODER.decode(bytes);
        } else {
            return elementValue.getBytes(StandardCharsets.UTF_8);
        }
    }
}
