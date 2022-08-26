//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.09.19 at 03:18:45 PM MESZ 
//


package org.matsim.contrib.minibus.genericUtils.gexf;

import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.matsim.contrib.minibus.genericUtils.gexf.viz.ColorContent;
import org.matsim.contrib.minibus.genericUtils.gexf.viz.NodeShapeContent;
import org.matsim.contrib.minibus.genericUtils.gexf.viz.PositionContent;
import org.matsim.contrib.minibus.genericUtils.gexf.viz.SizeContent;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for node-content complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="node-content">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element ref="{http://www.gexf.net/1.2draft}attvalues"/>
 *         &lt;element ref="{http://www.gexf.net/1.2draft}spells"/>
 *         &lt;choice>
 *           &lt;element ref="{http://www.gexf.net/1.2draft}nodes"/>
 *           &lt;element ref="{http://www.gexf.net/1.2draft}edges"/>
 *         &lt;/choice>
 *         &lt;element ref="{http://www.gexf.net/1.2draft}parents"/>
 *         &lt;choice>
 *           &lt;element ref="{http://www.gexf.net/1.2draft}color"/>
 *           &lt;element ref="{http://www.gexf.net/1.2draft}position"/>
 *           &lt;element ref="{http://www.gexf.net/1.2draft}size"/>
 *           &lt;element name="shape" type="{http://www.gexf.net/1.2draft/viz}node-shape-content"/>
 *         &lt;/choice>
 *       &lt;/choice>
 *       &lt;attribute name="start" type="{http://www.gexf.net/1.2draft}time-type" />
 *       &lt;attribute name="startopen" type="{http://www.gexf.net/1.2draft}time-type" />
 *       &lt;attribute name="end" type="{http://www.gexf.net/1.2draft}time-type" />
 *       &lt;attribute name="endopen" type="{http://www.gexf.net/1.2draft}time-type" />
 *       &lt;attribute name="pid" type="{http://www.gexf.net/1.2draft}id-type" />
 *       &lt;attribute name="id" use="required" type="{http://www.gexf.net/1.2draft}id-type" />
 *       &lt;attribute name="label" type="{http://www.w3.org/2001/XMLSchema}token" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "node-content", propOrder = {
    "attvaluesOrSpellsOrNodes"
})
public class XMLNodeContent {

    @XmlElements({
        @XmlElement(name = "size", type = SizeContent.class),
        @XmlElement(name = "position", type = PositionContent.class),
        @XmlElement(name = "edges", type = XMLEdgesContent.class),
        @XmlElement(name = "color", type = ColorContent.class),
        @XmlElement(name = "attvalues", type = XMLAttvaluesContent.class),
        @XmlElement(name = "parents", type = XMLParentsContent.class),
        @XmlElement(name = "shape", type = NodeShapeContent.class),
        @XmlElement(name = "nodes", type = XMLNodesContent.class),
        @XmlElement(name = "spells", type = XMLSpellsContent.class)
    })
    private List<Object> attvaluesOrSpellsOrNodes;
    @XmlAttribute
    private String start;
    @XmlAttribute
    private String startopen;
    @XmlAttribute
    private String end;
    @XmlAttribute
    private String endopen;
    @XmlAttribute
    private String pid;
    @XmlAttribute(required = true)
    private String id;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    private String label;

    /**
     * Gets the value of the attvaluesOrSpellsOrNodes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the attvaluesOrSpellsOrNodes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAttvaluesOrSpellsOrNodes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SizeContent }
     * {@link PositionContent }
     * {@link XMLEdgesContent }
     * {@link ColorContent }
     * {@link XMLAttvaluesContent }
     * {@link XMLParentsContent }
     * {@link NodeShapeContent }
     * {@link XMLNodesContent }
     * {@link XMLSpellsContent }
     * 
     * 
     */
    public List<Object> getAttvaluesOrSpellsOrNodes() {
        if (attvaluesOrSpellsOrNodes == null) {
            attvaluesOrSpellsOrNodes = new ArrayList<>();
        }
        return this.attvaluesOrSpellsOrNodes;
    }

    /**
     * Gets the value of the start property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStart() {
        return start;
    }

    /**
     * Sets the value of the start property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStart(String value) {
        this.start = value;
    }

    /**
     * Gets the value of the startopen property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStartopen() {
        return startopen;
    }

    /**
     * Sets the value of the startopen property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStartopen(String value) {
        this.startopen = value;
    }

    /**
     * Gets the value of the end property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEnd() {
        return end;
    }

    /**
     * Sets the value of the end property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEnd(String value) {
        this.end = value;
    }

    /**
     * Gets the value of the endopen property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEndopen() {
        return endopen;
    }

    /**
     * Sets the value of the endopen property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEndopen(String value) {
        this.endopen = value;
    }

    /**
     * Gets the value of the pid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPid() {
        return pid;
    }

    /**
     * Sets the value of the pid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPid(String value) {
        this.pid = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the label property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLabel(String value) {
        this.label = value;
    }

}
