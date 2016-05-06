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

package it.publisys.liferay.hook.shibboleth.util;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsUtil;

/**
 * @author mcolucci
 * @version 1.0
 * @since <pre>14/04/16</pre>
 */
public class ShibbolethPropsValues {

    // portal property values
    public static final boolean SHIBBOLETH_ENABLED = GetterUtil.getBoolean(PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_ENABLED));
    public static final String SHIBBOLETH_LOGOUT = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_LOGOUT);
    // ims property values
    public static final String LOGOUT_REDIRECT_URL = PropsUtil.get(ShibbolethPropsKeys.LOGOUT_REDIRECT_URL);

    // attribute name values
    public static final String SHIBBOLETH_NAME = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_NAME);
    public static final String SHIBBOLETH_FAMILY_NAME = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_FAMILY_NAME);
    public static final String SHIBBOLETH_PLACE_OF_BIRTH = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_PLACE_OF_BIRTH);
    public static final String SHIBBOLETH_COUNTY_OF_BIRTH = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_COUNTY_OF_BIRTH);
    public static final String SHIBBOLETH_DATE_OF_BIRTH = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_DATE_OF_BIRTH);
    public static final String SHIBBOLETH_GENDER = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_GENDER);
    public static final String SHIBBOLETH_FISCAL_NUMBER = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_FISCAL_NUMBER);
    public static final String SHIBBOLETH_HOME_PHONE = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_HOME_PHONE);
    public static final String SHIBBOLETH_MOBILE_PHONE = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_MOBILE_PHONE);
    public static final String SHIBBOLETH_EMAIL = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_EMAIL);
    public static final String SHIBBOLETH_DIGITAL_ADDRESS = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_DIGITAL_ADDRESS);
    public static final String SHIBBOLETH_VALIDATE = PropsUtil.get(ShibbolethPropsKeys.SHIBBOLETH_VALIDATE);

}
