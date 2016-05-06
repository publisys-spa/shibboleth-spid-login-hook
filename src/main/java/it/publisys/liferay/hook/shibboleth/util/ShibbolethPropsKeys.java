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

/**
 * @author mcolucci
 * @version 1.0
 * @since <pre>14/04/16</pre>
 */
interface ShibbolethPropsKeys {

    // portal property
    String SHIBBOLETH_ENABLED = "shibboleth.enabled";
    String SHIBBOLETH_LOGOUT = "shibboleth.logout.url";
    // ims
    String LOGOUT_REDIRECT_URL = "logout.redirect.url";
    // attribute name
    String SHIBBOLETH_FISCAL_NUMBER = "shibboleth.attr.fiscalNumber";
    String SHIBBOLETH_NAME = "shibboleth.attr.name";
    String SHIBBOLETH_FAMILY_NAME = "shibboleth.attr.familyName";
    String SHIBBOLETH_PLACE_OF_BIRTH = "shibboleth.attr.placeOfBirth";
    String SHIBBOLETH_COUNTY_OF_BIRTH = "shibboleth.attr.countyOfBirth";
    String SHIBBOLETH_DATE_OF_BIRTH = "shibboleth.attr.dateOfBirth";
    String SHIBBOLETH_GENDER = "shibboleth.attr.gender";
    String SHIBBOLETH_HOME_PHONE = "shibboleth.attr.homePhone";
    String SHIBBOLETH_MOBILE_PHONE = "shibboleth.attr.mobilePhone";
    String SHIBBOLETH_EMAIL = "shibboleth.attr.email";
    String SHIBBOLETH_DIGITAL_ADDRESS = "shibboleth.attr.digitalAddress";
    String SHIBBOLETH_VALIDATE = "shibboleth.attr.validate";

}
