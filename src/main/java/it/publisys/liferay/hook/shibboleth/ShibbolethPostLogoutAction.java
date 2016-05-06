/*******************************************************************************
 * Shibboleth SPID Auto Login
 * <p>
 * Copyright (c) 2016 Publisys S.p.A. srl (http://www.publisys.it).
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package it.publisys.liferay.hook.shibboleth;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import it.publisys.liferay.hook.shibboleth.util.ShibbolethPropsValues;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

/**
 * Effettua la logout da Shibboleth
 *
 * @author mcolucci
 * @version 1.0
 * @since <pre>14/04/16</pre>
 */
public class ShibbolethPostLogoutAction
        extends Action {

    private static final Log _log = LogFactory.getLog(ShibbolethPostLogoutAction.class);

    static {
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier() {

                    public boolean verify(String hostname,
                                          javax.net.ssl.SSLSession sslSession) {
                        return true;
                    }
                });
    }

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response)
            throws ActionException {

        boolean _shibbEnabled = ShibbolethPropsValues.SHIBBOLETH_ENABLED;
        if (!_shibbEnabled) {
            return;
        }

        if (_log.isDebugEnabled()) {
            _log.debug("- start");
        }

        String _logout = ShibbolethPropsValues.SHIBBOLETH_LOGOUT;
        if (_log.isDebugEnabled()) {
            _log.debug("Logout URL configurata: " + _logout);
        }

        String _cookies = _prepareCookies(request);
        if (_log.isDebugEnabled()) {
            _log.debug(_cookies);
        }

        _connect(_logout, _cookies);
        try {
            response.sendRedirect(ShibbolethPropsValues.LOGOUT_REDIRECT_URL);
        } catch (Exception e) {
            _log.warn("Impossibile effettuare la redirect al Portale dei Servizi.", e);
        }
    }

    /**
     * Effettua una {@link HttpURLConnection} inviando anche i cookies
     *
     * @param url     url
     * @param cookies cookies
     * @return response code
     */
    private int _connect(String url, String cookies) {
        int responseCode = -1;
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] xcs, String string)
                                throws CertificateException {
                        }

                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] xcs, String string)
                                throws CertificateException {
                        }

                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }

        HttpURLConnection connection = null;
        try {
            URL _url = new URL(url);
            connection = (HttpURLConnection) _url.openConnection(Proxy.NO_PROXY);
            connection.setRequestProperty("Cookie", cookies);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");

            responseCode = connection.getResponseCode();
            _log.info("Logout Shibb response code: " + responseCode);

            if (responseCode == 200 && _log.isDebugEnabled()) {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "UTF-8")
                    );
                    StringBuilder _buffer = new StringBuilder();
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        _buffer.append(line);
                    }
                    _log.debug(_buffer.toString());
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }

            }

        } catch (MalformedURLException mue) {
            mue.printStackTrace(System.err);
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }
        return responseCode;
    }

    /**
     * Prepara i {@link Cookie}s da inoltrare
     *
     * @param request {@link HttpServletRequest}
     * @return cookies string
     */
    private String _prepareCookies(HttpServletRequest request) {
        StringBuilder _cookieBuffer = new StringBuilder();
        try {
            Cookie[] _cookies = request.getCookies();
            for (Cookie _cookie : _cookies) {
                _cookieBuffer.append(_cookie.getName()).append("=")
                        .append(URLEncoder.encode(_cookie.getValue(), "UTF-8"));
                _cookieBuffer.append("; ");
            }
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace(System.err);
        }

        if (_cookieBuffer.length() > 2) {
            _cookieBuffer.delete(_cookieBuffer.length() - 2,
                    _cookieBuffer.length());
        }
        return _cookieBuffer.toString();
    }
}
