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
package fr.paris.lutece.plugins.identitystore.modules.mobilecertifier.web;

import fr.paris.lutece.plugins.identitystore.modules.mobilecertifier.service.MobileCertifierService;
import fr.paris.lutece.plugins.identitystore.modules.mobilecertifier.service.MobileCertifierService.ValidationResult;
import fr.paris.lutece.portal.service.security.UserNotSignedException;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.MVCApplication;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.l10n.LocaleService;
import fr.paris.lutece.portal.web.xpages.XPage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;


/**
 * Mobile Certifier App
 */
@Controller( xpageName = "mobilecertifier", pageTitleI18nKey = "module.identitystore.mobilecertifier.xpage.mobilecertifier.pageTitle", pagePathI18nKey = "module.identitystore.mobilecertifier.xpage.mobilecertifier.pagePathLabel" )
public class MobileCertifierApp extends MVCApplication
{
    private static final long serialVersionUID = 1L;
    private static final String TEMPLATE_HOME = "skin/plugins/identitystore/modules/mobilecertifier/home.html";
    private static final String TEMPLATE_VALIDATION_CODE = "skin/plugins/identitystore/modules/mobilecertifier/validation_code.html";
    private static final String TEMPLATE_VALIDATION_OK = "skin/plugins/identitystore/modules/mobilecertifier/validation_ok.html";
    private static final String VIEW_HOME = "home";
    private static final String VIEW_VALIDATION_CODE = "validationCode";
    private static final String VIEW_VALIDATION_OK = "validationOK";
    private static final String ACTION_CERTIFY = "certify";
    private static final String ACTION_VALIDATE_CODE = "validateCode";
    private static final String PARAMETER_MOBILE_NUMBER = "mobile_number";
    private static final String PARAMETER_VALIDATION_CODE = "validation_code";
    private static final String PATTERN_PHONE = "(\\d{10})$";
    private static final String PROPERTY_PATTERN = "module.identitystore.mobilecertifier.numbervalidation.regexp";
    private static final String MESSAGE_KEY_INVALID_NUMBER = "module.identitystore.mobilecertifier.message.invalidNumber";
    private static final String MESSAGE_CODE_VALIDATION_SEND_ERROR = "module.identitystore.mobilecertifier.message.codeValidationSendError";

    /**
     * Gets the Home page
     *
     * @param request The HTTP request
     * @return The XPage
     */
    @View( value = VIEW_HOME, defaultView = true )
    public XPage home( HttpServletRequest request )
    {
        return getXPage( TEMPLATE_HOME, LocaleService.getDefault(  ), getModel(  ) );
    }

    /**
     * process the mobile number
     * @param request The HTTP request
     * @return The redirected page
     * @throws UserNotSignedException if no user is connected
     */
    @Action( ACTION_CERTIFY )
    public XPage doCertify( HttpServletRequest request )
        throws UserNotSignedException
    {
        String strMobileNumber = request.getParameter( PARAMETER_MOBILE_NUMBER );
        String strErrorKey = validateNumber( strMobileNumber );

        if ( strErrorKey != null )
        {
            addError( strErrorKey, request.getLocale(  ) );

            return redirectView( request, VIEW_HOME );
        }
        try 
        {
            MobileCertifierService.startValidation( request, strMobileNumber );
        } 
        catch ( AppException appEx ) 
        {
            addError( MESSAGE_CODE_VALIDATION_SEND_ERROR , request.getLocale(  ) );
            return redirectView( request, VIEW_HOME );
            
        }

        return redirectView( request, VIEW_VALIDATION_CODE );
    }

    /**
     * Displays Validation code filling page
     * @param request The HTTP request
     * @return The page
     */
    @View( VIEW_VALIDATION_CODE )
    public XPage validationCode( HttpServletRequest request )
    {
        return getXPage( TEMPLATE_VALIDATION_CODE, LocaleService.getDefault(  ), getModel(  ) );
    }

    /**
     * process the validation
     * @param request The HTTP request
     * @return The redirected page
     */
    @Action( ACTION_VALIDATE_CODE )
    public XPage doValidateCode( HttpServletRequest request )
    {
        String strValidationCode = request.getParameter( PARAMETER_VALIDATION_CODE );
        ValidationResult result = MobileCertifierService.validate( request, strValidationCode );

        if ( result != ValidationResult.OK )
        {
            addError( result.getMessageKey(  ), LocaleService.getDefault(  ) );

            if ( result == ValidationResult.SESSION_EXPIRED )
            {
                return redirectView( request, VIEW_HOME );
            }

            return redirectView( request, VIEW_VALIDATION_CODE );
        }

        return redirectView( request, VIEW_VALIDATION_OK );
    }

    /**
     * Displays Validation OK page
     * @param request The HTTP request
     * @return The page
     */
    @View( VIEW_VALIDATION_OK )
    public XPage validationOK( HttpServletRequest request )
    {
        return getXPage( TEMPLATE_VALIDATION_OK );
    }

    /**
     * Validate a given mobile phone number
     * @param strMobileNumber The phone number
     * @return A message key if an error occures otherwise null
     */
    private String validateNumber( String strMobileNumber )
    {
        String strPattern = AppPropertiesService.getProperty( PROPERTY_PATTERN, PATTERN_PHONE );
        Pattern pattern = Pattern.compile( strPattern );
        Matcher matcher = pattern.matcher( strMobileNumber.trim(  ) );

        if ( !matcher.matches(  ) )
        {
            return MESSAGE_KEY_INVALID_NUMBER;
        }

        return null;
    }
}
