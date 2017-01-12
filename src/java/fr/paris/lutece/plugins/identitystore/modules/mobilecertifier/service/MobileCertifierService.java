/*
 * Copyright (c) 2002-2016, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.identitystore.modules.mobilecertifier.service;

import fr.paris.lutece.plugins.grubusiness.business.customer.Customer;
import fr.paris.lutece.plugins.grubusiness.business.demand.Demand;
import fr.paris.lutece.plugins.grubusiness.business.notification.BackofficeNotification;
import fr.paris.lutece.plugins.grubusiness.business.notification.BroadcastNotification;
import fr.paris.lutece.plugins.grubusiness.business.notification.Notification;
import fr.paris.lutece.plugins.grubusiness.business.notification.SMSNotification;
import fr.paris.lutece.plugins.grubusiness.business.notification.DashboardNotification;
import fr.paris.lutece.plugins.grubusiness.business.notification.EmailAddress;
import fr.paris.lutece.plugins.identitystore.business.AttributeCertificate;
import fr.paris.lutece.plugins.identitystore.business.AttributeCertifier;
import fr.paris.lutece.plugins.identitystore.business.AttributeCertifierHome;
import fr.paris.lutece.plugins.identitystore.business.Identity;
import fr.paris.lutece.plugins.identitystore.business.IdentityHome;
import fr.paris.lutece.plugins.identitystore.service.ChangeAuthor;
import fr.paris.lutece.plugins.identitystore.service.IdentityStoreService;
import fr.paris.lutece.plugins.identitystore.web.service.AuthorType;
import fr.paris.lutece.plugins.librarynotifygru.services.NotificationService;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

import org.apache.commons.lang.RandomStringUtils;

import java.sql.Timestamp;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


/**
 * Mobile Certifier Service
 */
public class MobileCertifierService
{
    private static final String MESSAGE_CODE_VALIDATION_OK = "module.identitystore.mobilecertifier.message.validation.ok";
    private static final String MESSAGE_CODE_VALIDATION_INVALID = "module.identitystore.mobilecertifier.message.validation.invalidCode";
    private static final String MESSAGE_SESSION_EXPIRED = "module.identitystore.mobilecertifier.message.validation.sessionExpired";
    private static final String MESSAGE_CODE_EXPIRED = "module.identitystore.mobilecertifier.message.validation.codeExpired";
    private static final String MESSAGE_TOO_MANY_ATTEMPS = "module.identitystore.mobilecertifier.message.validation.tooManyAttempts";
    private static final String MESSAGE_SMS_VALIDATION_TEXT = "module.identitystore.mobilecertifier.message.validation.smsValidationText";
    private static final String MESSAGE_SMS_VALIDATION_CONFIRM_TEXT = "module.identitystore.mobilecertifier.message.validation.smsValidationConfirmText";
    private static final String PROPERTY_CODE_LENGTH = "identitystore.mobilecertifier.codeLength";
    private static final String PROPERTY_EXPIRES_DELAY = "identitystore.mobilecertifier.expiresDelay";
    private static final String PROPERTY_MAX_ATTEMPTS = "identitystore.mobilecertifier.maxAttempts";
    private static final String PROPERTY_MOCKED_EMAIL = "identitystore.mobilecertifier.mockedEmail";
    private static final String PROPERTY_MOCKED_CONNECTION_ID = "identitystore.mobilecertifier.mockedConnectionId";
    private static final String PROPERTY_API_MANAGER_ENABLED = "identitystore.mobilecertifier.apiManager.enabled";
    private static final String PROPERTY_ATTRIBUTE = "identitystore.mobilecertifier.attribute";
    private static final String PROPERTY_CERTIFIER_CODE = "identitystore.mobilecertifier.certifierCode";
    private static final String PROPERTY_MOBILE_CERTIFIER_CLOSE_CRM_STATUS_ID = "identitystore.mobilecertifier.crmCloseStatusId";
    private static final String PROPERTY_MOBILE_CERTIFIER_CLOSE_DEMAND_STATUS_ID = "identitystore.mobilecertifier.demandCloseStatusId";
    private static final String PROPERTY_MOBILE_CERTIFIER_DEMAND_TYPE_ID = "identitystore.mobilecertifier.demandTypeId";
    private static final String PROPERTY_MOBILE_CERTIFIER_NOTIFICATION_TYPE = "identitystore.mobilecertifier.notificationType";
    private static final String PROPERTY_GRU_NOTIF_EMAIL_SENDER_MAIL = "identitystore.mobilecertifier.senderMail";
    private static final String PROPERTY_GRU_NOTIF_EMAIL_SENDER_NAME = "identitystore.mobilecertifier.senderName";
    private static final String PROPERTY_CERTIFICATE_LEVEL = "identitystore.mobilecertifier.certificate.level";
    private static final String PROPERTY_CERTIFICATE_EXPIRATION_DELAY = "identitystore.mobilecertifier.certificate.expirationDelay";
    private static final String MESSAGE_GRU_NOTIF_DASHBOARD_STATUS_TEXT = "module.identitystore.mobilecertifier.gru.notif.dashboard.statusText";
    private static final String MESSAGE_GRU_NOTIF_DASHBOARD_MESSAGE = "module.identitystore.mobilecertifier.gru.notif.dashboard.message";
    private static final String MESSAGE_GRU_NOTIF_DASHBOARD_SUBJECT = "module.identitystore.mobilecertifier.gru.notif.dashboard.subject";
    private static final String MESSAGE_GRU_NOTIF_DASHBOARD_DATA = "module.identitystore.mobilecertifier.gru.notif.dashboard.data";
    private static final String MESSAGE_GRU_NOTIF_DASHBOARD_SENDER_NAME = "module.identitystore.mobilecertifier.gru.notif.dashboard.senderName";
    private static final String MESSAGE_GRU_NOTIF_EMAIL_SUBJECT = "module.identitystore.mobilecertifier.gru.notif.email.subject";
    private static final String MESSAGE_GRU_NOTIF_EMAIL_MESSAGE = "module.identitystore.mobilecertifier.gru.notif.email.message";
    private static final String MESSAGE_GRU_NOTIF_AGENT_MESSAGE = "module.identitystore.mobilecertifier.gru.notif.agent.message";
    private static final String MESSAGE_GRU_NOTIF_AGENT_STATUS_TEXT = "module.identitystore.mobilecertifier.gru.notif.agent.statusText";
    private static final String DEFAULT_ATTRIBUTE_NAME = "mobile_phone";
    private static final String DEFAULT_CERTIFIER_CODE = "mobilecertifier";
    private static final String DEFAULT_CONNECTION_ID = "1";
    private static final String DEFAULT_EMAIL = "test@test.fr";
    private static final int DEFAULT_MOBILE_CERTIFIER_CRM_CLOSE_STATUS_ID = 1;
    private static final int DEFAULT_MOBILE_CERTIFIER_DEMAND_CLOSE_STATUS_ID = 1;
    private static final String DEFAULT_MOBILE_CERTIFIER_DEMAND_TYPE_ID = "401";
    private static final int DEFAULT_LENGTH = 6;
    private static final int DEFAULT_EXPIRES_DELAY = 5;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final int NO_CERTIFICATE_EXPIRATION_DELAY = -1;
    private static final int DEFAULT_CERTIFICATE_LEVEL = 1;
    private static final String ATTRIBUTE_NAME = AppPropertiesService.getProperty( PROPERTY_ATTRIBUTE,
            DEFAULT_ATTRIBUTE_NAME );
    private static final String CERTIFIER_CODE = AppPropertiesService.getProperty( PROPERTY_CERTIFIER_CODE,
            DEFAULT_CERTIFIER_CODE );
    private static final String MOCKED_USER_CONNECTION_ID = AppPropertiesService.getProperty( PROPERTY_MOCKED_CONNECTION_ID,
            DEFAULT_CONNECTION_ID );
    private static final String MOCKED_USER_EMAIL = AppPropertiesService.getProperty( PROPERTY_MOCKED_EMAIL,
            DEFAULT_EMAIL );
    private static final int EXPIRES_DELAY = AppPropertiesService.getPropertyInt( PROPERTY_EXPIRES_DELAY,
            DEFAULT_EXPIRES_DELAY );
    private static final int CODE_LENGTH = AppPropertiesService.getPropertyInt( PROPERTY_CODE_LENGTH, DEFAULT_LENGTH );
    private static final int MAX_ATTEMPTS = AppPropertiesService.getPropertyInt( PROPERTY_MAX_ATTEMPTS,
            DEFAULT_MAX_ATTEMPTS );
    private static final int CERTIFICATE_EXPIRATION_DELAY = AppPropertiesService.getPropertyInt( PROPERTY_CERTIFICATE_EXPIRATION_DELAY,
            NO_CERTIFICATE_EXPIRATION_DELAY );
    private static final int CERTIFICATE_LEVEL = AppPropertiesService.getPropertyInt( PROPERTY_CERTIFICATE_LEVEL,
            DEFAULT_CERTIFICATE_LEVEL );
    private static final String SERVICE_NAME = "Mobile Certifier Service";
    private static final String DEMAND_PREFIX = "MOBCERT_";
    private static final String BEAN_NOTIFICATION_SENDER = "identitystore-mobilecertifier.lib-notifygru.notificationService";
    private static Map<String, ValidationInfos> _mapValidationCodes = new HashMap<String, ValidationInfos>(  );
    private NotificationService _notifyGruSenderService;

    /**
     * constructor
     */
    public MobileCertifierService(  )
    {
        super(  );
        _notifyGruSenderService = SpringContextService.getBean( BEAN_NOTIFICATION_SENDER );
    }

    /**
     * Starts the validation process by generating and sending a validation code
     *
     * @param request
     *          The HTTP request
     * @param strMobileNumber
     *          The mobile phone number
     * @param strCustomerId
     *          customer Id
     * @throws fr.paris.lutece.portal.service.security.UserNotSignedException
     *           if no user found
     */
    public void startValidation( HttpServletRequest request, String strMobileNumber, String strCustomerId )
        throws UserNotSignedException
    {
        String strValidationCode = generateValidationCode(  );
        AppLogService.debug( "MobileCertifierService.startValidation for [" + strCustomerId + "][" + strMobileNumber +
            "] with code " + strValidationCode );

        if ( AppPropertiesService.getPropertyBoolean( PROPERTY_API_MANAGER_ENABLED, true ) )
        {

            Notification certifNotif = buildSendSMSCodeNotif( getUserConnectionId( request ),
                    strMobileNumber, strCustomerId, strValidationCode, request.getLocale(  ) );

            _notifyGruSenderService.send( certifNotif );
        }
        else
        {
            // mock mode => validation code is logged on file
            AppLogService.info( I18nService.getLocalizedString( MESSAGE_SMS_VALIDATION_TEXT,
                    new String[] { strValidationCode }, request.getLocale(  ) ) );
        }

        HttpSession session = request.getSession( true );
        ValidationInfos infos = new ValidationInfos(  );
        infos.setValidationCode( strValidationCode );
        infos.setExpiresTime( getExpiresTime(  ) );
        infos.setMobileNumber( strMobileNumber );
        infos.setUserConnectionId( getUserConnectionId( request ) );
        infos.setUserEmail( getUserEmail( request ) );
        infos.setCustomerId( strCustomerId );

        _mapValidationCodes.put( session.getId(  ), infos );
    }

    /**
     * Validate a validation code
     *
     * @param request
     *          The request
     * @param strValidationCode
     *          The validation code
     * @return A validation result
     */
    public ValidationResult validate( HttpServletRequest request, String strValidationCode )
    {
        AppLogService.debug( "MobileCertifierService.validate with code " + strValidationCode );

        HttpSession session = request.getSession(  );

        if ( session == null )
        {
            return ValidationResult.SESSION_EXPIRED;
        }

        String strKey = session.getId(  );
        ValidationInfos infos = _mapValidationCodes.get( strKey );

        if ( infos == null )
        {
            return ValidationResult.SESSION_EXPIRED;
        }

        if ( infos.getInvalidAttempts(  ) > MAX_ATTEMPTS )
        {
            return ValidationResult.TOO_MANY_ATTEMPS;
        }

        if ( ( strValidationCode == null ) || !strValidationCode.equals( infos.getValidationCode(  ) ) )
        {
            infos.setInvalidAttempts( infos.getInvalidAttempts(  ) + 1 );

            return ValidationResult.INVALID_CODE;
        }

        if ( infos.getExpiresTime(  ) < now(  ) )
        {
            _mapValidationCodes.remove( strKey );

            return ValidationResult.CODE_EXPIRED;
        }

        _mapValidationCodes.remove( strKey );
        certify( infos, request.getLocale(  ) );

        return ValidationResult.OK;
    }

    /**
     * Certify the attribute change
     *
     * @param infos
     *          The validation infos
     * @param locale
     *          the locale
     */
    private void certify( ValidationInfos infos, Locale locale )
    {
        AttributeCertifier certifier = AttributeCertifierHome.findByCode( CERTIFIER_CODE );
        AttributeCertificate certificate = new AttributeCertificate(  );
        certificate.setCertificateDate( new Timestamp( new Date(  ).getTime(  ) ) );
        certificate.setCertificateLevel( CERTIFICATE_LEVEL );
        certificate.setIdCertifier( certifier.getId(  ) );
        certificate.setCertifier( certifier.getName(  ) );

        if ( CERTIFICATE_EXPIRATION_DELAY != NO_CERTIFICATE_EXPIRATION_DELAY )
        {
            Calendar c = Calendar.getInstance(  );
            c.setTime( new Date(  ) );
            c.add( Calendar.DATE, CERTIFICATE_EXPIRATION_DELAY );
            certificate.setExpirationDate( new Timestamp( c.getTime(  ).getTime(  ) ) );
        }

        ChangeAuthor author = new ChangeAuthor(  );
        author.setApplication( SERVICE_NAME );
        author.setType( AuthorType.TYPE_USER_OWNER.getTypeValue(  ) );

        Identity identity = IdentityHome.findByConnectionId( infos.getUserConnectionId(  ) );
        IdentityStoreService.setAttribute( identity, ATTRIBUTE_NAME, infos.getMobileNumber(  ), author, certificate );

        if ( AppPropertiesService.getPropertyBoolean( PROPERTY_API_MANAGER_ENABLED, true ) )
        {
            Notification certifNotif = buildCertifiedNotif( infos, locale );

            _notifyGruSenderService.send( certifNotif );
        }
        else
        {
            // mock mode => certification message is logged
            AppLogService.info( I18nService.getLocalizedString( MESSAGE_SMS_VALIDATION_CONFIRM_TEXT, locale ) );
        }
    }

    /**
     * build a notification from validation infos
     *
     * @param infos
     *          validations infos
     * @param locale
     *          locale
     * @return Notification notification to send (SMS, agent,
     *         dashboard, email)
     */
    private static Notification buildCertifiedNotif( ValidationInfos infos, Locale locale )
    {
        Notification certifNotif = new Notification(  );
        certifNotif.setNotificationDate( new Date(  ).getTime(  ) );

        Demand demand = new Demand(  );
        demand.setId( generateDemandId(  ) );
        demand.setReference( DEMAND_PREFIX + demand.getId(  ) );
        demand.setStatusId( AppPropertiesService.getPropertyInt( PROPERTY_MOBILE_CERTIFIER_CLOSE_DEMAND_STATUS_ID,
                DEFAULT_MOBILE_CERTIFIER_DEMAND_CLOSE_STATUS_ID ) );
        demand.setTypeId( AppPropertiesService.getProperty( PROPERTY_MOBILE_CERTIFIER_DEMAND_TYPE_ID,
                DEFAULT_MOBILE_CERTIFIER_DEMAND_TYPE_ID ) );

        Customer customer = new Customer(  );
        customer.setId( infos.getCustomerId(  ) );
        customer.setAccountGuid( infos.getUserConnectionId(  ) );
        customer.setEmail( infos.getUserEmail(  ) );
        demand.setCustomer( customer );

        certifNotif.setDemand( demand );

        SMSNotification notifSMS = new SMSNotification(  );
        notifSMS.setMessage( I18nService.getLocalizedString( MESSAGE_SMS_VALIDATION_CONFIRM_TEXT, locale ) );
        notifSMS.setPhoneNumber( infos.getMobileNumber(  ) );
        certifNotif.setUserSMS( notifSMS );

        DashboardNotification notifDashboard = new DashboardNotification(  );
        notifDashboard.setStatusId( AppPropertiesService.getPropertyInt( 

                PROPERTY_MOBILE_CERTIFIER_CLOSE_CRM_STATUS_ID, DEFAULT_MOBILE_CERTIFIER_CRM_CLOSE_STATUS_ID )  );

        notifDashboard.setSubject( I18nService.getLocalizedString( MESSAGE_GRU_NOTIF_DASHBOARD_SUBJECT,
                new String[] { infos.getMobileNumber(  ) }, locale ) );
        notifDashboard.setMessage( I18nService.getLocalizedString( MESSAGE_GRU_NOTIF_DASHBOARD_MESSAGE,
                new String[] { infos.getMobileNumber(  ) }, locale ) );
        notifDashboard.setStatusText( I18nService.getLocalizedString( MESSAGE_GRU_NOTIF_DASHBOARD_STATUS_TEXT, locale ) );
        notifDashboard.setSenderName( I18nService.getLocalizedString( MESSAGE_GRU_NOTIF_DASHBOARD_SENDER_NAME, locale ) );
        notifDashboard.setData( I18nService.getLocalizedString( MESSAGE_GRU_NOTIF_DASHBOARD_DATA,
                new String[] { infos.getMobileNumber(  ) }, locale ) );
        certifNotif.setUserDashboard( notifDashboard );


        BroadcastNotification broadcastEmail = new BroadcastNotification(  );
        broadcastEmail.setMessage( I18nService.getLocalizedString( MESSAGE_GRU_NOTIF_EMAIL_MESSAGE,
                new String[] { infos.getMobileNumber(  ) }, locale ) );
        broadcastEmail.setSubject( I18nService.getLocalizedString( MESSAGE_GRU_NOTIF_EMAIL_SUBJECT,
                new String[] { infos.getMobileNumber(  ) }, locale ) );
        broadcastEmail.setSenderEmail( AppPropertiesService.getProperty( PROPERTY_GRU_NOTIF_EMAIL_SENDER_MAIL ) );
        broadcastEmail.setSenderName( AppPropertiesService.getProperty( PROPERTY_GRU_NOTIF_EMAIL_SENDER_NAME ) );

        broadcastEmail.setRecipient( EmailAddress.buildEmailAddresses( new String[] { infos.getUserEmail(  ) } ) );

        certifNotif.addBroadcastEmail( broadcastEmail );

        BackofficeNotification notifAgent = new BackofficeNotification(  );
        notifAgent.setMessage( I18nService.getLocalizedString( MESSAGE_GRU_NOTIF_AGENT_MESSAGE,
                new String[] { infos.getMobileNumber(  ) }, locale ) );
        notifAgent.setStatusText( I18nService.getLocalizedString( MESSAGE_GRU_NOTIF_AGENT_STATUS_TEXT,
                new String[] { infos.getMobileNumber(  ) }, locale ) );
        certifNotif.setBackofficeLogging( notifAgent );

        return certifNotif;
    }

    /**
     * build a Notification which contains only a SMS notification,
     * filled with input params
     *
     * @param strConnectionId
     *          connection Id
     * @param strMobileNumber
     *          mobile phone number to certify
     * @param strCustomerId
     *          customerId
     * @param strValidationCode
     *          sms validation code
     * @param locale
     *          locale
     * @return Notification
     */
    private static Notification buildSendSMSCodeNotif( String strConnectionId, String strMobileNumber,
        String strCustomerId, String strValidationCode, Locale locale )
    {
        Notification certifNotif = new Notification(  );
        SMSNotification notifSMS = new SMSNotification(  );
        notifSMS.setMessage( I18nService.getLocalizedString( MESSAGE_SMS_VALIDATION_TEXT,
                new String[] { strValidationCode }, locale ) );
        notifSMS.setPhoneNumber( strMobileNumber );

        certifNotif.setUserSMS( notifSMS );
        certifNotif.setNotificationDate( new Date(  ).getTime(  ) );

        return certifNotif;
    }

    /**
     * generate demandid for sms certification
     *
     * @return demand id
     */
    private static String generateDemandId(  )
    {
        // FIXME =>how to generate a unique id
        Random rand = new Random(  );
        int randomNum = rand.nextInt(  );

        return String.valueOf( Math.abs( randomNum ) );
    }

    /**
     * returns the user connection ID
     *
     * @param request
     *          The HTTP request
     * @return the user connection ID
     * @throws UserNotSignedException
     *           If no user is connected
     */
    private static String getUserConnectionId( HttpServletRequest request )
        throws UserNotSignedException
    {
        if ( SecurityService.isAuthenticationEnable(  ) )
        {
            LuteceUser user = SecurityService.getInstance(  ).getRegisteredUser( request );

            if ( user != null )
            {
                return user.getName(  );
            }
            else
            {
                throw new UserNotSignedException(  );
            }
        }
        else
        {
            return MOCKED_USER_CONNECTION_ID;
        }
    }

    /**
     * returns the user email
     *
     * @param request
     *          The HTTP request
     * @return the user connection ID
     * @throws UserNotSignedException
     *           If no user is connected
     */
    private static String getUserEmail( HttpServletRequest request )
        throws UserNotSignedException
    {
        if ( SecurityService.isAuthenticationEnable(  ) )
        {
            LuteceUser user = SecurityService.getInstance(  ).getRegisteredUser( request );

            if ( user != null )
            {
                return user.getEmail(  );
            }
            else
            {
                throw new UserNotSignedException(  );
            }
        }
        else
        {
            return MOCKED_USER_EMAIL;
        }
    }

    /**
     * Generate a random alphanumeric code
     *
     * @return The code
     */
    private static String generateValidationCode(  )
    {
        return RandomStringUtils.randomAlphanumeric( CODE_LENGTH ).toUpperCase(  );
    }

    /**
     * Calculate an expiration time
     *
     * @return the time as a long value
     */
    private static long getExpiresTime(  )
    {
        return now(  ) + ( (long) EXPIRES_DELAY * 60000L );
    }

    /**
     * The current time as a long value
     *
     * @return current time as a long value
     */
    private static long now(  )
    {
        return ( new Date(  ) ).getTime(  );
    }

    /**
     * Enumeration of all validation results
     */
    public enum ValidationResult
    {
        OK( MESSAGE_CODE_VALIDATION_OK ),
        INVALID_CODE( MESSAGE_CODE_VALIDATION_INVALID ),
        SESSION_EXPIRED( MESSAGE_SESSION_EXPIRED ),
        CODE_EXPIRED( MESSAGE_CODE_EXPIRED ),
        TOO_MANY_ATTEMPS( MESSAGE_TOO_MANY_ATTEMPS );	    	
    	
    	private String _strMessageKey;

        /**
         * Constructor
         *
         * @param strMessageKey
         *          The i18n message key
         */
        ValidationResult( String strMessageKey )
        {
            _strMessageKey = strMessageKey;
        }

        /**
         * Return the i18n message key
         *
         * @return the i18n message key
         */
        public String getMessageKey(  )
        {
            return _strMessageKey;
        }

    }
}
