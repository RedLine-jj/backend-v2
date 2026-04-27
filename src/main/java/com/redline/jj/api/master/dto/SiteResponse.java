package com.redline.jj.api.master.dto;

import com.redline.jj.domain.site.Site;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class SiteResponse implements Serializable {

    private final Long id;
    private final String siteName;
    private final String siteLink;

    private SiteResponse(Long id, String siteName, String siteLink) {
        this.id = id;
        this.siteName = siteName;
        this.siteLink = siteLink;
    }

    public static SiteResponse from(Site site) {
        return new SiteResponse(site.getId(), site.getSiteName(), site.getSiteLink());
    }
}
