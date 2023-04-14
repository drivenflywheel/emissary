package emissary.core.serialization;

/**
 * {@link emissary.core.IBaseDataObject} "setter" method names. These methods are invoked via reflection during IBDO
 * conversion to/from XML
 */
public class IBDOMethodNames {

    /**
     * The IBaseDataObject set method name for Birth Order.
     */
    static final String SET_BIRTH_ORDER = "setBirthOrder";
    /**
     * The IBaseDataObject set method name for Broken.
     */
    static final String SET_BROKEN = "setBroken";
    /**
     * The IBaseDataObject set method name for Classification.
     */
    static final String SET_CLASSIFICATION = "setClassification";
    /**
     * The IBaseDataObject set method name for Current Form.
     */
    static final String PUSH_CURRENT_FORM = "pushCurrentForm";
    /**
     * The IBaseDataObject set method name for Data.
     */
    static final String SET_CHANNEL_FACTORY = "setChannelFactory";
    /**
     * The IBaseDataObject set method name for Filename.
     */
    static final String SET_FILENAME = "setFilename";
    /**
     * The IBaseDataObject set method name for Font Encoding.
     */
    static final String SET_FONT_ENCODING = "setFontEncoding";
    /**
     * The IBaseDataObject set method name for Footer.
     */
    static final String SET_FOOTER = "setFooter";
    /**
     * The IBaseDataObject set method name for Header.
     */
    static final String SET_HEADER = "setHeader";
    /**
     * The IBaseDataObject set method name for Header Encoding.
     */
    static final String SET_HEADER_ENCODING = "setHeaderEncoding";
    /**
     * The IBaseDataObject set method name for Id.
     */
    static final String SET_ID = "setId";
    /**
     * The IBaseDataObject set method name for Num Siblings.
     */
    static final String SET_NUM_CHILDREN = "setNumChildren";
    /**
     * The IBaseDataObject set method name for Num Siblings.
     */
    static final String SET_NUM_SIBLINGS = "setNumSiblings";
    /**
     * The IBaseDataObject set method name for Outputable.
     */
    static final String SET_OUTPUTABLE = "setOutputable";
    /**
     * The IBaseDataObject set method name for Parameters.
     */
    static final String PUT_PARAMETER = "putParameter";
    /**
     * The IBaseDataObject set method name for Priority.
     */
    static final String SET_PRIORITY = "setPriority";
    /**
     * The IBaseDataObject set method name for Processing Error.
     */
    static final String ADD_PROCESSING_ERROR = "addProcessingError";
    /**
     * The IBaseDataObject set method name for Transaction Id.
     */
    static final String SET_TRANSACTION_ID = "setTransactionId";
    /**
     * The IBaseDataObject set method name for View.
     */
    static final String ADD_ALTERNATE_VIEW = "addAlternateView";
    /**
     * The IBaseDataObject set method name for Work Bundle Id.
     */
    static final String SET_WORK_BUNDLE_ID = "setWorkBundleId";

    private IBDOMethodNames() {}

}
