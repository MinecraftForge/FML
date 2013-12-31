package cpw.mods.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking something as an API
 * @author Benjamin
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface API {
	/**
	 * Who owns this API
	 * @return Whatever owns this API
	 */
    String owner();
    /**
     * What this API provides
     * @return Whatever this API provides
     */
    String provides();
    /**
     * The version of this API
     * @return Which version of the API this is
     */
    String apiVersion();
}
