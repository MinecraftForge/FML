/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     cpw - implementation
 */

package cpw.mods.fml.common;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.cert.Certificate;

/**
 * Helper for extracting fingerprints from certificates and bytebuffers
 */
public class CertificateHelper {

    private static final String HEXES = "0123456789abcdef";

    /**
     * Get the fingerprint from a certificate
     * @param certificate The certificate to retrieve a fingerprint from
     * @return The fingerprint of the certificate
     */
    public static String getFingerprint(Certificate certificate)
    {
        if (certificate == null)
        {
            return "NO VALID CERTIFICATE FOUND";
        }
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] der = certificate.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            return hexify(digest);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Get the fingerprint from a buffer of bytes
     * @param buffer The buffer to retrieve the fingerprint from
     * @return The fingerprint of the buffer
     */
    public static String getFingerprint(ByteBuffer buffer)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(buffer);
            byte[] chksum = digest.digest();
            return hexify(chksum);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Convert a byte array to a hex string
     * @param chksum The byte array to convert
     * @return A hex string from the byte array
     */
    private static String hexify(byte[] chksum)
    {
        final StringBuilder hex = new StringBuilder( 2 * chksum.length );
        for ( final byte b : chksum ) {
          hex.append(HEXES.charAt((b & 0xF0) >> 4))
             .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

}
