package eu.philcar.csg.OBC.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

public class Security {


    public static String getSignature(PackageInfo  p, PackageManager pm) {


        StringBuilder sb = new StringBuilder();


        final String strName = p.applicationInfo.loadLabel(pm).toString();
        final String strVendor = p.packageName;

        sb.append("" + strName + " / " + strVendor + ": \n");

        final Signature[] arrSignatures = p.signatures;
        for (final Signature sig : arrSignatures) {
	        /*
	        * Get the X.509 certificate.
	        */
            final byte[] rawCert = sig.toByteArray();
            InputStream certStream = new ByteArrayInputStream(rawCert);

            final CertificateFactory certFactory;
            final X509Certificate x509Cert;
            try {
                certFactory = CertificateFactory.getInstance("X509");
                x509Cert = (X509Certificate) certFactory.generateCertificate(certStream);

                sb.append("Certificate subject: " + x509Cert.getSubjectDN() + "\n");
                sb.append("Certificate issuer: " + x509Cert.getIssuerDN() + "\n");
                sb.append("Certificate serial number: " + x509Cert.getSerialNumber() + "\n");
                sb.append("\n");
            }
            catch (CertificateException e) {
                // e.printStackTrace();
            }
        }


        return sb.toString();
    }


}