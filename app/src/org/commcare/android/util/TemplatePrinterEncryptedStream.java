package org.commcare.android.util;

import org.commcare.android.crypt.CryptUtil;
import org.commcare.dalvik.activities.TemplatePrinterActivity;
import org.commcare.dalvik.application.CommCareApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Provides an output stream to write encyrypted in ZIP format to the specified file location,
 * as well as an input stream to read back decrypted from that file.
 *
 * @author amstone
 */
public class TemplatePrinterEncryptedStream {

    private SecretKey key;
    private String pathWithoutExtension;
    private String inputExtension;

    public TemplatePrinterEncryptedStream(String pathWithoutExtension, String inputExtension) {
        this.pathWithoutExtension = pathWithoutExtension;
        this.inputExtension = inputExtension;
        this.key = CryptUtil.generateSemiRandomKey();
    }

    /**
     * Returns an output stream to WRITE to a .HTML version of the print output file (in ZIP
     * format), and encrypts what is being written.
     * Used by TemplatePrinterTask
     */
    public OutputStream getHtmlOutputStream() {
        try {
            Cipher encrypter = Cipher.getInstance("AES");
            encrypter.init(Cipher.ENCRYPT_MODE, key);
            FileOutputStream fos = new FileOutputStream(
                    new File(pathWithoutExtension + inputExtension));
            CipherOutputStream cos = new CipherOutputStream(fos, encrypter);
            //return new ZipOutputStream(cos);
            return cos;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an input stream to READ back from the .HTML file generated by the above output stream.
     * Used by the docxToPDF in TemplatePrinterUtils
     */
    public InputStream getHtmlInputStream() {
        try {
            Cipher decrypter = Cipher.getInstance("AES");
            decrypter.init(Cipher.DECRYPT_MODE, this.key);
            FileInputStream fis = new FileInputStream(
                    new File(pathWithoutExtension + inputExtension));
            CipherInputStream cis = new CipherInputStream(fis, decrypter);
            //return cis;
            return fis;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an output stream to WRITE to a .PDF version of the print output file, and encrypts
     * what is being written.
     * Used by docxToPDF in TemplatePrinterUtils
     */
    public OutputStream getPdfOutputStream() {
        try {
            Cipher encrypter = Cipher.getInstance("AES");
            encrypter.init(Cipher.ENCRYPT_MODE, key);
            FileOutputStream fos = new FileOutputStream(new File(pathWithoutExtension + ".pdf"));
            CipherOutputStream cos = new CipherOutputStream(fos, encrypter);
            return cos;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Create an input stream to READ back from the .PDF file generated by the above output stream.
     * Used by PdfPrintDocumentAdapter
     */
    public InputStream getPdfInputStream() {
        try {
            Cipher decrypter = Cipher.getInstance("AES");
            decrypter.init(Cipher.DECRYPT_MODE, this.key);
            FileInputStream fis = new FileInputStream(new File(pathWithoutExtension + ".pdf"));
            CipherInputStream cis = new CipherInputStream(fis, decrypter);
            return cis;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
