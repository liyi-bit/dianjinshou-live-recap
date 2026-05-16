package com.dianjinshou.modules.storage.vo;

public class CloudReadonlyDetailVO {

    private CloudFileVO file;
    private SignedUrlVO signedUrl;
    private Object recapDetail;
    private Object comparisonDetail;
    private Boolean readonly;
    private Boolean allowDownload;
    private Boolean allowDownloadToLocal;

    public CloudFileVO getFile() {
        return file;
    }

    public void setFile(CloudFileVO file) {
        this.file = file;
    }

    public SignedUrlVO getSignedUrl() {
        return signedUrl;
    }

    public void setSignedUrl(SignedUrlVO signedUrl) {
        this.signedUrl = signedUrl;
    }

    public Object getRecapDetail() {
        return recapDetail;
    }

    public void setRecapDetail(Object recapDetail) {
        this.recapDetail = recapDetail;
    }

    public Object getComparisonDetail() {
        return comparisonDetail;
    }

    public void setComparisonDetail(Object comparisonDetail) {
        this.comparisonDetail = comparisonDetail;
    }

    public Boolean getReadonly() {
        return readonly;
    }

    public void setReadonly(Boolean readonly) {
        this.readonly = readonly;
    }

    public Boolean getAllowDownload() {
        return allowDownload;
    }

    public void setAllowDownload(Boolean allowDownload) {
        this.allowDownload = allowDownload;
    }

    public Boolean getAllowDownloadToLocal() {
        return allowDownloadToLocal;
    }

    public void setAllowDownloadToLocal(Boolean allowDownloadToLocal) {
        this.allowDownloadToLocal = allowDownloadToLocal;
    }
}
