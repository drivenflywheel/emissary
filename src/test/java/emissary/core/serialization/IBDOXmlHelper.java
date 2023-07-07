package emissary.core.serialization;

import emissary.core.BaseDataObject;
import emissary.core.IBaseDataObject;
import emissary.core.channels.SeekableByteChannelFactory;
import emissary.kff.KffDataObjectHandler;
import emissary.util.xml.AbstractJDOMUtil;

import org.apache.commons.lang3.Validate;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static emissary.core.channels.SeekableByteChannelHelper.getByteArrayFromChannel;
import static emissary.core.serialization.ElementDecoders.BASE64_ENCODER;
import static emissary.core.serialization.ElementDecoders.BASE64_NEW_LINE_STRING;
import static emissary.core.serialization.ElementDecoders.BooleanDecoder;
import static emissary.core.serialization.ElementDecoders.ByteArrayDecoder;
import static emissary.core.serialization.ElementDecoders.IntegerDecoder;
import static emissary.core.serialization.ElementDecoders.SeekableByteChannelFactoryDecoder;
import static emissary.core.serialization.ElementDecoders.StringByteArrayDecoder;
import static emissary.core.serialization.ElementDecoders.StringDecoder;
import static emissary.core.serialization.ElementDecoders.StringObjectDecoder;
import static emissary.core.serialization.IBDOMethodNames.*;
import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;

/**
 * This class helps convert IBaseDataObjects to and from XML.
 */
public final class IBDOXmlHelper {
    /**
     * Logger instance
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(IBDOXmlHelper.class);

    /**
     * The XML namespace for "xml".
     */
    public static final Namespace XML_NAMESPACE = Namespace.getNamespace(XML_NS_PREFIX, XML_NS_URI);


    /**
     * A map of element names of IBaseDataObject methods that get/set primitives and their default values.
     */
    public static final Map<String, Object> PRIMITVE_NAME_DEFAULT_MAP = Collections
            .unmodifiableMap(new ConcurrentHashMap<>(Stream.of(
                    new SimpleEntry<>(XmlElementNames.BIRTH_ORDER, new BaseDataObject().getBirthOrder()),
                    new SimpleEntry<>(XmlElementNames.BROKEN, new BaseDataObject().isBroken()),
                    new SimpleEntry<>(XmlElementNames.NUM_CHILDREN, new BaseDataObject().getNumChildren()),
                    new SimpleEntry<>(XmlElementNames.NUM_SIBLINGS, new BaseDataObject().getNumSiblings()),
                    new SimpleEntry<>(XmlElementNames.OUTPUTABLE, new BaseDataObject().isOutputable()),
                    new SimpleEntry<>(XmlElementNames.PRIORITY, new BaseDataObject().getPriority()))
                    .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue))));

    private IBDOXmlHelper() {}

    /**
     * Setup a typical BDO
     * 
     * @param sbcf initial channel factory for the data
     * @param classification initial classification string
     * @param formAndFileType initial form and file type
     * @param kff an existing Kff handler
     * @return a typical BDO with the specified data
     */
    public static IBaseDataObject createStandardInitialIbdo(final SeekableByteChannelFactory sbcf,
            final String classification, final String formAndFileType, final KffDataObjectHandler kff) {
        final IBaseDataObject ibdo = new BaseDataObject();
        final IBaseDataObject tempIbdo = new BaseDataObject();

        // We want to return the ibdo with the data field equal to null. This can only
        // be accomplished if the data is never set. Therefore, we have to set the data
        // on a separate ibdo, hash the ibdo and then transfer just the parameters back
        // to the original ibdo.
        tempIbdo.setChannelFactory(sbcf);
        kff.hash(tempIbdo);
        ibdo.setParameters(tempIbdo.getParameters());

        ibdo.setCurrentForm(formAndFileType);
        ibdo.setFileType(formAndFileType);
        ibdo.setClassification(classification);

        return ibdo;
    }

    /**
     * Creates an IBaseDataObject and associated children from an XML document.
     * 
     * @param document containing the IBaseDataObject and children descriptions.
     * @param children the list where the children will be added.
     * @return the IBaseDataObject.
     */
    public static IBaseDataObject ibdoFromXml(final Document document, final List<IBaseDataObject> children) {
        Validate.notNull(document, "Required document != null!");
        Validate.notNull(children, "Required children != null!");

        final Element root = document.getRootElement();
        final Element answersElement = root.getChild(XmlElementNames.ANSWERS);
        final IBaseDataObject parentIbdo = new BaseDataObject();
        final List<Element> answerChildren = answersElement.getChildren();

        ibdoFromXmlMainElements(answersElement, parentIbdo);

        for (final Element answerChild : answerChildren) {
            final IBaseDataObject childIbdo = new BaseDataObject();
            final String childName = answerChild.getName();

            if (childName.startsWith(XmlElementNames.EXTRACT)) {
                parentIbdo.addExtractedRecord(ibdoFromXmlMainElements(answerChild, childIbdo));
            } else if (childName.startsWith(XmlElementNames.ATTACHMENT_ELEMENT_PREFIX)) {
                children.add(ibdoFromXmlMainElements(answerChild, childIbdo));
            }
        }

        return parentIbdo;
    }

    /**
     * Creates an IBaseDataObject from an XML element excluding Extracted Records and children.
     * 
     * @param element to create IBaseDataObject from.
     * @param ibdo to apply the element values to.
     * @return the IBaseDataObject that was passed in.
     */
    public static IBaseDataObject ibdoFromXmlMainElements(final Element element, final IBaseDataObject ibdo) {
        parseElement(element.getChild(XmlElementNames.DATA), ibdo, SET_CHANNEL_FACTORY,
                SeekableByteChannelFactoryDecoder);
        parseElement(element.getChild(XmlElementNames.BIRTH_ORDER), ibdo, SET_BIRTH_ORDER, IntegerDecoder);
        parseElement(element.getChild(XmlElementNames.BROKEN), ibdo, SET_BROKEN, StringDecoder);
        parseElement(element.getChild(XmlElementNames.CLASSIFICATION), ibdo, SET_CLASSIFICATION,
                StringDecoder);

        for (final Element currentForm : element.getChildren(XmlElementNames.CURRENT_FORM)) {
            parseElement(currentForm, ibdo, PUSH_CURRENT_FORM, StringDecoder);
        }

        parseElement(element.getChild(XmlElementNames.FILENAME), ibdo, SET_FILENAME, StringDecoder);
        parseElement(element.getChild(XmlElementNames.FONT_ENCODING), ibdo, SET_FONT_ENCODING, StringDecoder);
        parseElement(element.getChild(XmlElementNames.FOOTER), ibdo, SET_FOOTER, ByteArrayDecoder);
        parseElement(element.getChild(XmlElementNames.HEADER_ELEMENT_NAME), ibdo, SET_HEADER, ByteArrayDecoder);
        parseElement(element.getChild(XmlElementNames.HEADER_ENCODING), ibdo, SET_HEADER_ENCODING,
                StringDecoder);
        parseElement(element.getChild(XmlElementNames.ID), ibdo, SET_ID, StringDecoder);
        parseElement(element.getChild(XmlElementNames.NUM_CHILDREN), ibdo, SET_NUM_CHILDREN, IntegerDecoder);
        parseElement(element.getChild(XmlElementNames.NUM_SIBLINGS), ibdo, SET_NUM_SIBLINGS, IntegerDecoder);
        parseElement(element.getChild(XmlElementNames.OUTPUTABLE), ibdo, SET_OUTPUTABLE, BooleanDecoder);
        parseElement(element.getChild(XmlElementNames.PRIORITY), ibdo, SET_PRIORITY, IntegerDecoder);
        parseElement(element.getChild(XmlElementNames.PROCESSING_ERROR), ibdo, ADD_PROCESSING_ERROR,
                StringDecoder);
        parseElement(element.getChild(XmlElementNames.TRANSACTION_ID), ibdo, SET_TRANSACTION_ID,
                StringDecoder);
        parseElement(element.getChild(XmlElementNames.WORK_BUNDLE_ID), ibdo, SET_WORK_BUNDLE_ID,
                StringDecoder);

        for (final Element parameter : element.getChildren(XmlElementNames.META)) {
            parseElement(parameter, ibdo, PUT_PARAMETER, StringObjectDecoder);
        }

        for (final Element view : element.getChildren(XmlElementNames.VIEW)) {
            parseElement(view, ibdo, ADD_ALTERNATE_VIEW, StringByteArrayDecoder);
        }

        return ibdo;
    }

    /**
     * Parse an element to set the value on a BDO
     * 
     * @param element to get the data from
     * @param ibdo to set the data on
     * @param ibdoMethodName to use to set the data
     * @param elementDecoder to use to decode the element data
     */
    private static void parseElement(final Element element, final IBaseDataObject ibdo, final String ibdoMethodName,
            final ElementDecoder elementDecoder) {
        if (element != null) {
            final Object parameter = elementDecoder.decode(element);

            if (parameter != null) {
                setParameterOnIbdo(elementDecoder.getKeyClass(), elementDecoder.getValueClass(), ibdo, ibdoMethodName,
                        parameter, element);
            }
        }
    }

    /**
     * Set a parameter on a specific BDO
     * 
     * @param keyClass to use for the key, otherwise assumes string
     * @param valueClass to use for the value
     * @param ibdo to set the parameter on
     * @param ibdoMethodName method name to use (e.g. setFontEncoding)
     * @param parameter value to use
     * @param element to get the name from
     */
    private static void setParameterOnIbdo(final Class<?> keyClass, final Class<?> valueClass,
            final IBaseDataObject ibdo, final String ibdoMethodName, final Object parameter, final Element element) {
        try {
            if (keyClass == null) {
                final Method method = IBaseDataObject.class.getDeclaredMethod(ibdoMethodName, valueClass);

                method.invoke(ibdo, parameter);
            } else {
                final String name = (String) StringDecoder.decode(element.getChild(XmlElementNames.NAME));
                final Method method = IBaseDataObject.class.getDeclaredMethod(ibdoMethodName, keyClass, valueClass);

                method.invoke(ibdo, name, parameter);
            }
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            LOGGER.warn("Unable to call ibdo method {}!", ibdoMethodName, e);
        }
    }

    /**
     * Creates an XML string from a parent IBaseDataObject and a list of children IBaseDataObjects.
     * 
     * @param parent the parent IBaseDataObject
     * @param children the children IBaseDataObjects.
     * @param initialIbdo the initial IBaseDataObject.
     * @return the XML string.
     */
    public static String xmlFromIbdo(final IBaseDataObject parent, final List<IBaseDataObject> children,
            final IBaseDataObject initialIbdo) {
        Validate.notNull(parent, "Required: parent != null!");
        Validate.notNull(children, "Required: children != null!");
        Validate.notNull(initialIbdo, "Required: initialIbdo != null!");

        final Element rootElement = new Element(XmlElementNames.RESULT);
        final Element setupElement = new Element(XmlElementNames.SETUP);

        rootElement.addContent(setupElement);

        xmlFromIbdoMainElements(initialIbdo, setupElement);

        final Element answersElement = new Element(XmlElementNames.ANSWERS);

        rootElement.addContent(answersElement);

        xmlFromIbdoMainElements(parent, answersElement);

        final List<IBaseDataObject> extractedRecords = parent.getExtractedRecords();
        if (extractedRecords != null) {
            for (int i = 0; i < extractedRecords.size(); i++) {
                final IBaseDataObject extractedRecord = extractedRecords.get(i);
                final Element extractElement = new Element(XmlElementNames.EXTRACT + (i + 1));

                xmlFromIbdoMainElements(extractedRecord, extractElement);

                answersElement.addContent(extractElement);
            }
        }

        for (int i = 0; i < children.size(); i++) {
            final IBaseDataObject child = children.get(i);
            final Element childElement = new Element(XmlElementNames.ATTACHMENT_ELEMENT_PREFIX + (i + 1));

            xmlFromIbdoMainElements(child, childElement);

            answersElement.addContent(childElement);
        }

        return AbstractJDOMUtil.toString(new Document(rootElement));
    }

    /**
     * Creates xml from the IBaseDataObject excluding the extracted records and children.
     * 
     * @param ibdo to create xml from.
     * @param element to add the xml to.
     */
    public static void xmlFromIbdoMainElements(final IBaseDataObject ibdo, final Element element) {
        addNonNullContent(element, ibdo.getChannelFactory());
        addNonDefaultContent(element, XmlElementNames.BIRTH_ORDER, ibdo.getBirthOrder());
        addNonNullContent(element, XmlElementNames.BROKEN, ibdo.getBroken());
        addNonNullContent(element, XmlElementNames.CLASSIFICATION, ibdo.getClassification());

        final int childCount = element.getChildren().size();
        for (final String currentForm : ibdo.getAllCurrentForms()) {
            element.addContent(childCount, protectedElement(XmlElementNames.CURRENT_FORM, currentForm));
        }

        addNonNullContent(element, XmlElementNames.FILENAME, ibdo.getFilename());
        addNonNullContent(element, XmlElementNames.FONT_ENCODING, ibdo.getFontEncoding());
        addNonNullContent(element, XmlElementNames.FOOTER, ibdo.footer());
        addNonNullContent(element, XmlElementNames.HEADER_ELEMENT_NAME, ibdo.header());
        addNonNullContent(element, XmlElementNames.HEADER_ENCODING, ibdo.getHeaderEncoding());
        addNonNullContent(element, XmlElementNames.ID, ibdo.getId());
        addNonDefaultContent(element, XmlElementNames.NUM_CHILDREN, ibdo.getNumChildren());
        addNonDefaultContent(element, XmlElementNames.NUM_SIBLINGS, ibdo.getNumSiblings());
        addNonDefaultContent(element, ibdo.isOutputable());
        addNonDefaultContent(element, XmlElementNames.PRIORITY, ibdo.getPriority());

        final String processingError = ibdo.getProcessingError();
        final String fixedProcessingError = processingError == null ? null
                : processingError.substring(0, processingError.length() - 1);
        addNonNullContent(element, XmlElementNames.PROCESSING_ERROR, fixedProcessingError);

        addNonNullContent(element, XmlElementNames.TRANSACTION_ID, ibdo.getTransactionId());
        addNonNullContent(element, XmlElementNames.WORK_BUNDLE_ID, ibdo.getWorkBundleId());

        for (final Entry<String, Collection<Object>> parameter : ibdo.getParameters().entrySet()) {
            for (final Object item : parameter.getValue()) {
                final Element metaElement = new Element(XmlElementNames.META);

                element.addContent(metaElement);
                metaElement.addContent(preserve(protectedElement(XmlElementNames.NAME, parameter.getKey())));
                metaElement.addContent(preserve(protectedElement(XmlElementNames.VALUE_ELEMENT_NAME, item.toString())));
            }
        }

        for (final Entry<String, byte[]> view : ibdo.getAlternateViews().entrySet()) {
            final Element metaElement = new Element(XmlElementNames.VIEW);

            element.addContent(metaElement);
            metaElement.addContent(preserve(protectedElement(XmlElementNames.NAME, view.getKey())));
            metaElement.addContent(preserve(protectedElement(XmlElementNames.VALUE_ELEMENT_NAME, view.getValue())));
        }
    }

    private static void addNonNullContent(final Element parent, final String elementName, final String string) {
        if (string != null) {
            parent.addContent(preserve(protectedElement(elementName, string)));
        }
    }

    private static void addNonNullContent(final Element parent, final String elementName, final byte[] bytes) {
        if (bytes != null) {
            parent.addContent(preserve(protectedElement(elementName, bytes)));
        }
    }

    private static void addNonNullContent(final Element parent, final SeekableByteChannelFactory sbcf) {
        if (sbcf != null) {
            try {
                final byte[] bytes = getByteArrayFromChannel(sbcf, BaseDataObject.MAX_BYTE_ARRAY_SIZE);
                addNonNullContent(parent, XmlElementNames.DATA, bytes);
            } catch (final IOException e) {
                LOGGER.error("Could not get bytes from SeekableByteChannel!", e);
            }
        }
    }

    private static void addNonDefaultContent(final Element parent, final boolean bool) {
        if ((Boolean) PRIMITVE_NAME_DEFAULT_MAP.get(XmlElementNames.OUTPUTABLE) != bool) {
            parent.addContent(AbstractJDOMUtil.simpleElement(XmlElementNames.OUTPUTABLE, bool));
        }
    }

    private static void addNonDefaultContent(final Element parent, final String elementName, final int integer) {
        if ((Integer) PRIMITVE_NAME_DEFAULT_MAP.get(elementName) != integer) {
            parent.addContent(AbstractJDOMUtil.simpleElement(elementName, integer));
        }
    }

    private static Element preserve(final Element element) {
        element.setAttribute("space", "preserve", XML_NAMESPACE);

        return element;
    }

    private static Element protectedElement(final String name, final String string) {
        return protectedElement(name, string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a 'protected' element which can be encoded with base64 if it contains unsafe characters
     * 
     * See method source for specific definition of 'unsafe'.
     * 
     * @param name of the element
     * @param bytes to wrap, if they contain unsafe characters
     * @return the created element
     */
    private static Element protectedElement(final String name, final byte[] bytes) {
        final Element element = new Element(name);

        boolean badCharacters = false;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] < 9 || bytes[i] > 13 && bytes[i] < 32) {
                badCharacters = true;
                break;
            }
        }
        if (badCharacters) {
            final StringBuilder base64String = new StringBuilder();
            base64String.append(BASE64_NEW_LINE_STRING);
            base64String.append(BASE64_ENCODER.encodeToString(bytes));
            base64String.append(BASE64_NEW_LINE_STRING);

            element.setAttribute(XmlAttributeNames.ENCODING_ATTRIBUTE_NAME, XmlAttributeNames.BASE64_ATTRIBUTE_NAME);
            element.addContent(base64String.toString());
        } else {
            element.addContent(new String(bytes, StandardCharsets.ISO_8859_1));
        }

        return element;
    }
}
