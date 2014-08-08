package org.ihtsdo.rvf.validation.model;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.digester3.Rule;
import org.xml.sax.Attributes;

import java.beans.PropertyDescriptor;

import static java.lang.String.format;
import static org.apache.commons.beanutils.BeanUtils.setProperty;
import static org.apache.commons.beanutils.PropertyUtils.getPropertyDescriptor;

/**
 *
 */
public class SetNamePropertyRule extends Rule
{

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a "set property" rule with the specified name and value attributes.
     *
     * @param elementName Name of the attribute that will contain the name of the property to be set
     * @param propertyName Name of the attribute that will contain the value to which the property should be set
     */
    public SetNamePropertyRule(String elementName, String propertyName)
    {
        this.elementName = elementName;
        this.propertyName = propertyName;
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * The attribute that will contain the property name.
     */
    protected String elementName = null;

    /**
     * The attribute that will contain the property value.
     */
    protected String propertyName = null;

    // --------------------------------------------------------- Public Methods

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin( String namespace, String name, Attributes attributes )
            throws Exception
    {
        if ( attributes.getLength() == 0 )
        {
            return;
        }

        // Identify the actual property name and value to be used
        String actualName = null;
        String actualValue = null;
        for ( int i = 0; i < attributes.getLength(); i++ )
        {
            String attributeName = attributes.getLocalName( i );
            if ( "".equals( attributeName ) )
            {
                attributeName = attributes.getQName( i );
            }
            String value = attributes.getValue( i );
            if ( attributeName.equals( this.elementName) )
            {
                actualName = propertyName;
                actualValue = value;
            }
            
        }

        // Get a reference to the top object
        Object top = getDigester().peek();

        // Log some debugging information
        if ( getDigester().getLogger().isDebugEnabled() )
        {
            getDigester().getLogger().debug( format( "[SetPropertiesRule]{%s} Set %s property %s to %s",
                    getDigester().getMatch(),
                    top.getClass().getName(),
                    actualName,
                    actualValue ) );
        }

        // Force an exception if the property does not exist
        // (BeanUtils.setProperty() silently returns in this case)
        //
        // This code should probably use PropertyUtils.isWriteable(),
        // like SetPropertiesRule does.
        if ( top instanceof DynaBean)
        {
            DynaProperty desc = ( (DynaBean) top ).getDynaClass().getDynaProperty( actualName );
            if ( desc == null )
            {
                throw new NoSuchMethodException( "Bean has no property named " + actualName );
            }
        }
        else
        /* this is a standard JavaBean */
        {
            PropertyDescriptor desc = getPropertyDescriptor( top, actualName );
            if ( desc == null )
            {
                throw new NoSuchMethodException( "Bean has no property named " + actualName );
            }
        }

        // Set the property (with conversion as necessary)
        setProperty( top, actualName, actualValue );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return format( "SetPropertyRule[name=%s, value=%s]", elementName, propertyName);
    }
}
