/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package org.ala.model;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Simple POJO representing an image within the system. 
 * FIXME We *should* be generating thumbnails at repository load
 * time. Hence the thumbnail property should be populated.
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
@JsonIgnoreProperties({"thumbnail", "dcLocation"})
public class Image extends AttributableObject implements Comparable<Image>{

	/** The guid of this image, typically the URL from whence it came */
	protected String guid;
	/** The content type of this image */
	protected String contentType;
	/** The location in the repository */
	protected String repoLocation;
	/** The location in the repository */
	protected String dcLocation;
	/** The location in the repository */
	protected String thumbnail;
    /** The title of the resource contributing the property */
    protected String title;
    /** The title of the identifier (URI) contributing the property */
    protected String identifier;
    /** The creator of this image - for attribution */
    protected String creator;
    /** The locality this image was taken at as a free text string */
    protected String locality;
    /** The locality this image was taken at as a free text string */
    protected String isPartOf;
    /** The licence */
    protected String licence;
    /** The rights */
    protected String rights;


    /**
     * Compare to method
     *
     * @param o
     * @return
     */
    @Override
	public int compareTo(Image o) {
		//check the infosources
		if(o.getRepoLocation()!=null && repoLocation!=null){
			return o.getRepoLocation().compareTo(repoLocation);
		}
		return -1;
	}

    /**
	 * @return the guid
	 */
	public String getGuid() {
		return guid;
	}
	/**
	 * @param guid the guid to set
	 */
	public void setGuid(String guid) {
		this.guid = guid;
	}
	/**
	 * @return the repoLocation
	 */
	public String getRepoLocation() {
		return repoLocation;
	}
	/**
	 * @param repoLocation the repoLocation to set
	 */
	public void setRepoLocation(String repoLocation) {
		this.repoLocation = repoLocation;
	}
	/**
	 * @return the contentType
	 */
	public String getContentType() {
		return contentType;
	}
	/**
	 * @param contentType the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
    /**
     * Get the value of title
     *
     * @return the value of title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the value of title
     *
     * @param title new value of title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get the value of identifier
     *
     * @return the value of identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the value of identifier
     *
     * @param identifier new value of identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

	/**
	 * @return the dcLocation
	 */
	public String getDcLocation() {
		return dcLocation;
	}

	/**
	 * @param dcLocation the dcLocation to set
	 */
	public void setDcLocation(String dcLocation) {
		this.dcLocation = dcLocation;
	}

	/**
	 * @return the thumbnail
	 */
	public String getThumbnail() {
		return thumbnail;
	}

	/**
	 * @param thumbnail the thumbnail to set
	 */
	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	/**
	 * @return the creator
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * @param creator the creator to set
	 */
	public void setCreator(String creator) {
		this.creator = creator;
	}

	/**
	 * @return the locality
	 */
	public String getLocality() {
		return locality;
	}

	/**
	 * @param locality the locality to set
	 */
	public void setLocality(String locality) {
		this.locality = locality;
	}
	
	/**
	 * @return the isPartOf
	 */
	public String getIsPartOf() {
		return isPartOf;
	}

	/**
	 * @param isPartOf the isPartOf to set
	 */
	public void setIsPartOf(String isPartOf) {
		this.isPartOf = isPartOf;
	}
	

	/**
	 * @return the licence
	 */
	public String getLicence() {
		return licence;
	}

	/**
	 * @param licence the licence to set
	 */
	public void setLicence(String licence) {
		this.licence = licence;
	}

	/**
	 * @return the rights
	 */
	public String getRights() {
		return rights;
	}

	/**
	 * @param rights the rights to set
	 */
	public void setRights(String rights) {
		this.rights = rights;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Image [contentType=");
		builder.append(this.contentType);
		builder.append(", guid=");
		builder.append(this.guid);
		builder.append(", identifier=");
		builder.append(this.identifier);
		builder.append(", repoLocation=");
		builder.append(this.repoLocation);
		builder.append(", title=");
		builder.append(this.title);
		builder.append(", documentId=");
		builder.append(this.documentId);
		builder.append(", infoSourceId=");
		builder.append(this.infoSourceId);
		builder.append(", infoSourceName=");
		builder.append(this.infoSourceName);
		builder.append(", infoSourceURL=");
		builder.append(this.infoSourceURL);
		builder.append("]");
		return builder.toString();
	}
}