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

import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.struts.LastPath;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.model.CompanyConstants;
import com.liferay.portal.model.Contact;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.AutoLogin;
import com.liferay.portal.security.auth.AutoLoginException;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.ContactLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import it.publisys.liferay.hook.shibboleth.util.LiferayCustomAttributeKeys;
import it.publisys.liferay.hook.shibboleth.util.ShibbolethPropsValues;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ShibbolethAutoLogin
 *
 * @author mcolucci
 * @version 1.0
 * @since <pre>14/04/16</pre>
 */
public class ShibbolethAutoLogin
        implements AutoLogin {

    private static final Log _log = LogFactory.getLog(ShibbolethAutoLogin.class);
    //
    private static final String LTYPE_PARAM = "ltype";
    //
    private static final String LTYPE_VALUE = "shibb";
    //
    private static final String LOGIN_PATH = "/c/portal/login";
    //
    private static final String SHIB_PREFIX = "shib-";

    @SuppressWarnings("unchecked")
    public String[] login(HttpServletRequest request,
                          HttpServletResponse response) throws
            AutoLoginException {
        if (_log.isDebugEnabled()) {
            _log.debug(" - start");
        }

        String[] credentials = null;
        //
        boolean _isNew = false;
        long companyId = 0;
        //
        String _ltype = request.getParameter(LTYPE_PARAM);
        String _login = null;
        User _user = null;
        try {
            // verifico se il request path e' quello di login
            if (request.getRequestURI().contains(LOGIN_PATH)) {
                if (_log.isDebugEnabled()) {
                    _log.debug(LTYPE_PARAM + ": " + _ltype);
                }

                // verifico che il parametro 'ltype' sia 'shibb'
                if (Validator.isNotNull(_ltype) && LTYPE_VALUE.equals(_ltype)) {

                    if (_log.isDebugEnabled()) {
                        Enumeration<Object> _ens = request.getHeaderNames();
                        while (_ens.hasMoreElements()) {
                            Object _o = _ens.nextElement();
                            if (_o.toString().startsWith(SHIB_PREFIX)) {
                                _log.debug("*=*=* " + _o + ": " + request.getHeader(_o.toString()));
                            }
                        }
                    }

                    String authType = getAuthType(companyId);

                    // Parametro trasmesso dallo ShibbolethSP assunto come default
                    _login = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_EMAIL);

                    if (authType.equals(CompanyConstants.AUTH_TYPE_SN)) {
                        _login = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_FISCAL_NUMBER);
                    } else if (authType.equals(CompanyConstants.AUTH_TYPE_EA)) {
                        _login = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_EMAIL);
                    }

                    if (Validator.isNotNull(_login)) {
                        _login = _login.toUpperCase();

                        companyId = PortalUtil.getCompany(request).getCompanyId();

                        if (authType.equals(CompanyConstants.AUTH_TYPE_SN)) {
                            _log.info("Login by ScreenName: " + _login);
                            _user = loginByScreenName(companyId, _login);
                        } else if (authType.equals(CompanyConstants.AUTH_TYPE_EA)) {
                            _log.info("Login by Email: " + _login);
                            _user = loginByEmail(companyId, _login);
                        }

                        if (_user == null) {
                            _log.warn(String.format("Utente non presente in archivio Liferay. [_login:%s]", _login));
                            throw new NoSuchUserException("Utente non presente in archivio Liferay.");
                        }

                        credentials = new String[]{
                                String.valueOf(_user.getUserId()),
                                _user.getPassword(),
                                String.valueOf(_user.isPasswordEncrypted())
                        };

                        request.getSession().setAttribute(WebKeys.USER_ID, _user.getUserId());

                    } else {
                        SessionMessages.add(request, "shibbError", "Parametri per la login non ricevuti.");
                        throw new AutoLoginException("Parametri per la login non ricevuti.");
                    }
                }
            }
        } catch (NoSuchUserException e) {
            _log.warn("No Such User with login: " + _login + ". Insert new User.");
            _isNew = true;
        } catch (Exception e) {
            _log.error("Generic Error.", e);
            SessionMessages.add(request, "shibbError", "Si &egrave; verificato un errore. Riprovare pi&ugrave; tardi.");
            throw new AutoLoginException(e);
        }

        // creare nuovo utente se _isNew
        if (_isNew) {
            _user = _createNewUser(companyId, request);
            if (_user != null) {
                credentials = new String[]{
                        String.valueOf(_user.getUserId()),
                        _user.getPassword(),
                        String.valueOf(_user.isPasswordEncrypted())};

                request.getSession().setAttribute(WebKeys.USER_ID, _user.getUserId());
            }
        }

        if (_user != null) {
            _updateUser(_user, request);

            // aggiorno la data di ultimo accesso
            try {
                UserLocalServiceUtil.updateLastLogin(_user.getUserId(), request.getRemoteAddr());
            } catch (PortalException e) {
                _log.warn("Impossibile aggiornare la data di ultimo accesso dell'utente [" + _user.getUserId() + "]", e);
            } catch (SystemException e) {
                _log.warn("Impossibile aggiornare la data di ultimo accesso dell'utente [" + _user.getUserId() + "]", e);
            }
        }

        String _redirecturl = request.getParameter("redirecturl");
        if (_redirecturl != null && "true".equals(_redirecturl)) {
            _log.info("============================================");
            _log.info("[Redirect URL] " + _redirecturl);
            _log.info("Effettuo la redirect");

            String _sname = request.getParameter("sname");
            _log.info("[sname] " + _sname);
            String _path = request.getParameter("path");
            _log.info("[path] " + _path);

            Map<String, String[]> params = new HashMap<String, String[]>();
            params.put("sname", new String[]{_sname});

            LastPath lastPath = new LastPath("/", _path, params);
            request.getSession().setAttribute(WebKeys.LAST_PATH, lastPath);
            _log.info("[LastPath] " + lastPath);
            _log.info("============================================");
        }

        return credentials;
    }

    /**
     * Aggiornamento dati utente
     *
     * @param user    user
     * @param request {@link HttpServletRequest}
     */
    private void _updateUser(User user, HttpServletRequest request) {
        _setCustomAttribute(user, request);

        try {
            String firstname = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_NAME);
            String lastname = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_FAMILY_NAME);
            boolean male = !"F".equalsIgnoreCase(request.getHeader(ShibbolethPropsValues.SHIBBOLETH_GENDER));

            user.setFirstName(firstname);
            user.setLastName(lastname);

            Contact _contact = ContactLocalServiceUtil.getContact(user.getContactId());
            if (_contact != null) {
                if (_log.isDebugEnabled())
                    _log.debug("Avvio aggiornamento Contact: " + _contact.getContactId() + ":" + _contact.getUserId());

                _contact.setMale(male);
                // ddn
                String _ddnStr = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_DATE_OF_BIRTH);

                if (Validator.isNotNull(_ddnStr)) {
                    try {
                        Date _ddn = _parseDate(_ddnStr);
                        if (_ddn != null) {
                            _contact.setBirthday(_ddn);
                        }
                    } catch (Exception e) {
                        e.printStackTrace(System.out);
                    }
                }

                ContactLocalServiceUtil.updateContact(_contact, true);

                if (_log.isDebugEnabled())
                    _log.debug("Contact aggiornato: " + _contact.getContactId() + ":" + _contact.getUserId());
            }

            user = UserLocalServiceUtil.updateUser(user, true);
            if (_log.isDebugEnabled())
                _log.debug(String.format("Utente aggiornato [%s]", user.getUserId()));

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    /**
     * Setting dei custom attribute dell'utente
     *
     * @param user    user
     * @param request request
     */
    private void _setCustomAttribute(User user, HttpServletRequest request) {

        if (_log.isDebugEnabled())
            _log.debug("[START] Setto i Custom Attribute.");

        String _pec = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_DIGITAL_ADDRESS);
        String _fiscalcode = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_FISCAL_NUMBER);
        _fiscalcode = _fiscalcode.toUpperCase();
        //
        String _birthplace = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_PLACE_OF_BIRTH);
        String _birthplace_stato = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_COUNTY_OF_BIRTH);
        //
        String _telephonenumber = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_HOME_PHONE);
        String _cellularnumber = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_MOBILE_PHONE);

        String _validate = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_VALIDATE);

        //custom-attribute-list
        PermissionChecker _oldChecker = PermissionThreadLocal.getPermissionChecker();
        try {
            if (_log.isDebugEnabled())
                _log.debug("Setto i permessi per i Custom Attribute.");

            PermissionChecker _permissionChecker = PermissionCheckerFactoryUtil.create(user, false);
            PermissionThreadLocal.setPermissionChecker(_permissionChecker);

            user.getExpandoBridge().setAttribute(LiferayCustomAttributeKeys.FISCAL_CODE_NAME, _fiscalcode);
            user.getExpandoBridge().setAttribute(LiferayCustomAttributeKeys.PEC_CODE_NAME, _pec);
            user.getExpandoBridge().setAttribute(LiferayCustomAttributeKeys.BIRTHPLACE_CODE_NAME, _birthplace);
            user.getExpandoBridge().setAttribute(LiferayCustomAttributeKeys.BIRTHPLACE_STATO_CODE_NAME, _birthplace_stato);

            if (_log.isDebugEnabled())
                _log.debug("Settati i Custom Attribute base.");

            user.getExpandoBridge().setAttribute(LiferayCustomAttributeKeys.TELEPHONE_CODE_NAME, _telephonenumber);
            user.getExpandoBridge().setAttribute(LiferayCustomAttributeKeys.MOBILE_PHONE_CODE_NAME, _cellularnumber);

            if (_log.isDebugEnabled())
                _log.debug("Settati i Custom Attribute contatti telefonici.");

            user.getExpandoBridge().setAttribute(LiferayCustomAttributeKeys.VALIDATE_CODE_NAME, _validate);

            if (_log.isDebugEnabled())
                _log.debug("Settato il Custom Attribute validate.");

        } catch (Exception ex) {
            _log.error(ex, ex);
        } finally {
            PermissionThreadLocal.setPermissionChecker(_oldChecker);
        }

        if (_log.isDebugEnabled())
            _log.debug("[END] Setto i Custom Attribute.");
    }

    /**
     * Creazione di un nuovo utente utilizzando gli attributi inoltrati dallo ShibbolethSP
     *
     * @param companyId companyId
     * @param request   companyId
     * @return User
     * @throws AutoLoginException
     */
    private User _createNewUser(long companyId, HttpServletRequest request)
            throws AutoLoginException {
        if (_log.isDebugEnabled())
            _log.debug("[START] Nuovo utente");

        try {
            long creatorUserId = 0;
            boolean autoPassword = true;
            String password1 = UUID.randomUUID().toString().replaceAll("-", "");
            Locale locale = Locale.ITALY;//PortalUtil.getLocale(request);
            //
            String emailAddress = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_EMAIL);

            String username = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_FISCAL_NUMBER);
            username = username.toUpperCase();

            String firstname = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_NAME);
            String middlename = null;
            String lastname = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_FAMILY_NAME);
            boolean male = !"F".equalsIgnoreCase(request.getHeader(ShibbolethPropsValues.SHIBBOLETH_GENDER));

            if (_log.isDebugEnabled())
                _log.debug("Caricati i dati dall'header.");

            // ddn
            String _ddnStr = request.getHeader(ShibbolethPropsValues.SHIBBOLETH_DATE_OF_BIRTH);
            int birthdayDay = 1;
            int birthdayMonth = 0;
            int birthdayYear = 1970;
            if (Validator.isNotNull(_ddnStr)) {
                try {
                    Date _ddn = _parseDate(_ddnStr);
                    if (_ddn != null) {
                        Calendar _ddnCal = Calendar.getInstance();
                        _ddnCal.setTime(_ddn);
                        birthdayDay = _ddnCal.get(Calendar.DAY_OF_MONTH);
                        birthdayMonth = _ddnCal.get(Calendar.MONTH);
                        birthdayYear = _ddnCal.get(Calendar.YEAR);

                        if (_log.isDebugEnabled())
                            _log.debug("Caricata data di nascita.");
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }

            //
            String jobTitle = null;
            //
            long facebookId = 0;
            String openId = "";
            int prefixId = 0;
            int suffixId = 0;
            //
            boolean sendEmail = false;
            boolean autoScreenName = false;
            //
            long[] groupIds = null;
            long[] organizationIds = null;
            long[] roleIds = null;
            long[] userGroupIds = null;
            ServiceContext serviceContext = new ServiceContext();

            User user = UserLocalServiceUtil.addUser(creatorUserId, companyId,
                    autoPassword, password1, password1, autoScreenName,
                    username, emailAddress, facebookId, openId, locale,
                    firstname, middlename, lastname, prefixId, suffixId, male,
                    birthdayMonth, birthdayDay, birthdayYear, jobTitle,
                    groupIds, organizationIds, roleIds, userGroupIds,
                    sendEmail, serviceContext);

            _log.info("Nuovo utente creato: " + user.getUserId());

            return user;
        } catch (PortalException e) {
            _log.error("Method addUser launched a PortalException.", e);
            e.printStackTrace(System.out);
            SessionMessages.add(request, "shibbError", "Si &egrave; verificato un errore durante la configurazione dell'account. [Codice:PE]");
            throw new AutoLoginException(e);
        } catch (SystemException e) {
            _log.error("Method addUser launched a SystemException.", e);
            e.printStackTrace(System.out);
            SessionMessages.add(request, "shibbError", "Si &egrave; verificato un errore durante la configurazione dell'account. [Codice:SE]");
            throw new AutoLoginException(e);
        } catch (RuntimeException e) {
            _log.error("Method addUser launched a Exception.", e);
            e.printStackTrace(System.out);
            SessionMessages.add(request, "shibbError", "Si &egrave; verificato un errore durante la creazione dell'account. [Codice:E]");
            throw new AutoLoginException(e);
        } catch (Exception e) {
            _log.error("Method addUser launched a Exception.", e);
            e.printStackTrace(System.out);
            SessionMessages.add(request, "shibbError", "Si &egrave; verificato un errore durante la creazione dell'account. [Codice:E]");
            throw new AutoLoginException(e);
        }
    }

    private User loginByEmail(long companyId, String login)
            throws NoSuchUserException, Exception {
        return UserLocalServiceUtil.getUserByEmailAddress(companyId, login);
    }

    private User loginByScreenName(long companyId, String login)
            throws NoSuchUserException, Exception {
        return UserLocalServiceUtil.getUserByScreenName(companyId, login);
    }

    /**
     * Ricavo da Liferay la tipologia di autenticazione configurata (username o email)
     *
     * @param companyId companyId
     * @return auth type
     * @throws Exception
     */
    private static String getAuthType(long companyId)
            throws Exception {
        return GetterUtil.getString(getValue(companyId, PropsKeys.COMPANY_SECURITY_AUTH_TYPE), CompanyConstants.AUTH_TYPE_EA);
    }

    private static String getValue(long companyId, String key)
            throws Exception {
        return PrefsPropsUtil.getString(companyId, key);
    }

    public static Date _parseDate(String val) {
        String p1 = "dd/MM/yyyy";
        String p2 = "yyyy-MM-dd";

        SimpleDateFormat _sdf1 = new SimpleDateFormat(p1);
        SimpleDateFormat _sdf2 = new SimpleDateFormat(p2);

        Date date = null;
        try {
            date = _sdf2.parse(val);
        } catch (Exception ex) {
            try {
                date = _sdf1.parse(val);
            } catch (Exception exc) {
                exc.printStackTrace(System.err);
            }
        }

        return date;
    }
}
