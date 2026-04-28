package com.redline.jj.batch.dto;

import com.redline.jj.domain.model.Model;
import com.redline.jj.domain.site.Site;

public record ResolvedItem(
    Model model,
    Site site,
    String optionLabel,
    Integer price,
    boolean inStock,
    String url,
    String siteModelName
) {}
