package cn.keking.config;

import cn.keking.utils.ConfigUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author: chenjh
 * @since: 2019/4/10 17:22
 */
@Component(value = ConfigConstants.BEAN_NAME)
public class ConfigConstants {
    public static final String BEAN_NAME = "configConstants";

    static {
        //pdfbox兼容低版本jdk
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    }

    private static Boolean cacheEnabled;
    private static String[] simTexts = {};
    private static String[] medias = {};
    private static String[] convertMedias = {};
    private static String mediaConvertDisable;
    private static String officePreviewType;
    private static String officePreviewSwitchDisabled;
    private static String ftpUsername;
    private static String ftpPassword;
    private static String ftpControlEncoding;
    private static String baseUrl;
    private static String fileDir = ConfigUtils.getHomePath() + File.separator + "file" + File.separator;
    private static String localPreviewDir;
    private static CopyOnWriteArraySet<String> trustHostSet;
    private static CopyOnWriteArraySet<String> notTrustHostSet;
    private static String pdfPresentationModeDisable;
    private static String pdfDisableEditing;
    private static String pdfOpenFileDisable;
    private static String pdfPrintDisable;
    private static String pdfDownloadDisable;
    private static String pdfBookmarkDisable;
    private static Boolean fileUploadDisable;
    private static String tifPreviewType;
    private static String beian;
    private static String[] prohibit = {};
    private static String size;
    private static String password;
    private static int pdf2JpgDpi;
    private static String officeTypeWeb;
    private static String cadPreviewType;
    private static Boolean deleteSourceFile;
    private static Boolean deleteCaptcha;
    private static String officePageRange;
    private static String officeWatermark;
    private static String officeQuality;
    private static String officeMaxImageResolution;
    private static Boolean officeExportBookmarks;
    private static Boolean officeExportNotes;
    private static Boolean officeDocumentOpenPasswords;
    private static String cadTimeout;
    private static int cadThread;
    private static String homePageNumber;
    private static String homePagination;
    private static String homePageSize;
    private static String homeSearch;
    private static int pdfTimeout;
    private static int pdfTimeout80;
    private static int pdfTimeout200;
    private static int pdfThread;

    // 用户行为监控配置
    private static Boolean behaviorMonitorEnabled;
    private static Integer behaviorMonitorTimeWindow;
    private static Integer behaviorMonitorMaxRequests;
    private static Integer behaviorMonitorDailyMaxRequests;
    private static String behaviorMonitorEmailHost;
    private static Integer behaviorMonitorEmailPort;
    private static String behaviorMonitorEmailUsername;
    private static String behaviorMonitorEmailPassword;
    private static String behaviorMonitorEmailFrom;
    private static String behaviorMonitorEmailTo;
    private static String behaviorMonitorEmailSubject;
    private static String behaviorMonitorEmailContent;

    public static final String DEFAULT_CACHE_ENABLED = "true";
    public static final String DEFAULT_TXT_TYPE = "txt,html,htm,asp,jsp,xml,json,properties,md,gitignore,log,java,py,c,cpp,sql,sh,bat,m,bas,prg,cmd,xbrl";
    public static final String DEFAULT_MEDIA_TYPE = "mp3,wav,mp4,flv";
    public static final String DEFAULT_OFFICE_PREVIEW_TYPE = "image";
    public static final String DEFAULT_OFFICE_PREVIEW_SWITCH_DISABLED = "false";
    public static final String DEFAULT_FTP_USERNAME = null;
    public static final String DEFAULT_FTP_PASSWORD = null;
    public static final String DEFAULT_FTP_CONTROL_ENCODING = "UTF-8";
    public static final String DEFAULT_VALUE = "default";
    public static final String DEFAULT_PDF_PRESENTATION_MODE_DISABLE = "true";
    public static final String DEFAULT_PDF_OPEN_FILE_DISABLE = "true";
    public static final String DEFAULT_PDF_PRINT_DISABLE = "true";
    public static final String DEFAULT_PDF_DOWNLOAD_DISABLE = "true";
    public static final String DEFAULT_PDF_BOOKMARK_DISABLE = "true";
    public static final String DEFAULT_PDF_DISABLE_EDITING = "true";
    public static final String DEFAULT_FILE_UPLOAD_DISABLE = "false";
    public static final String DEFAULT_TIF_PREVIEW_TYPE = "tif";
    public static final String DEFAULT_CAD_PREVIEW_TYPE = "pdf";
    public static final String DEFAULT_BEIAN = "无";
    public static final String DEFAULT_SIZE = "500MB";
    public static final String DEFAULT_PROHIBIT = "exe,dll";
    public static final String DEFAULT_PASSWORD = "123456";
    public static final String DEFAULT_PDF2_JPG_DPI = "105";
    public static final String DEFAULT_OFFICE_TYPE_WEB = "web";
    public static final String DEFAULT_DELETE_SOURCE_FILE = "true";
    public static final String DEFAULT_DELETE_CAPTCHA = "false";
    public static final String DEFAULT_CAD_TIMEOUT = "90";
    public static final String DEFAULT_CAD_THREAD = "5";
    public static final String DEFAULT_OFFICE_PAQERANQE = "false";
    public static final String DEFAULT_OFFICE_WATERMARK = "false";
    public static final String DEFAULT_OFFICE_QUALITY = "80";
    public static final String DEFAULT_OFFICE_MAXIMAQERESOLUTION = "150";
    public static final String DEFAULT_OFFICE_EXPORTBOOKMARKS = "true";
    public static final String DEFAULT_OFFICE_EXPORTNOTES = "true";
    public static final String DEFAULT_OFFICE_EOCUMENTOPENPASSWORDS = "true";
    public static final String DEFAULT_HOME_PAGENUMBER = "1";
    public static final String DEFAULT_HOME_PAGINATION = "true";
    public static final String DEFAULT_HOME_PAGSIZE = "15";
    public static final String DEFAULT_HOME_SEARCH = "true";
    public static final String DEFAULT_PDF_TIMEOUT = "90";
    public static final String DEFAULT_PDF_TIMEOUT80 = "180";
    public static final String DEFAULT_PDF_TIMEOUT200 = "300";
    public static final String DEFAULT_PDF_THREAD = "5";

    // 用户行为监控默认配置
    public static final String DEFAULT_BEHAVIOR_MONITOR_ENABLED = "false";
    public static final String DEFAULT_BEHAVIOR_MONITOR_TIME_WINDOW = "10";
    public static final String DEFAULT_BEHAVIOR_MONITOR_MAX_REQUESTS = "50";
    public static final String DEFAULT_BEHAVIOR_MONITOR_DAILY_MAX_REQUESTS = "500";
    public static final String DEFAULT_BEHAVIOR_MONITOR_EMAIL_HOST = "smtp.qq.com";
    public static final String DEFAULT_BEHAVIOR_MONITOR_EMAIL_PORT = "587";
    public static final String DEFAULT_BEHAVIOR_MONITOR_EMAIL_USERNAME = "";
    public static final String DEFAULT_BEHAVIOR_MONITOR_EMAIL_PASSWORD = "";
    public static final String DEFAULT_BEHAVIOR_MONITOR_EMAIL_FROM = "";
    public static final String DEFAULT_BEHAVIOR_MONITOR_EMAIL_TO = "";
    public static final String DEFAULT_BEHAVIOR_MONITOR_EMAIL_SUBJECT = "用户行为异常";
    public static final String DEFAULT_BEHAVIOR_MONITOR_EMAIL_CONTENT = "IP地址%s在过去%d分钟内访问了%d次系统，超出正常范围，请保持关注。";

    public static Boolean isCacheEnabled() {
        return cacheEnabled;
    }

    @Value("${cache.enabled:true}")
    public void setCacheEnabled(String cacheEnabled) {
        setCacheEnabledValueValue(Boolean.parseBoolean(cacheEnabled));
    }

    public static void setCacheEnabledValueValue(Boolean cacheEnabled) {
        ConfigConstants.cacheEnabled = cacheEnabled;
    }

    public static String[] getSimText() {
        return simTexts;
    }

    @Value("${simText:txt,html,htm,asp,jsp,xml,json,properties,md,gitignore,log,java,py,c,cpp,sql,sh,bat,m,bas,prg,cmd,xbrl}")
    public void setSimText(String simText) {
        String[] simTextArr = simText.split(",");
        setSimTextValue(simTextArr);
    }

    public static void setSimTextValue(String[] simText) {
        ConfigConstants.simTexts = simText;
    }

    public static String[] getMedia() {
        return medias;
    }

    @Value("${media:mp3,wav,mp4,flv}")
    public void setMedia(String media) {
        String[] mediaArr = media.split(",");
        setMediaValue(mediaArr);
    }

    public static void setMediaValue(String[] Media) {
        ConfigConstants.medias = Media;
    }

    public static String[] getConvertMedias() {
        return convertMedias;
    }

    @Value("${convertMedias:avi,mov,wmv,mkv,3gp,rm}")
    public void setConvertMedias(String convertMedia) {
        String[] mediaArr = convertMedia.split(",");
        setConvertMediaValue(mediaArr);
    }

    public static void setConvertMediaValue(String[] ConvertMedia) {
        ConfigConstants.convertMedias = ConvertMedia;
    }

    public static String getMediaConvertDisable() {
        return mediaConvertDisable;
    }


    @Value("${media.convert.disable:true}")
    public void setMediaConvertDisable(String mediaConvertDisable) {
        setMediaConvertDisableValue(mediaConvertDisable);
    }

    public static void setMediaConvertDisableValue(String mediaConvertDisable) {
        ConfigConstants.mediaConvertDisable = mediaConvertDisable;
    }

    public static String getOfficePreviewType() {
        return officePreviewType;
    }

    @Value("${office.preview.type:image}")
    public void setOfficePreviewType(String officePreviewType) {
        setOfficePreviewTypeValue(officePreviewType);
    }

    public static void setOfficePreviewTypeValue(String officePreviewType) {
        ConfigConstants.officePreviewType = officePreviewType;
    }

    public static String getFtpUsername() {
        return ftpUsername;
    }

    @Value("${ftp.username:}")
    public void setFtpUsername(String ftpUsername) {
        setFtpUsernameValue(ftpUsername);
    }

    public static void setFtpUsernameValue(String ftpUsername) {
        ConfigConstants.ftpUsername = ftpUsername;
    }

    public static String getFtpPassword() {
        return ftpPassword;
    }

    @Value("${ftp.password:}")
    public void setFtpPassword(String ftpPassword) {
        setFtpPasswordValue(ftpPassword);
    }

    public static void setFtpPasswordValue(String ftpPassword) {
        ConfigConstants.ftpPassword = ftpPassword;
    }

    public static String getFtpControlEncoding() {
        return ftpControlEncoding;
    }

    @Value("${ftp.control.encoding:UTF-8}")
    public void setFtpControlEncoding(String ftpControlEncoding) {
        setFtpControlEncodingValue(ftpControlEncoding);
    }

    public static void setFtpControlEncodingValue(String ftpControlEncoding) {
        ConfigConstants.ftpControlEncoding = ftpControlEncoding;
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    @Value("${base.url:default}")
    public void setBaseUrl(String baseUrl) {
        setBaseUrlValue(baseUrl);
    }

    public static void setBaseUrlValue(String baseUrl) {
        ConfigConstants.baseUrl = baseUrl;
    }

    public static String getFileDir() {
        return fileDir;
    }

    @Value("${file.dir:default}")
    public void setFileDir(String fileDir) {
        setFileDirValue(fileDir);
    }

    public static void setFileDirValue(String fileDir) {
        if (!DEFAULT_VALUE.equalsIgnoreCase(fileDir)) {
            if (!fileDir.endsWith(File.separator)) {
                fileDir = fileDir + File.separator;
            }
            ConfigConstants.fileDir = fileDir;
        }
    }

    public static String getLocalPreviewDir() {
        return localPreviewDir;
    }

    @Value("${local.preview.dir:default}")
    public void setLocalPreviewDir(String localPreviewDir) {
        setLocalPreviewDirValue(localPreviewDir);
    }

    public static void setLocalPreviewDirValue(String localPreviewDir) {
        if (!DEFAULT_VALUE.equals(localPreviewDir)) {
            if (!localPreviewDir.endsWith(File.separator)) {
                localPreviewDir = localPreviewDir + File.separator;
            }
        }
        ConfigConstants.localPreviewDir = localPreviewDir;
    }

    @Value("${trust.host:default}")
    public void setTrustHost(String trustHost) {
        setTrustHostSet(getHostValue(trustHost));
    }

    public static void setTrustHostValue(String trustHost){
        setTrustHostSet(getHostValue(trustHost));
    }

    @Value("${not.trust.host:default}")
    public void setNotTrustHost(String notTrustHost) {
        setNotTrustHostSet(getHostValue(notTrustHost));
    }

    public static void setNotTrustHostValue(String notTrustHost){
        setNotTrustHostSet(getHostValue(notTrustHost));
    }

    private static CopyOnWriteArraySet<String> getHostValue(String trustHost) {
        if (DEFAULT_VALUE.equalsIgnoreCase(trustHost)) {
            return new CopyOnWriteArraySet<>();
        } else {
            // 去除空格并转小写
            String[] trustHostArray = trustHost.toLowerCase().replaceAll("\\s+", "").split(",");
            return new CopyOnWriteArraySet<>(Arrays.asList(trustHostArray));
        }
    }

    public static Set<String> getTrustHostSet() {
        return trustHostSet;
    }

    private static void setTrustHostSet(CopyOnWriteArraySet<String> trustHostSet) {
        ConfigConstants.trustHostSet = trustHostSet;
    }

    public static Set<String> getNotTrustHostSet() {
        return notTrustHostSet;
    }

    public static void setNotTrustHostSet(CopyOnWriteArraySet<String> notTrustHostSet) {
        ConfigConstants.notTrustHostSet = notTrustHostSet;
    }


    public static String getPdfPresentationModeDisable() {
        return pdfPresentationModeDisable;
    }

    @Value("${pdf.presentationMode.disable:true}")
    public void setPdfPresentationModeDisable(String pdfPresentationModeDisable) {
        setPdfPresentationModeDisableValue(pdfPresentationModeDisable);
    }

    public static void setPdfPresentationModeDisableValue(String pdfPresentationModeDisable) {
        ConfigConstants.pdfPresentationModeDisable = pdfPresentationModeDisable;
    }

    public static String getPdfOpenFileDisable() {
        return pdfOpenFileDisable;
    }

    @Value("${pdf.openFile.disable:true}")
    public void setPdfOpenFileDisable(String pdfOpenFileDisable) {
        setPdfOpenFileDisableValue(pdfOpenFileDisable);
    }

    public static void setPdfOpenFileDisableValue(String pdfOpenFileDisable) {
        ConfigConstants.pdfOpenFileDisable = pdfOpenFileDisable;
    }

    public static String getPdfPrintDisable() {
        return pdfPrintDisable;
    }

    @Value("${pdf.print.disable:true}")
    public void setPdfPrintDisable(String pdfPrintDisable) {
        setPdfPrintDisableValue(pdfPrintDisable);
    }

    public static void setPdfPrintDisableValue(String pdfPrintDisable) {
        ConfigConstants.pdfPrintDisable = pdfPrintDisable;
    }

    public static String getPdfDownloadDisable() {
        return pdfDownloadDisable;
    }

    @Value("${pdf.download.disable:true}")
    public void setPdfDownloadDisable(String pdfDownloadDisable) {
        setPdfDownloadDisableValue(pdfDownloadDisable);
    }

    public static void setPdfDownloadDisableValue(String pdfDownloadDisable) {
        ConfigConstants.pdfDownloadDisable = pdfDownloadDisable;
    }

    public static String getPdfBookmarkDisable() {
        return pdfBookmarkDisable;
    }

    @Value("${pdf.bookmark.disable:true}")
    public void setPdfBookmarkDisable(String pdfBookmarkDisable) {
        setPdfBookmarkDisableValue(pdfBookmarkDisable);
    }

    public static void setPdfBookmarkDisableValue(String pdfBookmarkDisable) {
        ConfigConstants.pdfBookmarkDisable = pdfBookmarkDisable;
    }


    public static String getPdfDisableEditing() {
        return pdfDisableEditing;
    }

    @Value("${pdf.disable.editing:true}")
    public void setpdfDisableEditing(String pdfDisableEditing) {
        setPdfDisableEditingValue(pdfDisableEditing);
    }

    public static void setPdfDisableEditingValue(String pdfDisableEditing) {
        ConfigConstants.pdfDisableEditing = pdfDisableEditing;
    }

    public static String getOfficePreviewSwitchDisabled() {
        return officePreviewSwitchDisabled;
    }

    @Value("${office.preview.switch.disabled:true}")
    public void setOfficePreviewSwitchDisabled(String officePreviewSwitchDisabled) {
        ConfigConstants.officePreviewSwitchDisabled = officePreviewSwitchDisabled;
    }

    public static void setOfficePreviewSwitchDisabledValue(String officePreviewSwitchDisabled) {
        ConfigConstants.officePreviewSwitchDisabled = officePreviewSwitchDisabled;
    }

    public static Boolean getFileUploadDisable() {
        return fileUploadDisable;
    }

    @Value("${file.upload.disable:true}")
    public void setFileUploadDisable(Boolean fileUploadDisable) {
        setFileUploadDisableValue(fileUploadDisable);
    }

    public static void setFileUploadDisableValue(Boolean fileUploadDisable) {
        ConfigConstants.fileUploadDisable = fileUploadDisable;
    }


    public static String getTifPreviewType() {
        return tifPreviewType;
    }

    @Value("${tif.preview.type:tif}")
    public void setTifPreviewType(String tifPreviewType) {
        setTifPreviewTypeValue(tifPreviewType);
    }

    public static void setTifPreviewTypeValue(String tifPreviewType) {
        ConfigConstants.tifPreviewType = tifPreviewType;
    }

    public static String[] getProhibit() {
        return prohibit;
    }

    @Value("${prohibit:exe,dll}")
    public void setProhibit(String prohibit) {
        String[] prohibitArr = prohibit.split(",");
        setProhibitValue(prohibitArr);
    }

    public static void setProhibitValue(String[] prohibit) {
        ConfigConstants.prohibit = prohibit;
    }

    public static String maxSize() {
        return size;
    }

    @Value("${spring.servlet.multipart.max-file-size:500MB}")
    public void setSize(String size) {
        setSizeValue(size);
    }

    public static void setSizeValue(String size) {
        ConfigConstants.size = size;
    }

    public static String getPassword() {
        return password;
    }

    @Value("${delete.password:123456}")
    public void setPassword(String password) {
        setPasswordValue(password);
    }

    public static void setPasswordValue(String password) {
        ConfigConstants.password = password;
    }


    public static int getPdf2JpgDpi() {
        return pdf2JpgDpi;
    }

    @Value("${pdf2jpg.dpi:105}")
    public void pdf2JpgDpi(int pdf2JpgDpi) {
        setPdf2JpgDpiValue(pdf2JpgDpi);
    }

    public static void setPdf2JpgDpiValue(int pdf2JpgDpi) {
        ConfigConstants.pdf2JpgDpi = pdf2JpgDpi;
    }

    public static String getOfficeTypeWeb() {
        return officeTypeWeb;
    }

    @Value("${office.type.web:web}")
    public void setOfficeTypeWeb(String officeTypeWeb) {
        setOfficeTypeWebValue(officeTypeWeb);
    }

    public static void setOfficeTypeWebValue(String officeTypeWeb) {
        ConfigConstants.officeTypeWeb = officeTypeWeb;
    }


    public static Boolean getDeleteSourceFile() {
        return deleteSourceFile;
    }

    @Value("${delete.source.file:true}")
    public void setDeleteSourceFile(Boolean deleteSourceFile) {
        setDeleteSourceFileValue(deleteSourceFile);
    }

    public static void setDeleteSourceFileValue(Boolean deleteSourceFile) {
        ConfigConstants.deleteSourceFile = deleteSourceFile;
    }

    public static Boolean getDeleteCaptcha() {
        return deleteCaptcha;
    }

    @Value("${delete.captcha:false}")
    public void setDeleteCaptcha(Boolean deleteCaptcha) {
        setDeleteCaptchaValue(deleteCaptcha);
    }

    public static void setDeleteCaptchaValue(Boolean deleteCaptcha) {
        ConfigConstants.deleteCaptcha = deleteCaptcha;
    }

    /**
     * 以下为cad转换模块设置
     */

    public static String getCadPreviewType() {
        return cadPreviewType;
    }

    @Value("${cad.preview.type:svg}")
    public void setCadPreviewType(String cadPreviewType) {
        setCadPreviewTypeValue(cadPreviewType);
    }

    public static void setCadPreviewTypeValue(String cadPreviewType) {
        ConfigConstants.cadPreviewType = cadPreviewType;
    }


    public static String getCadTimeout() {
        return cadTimeout;
    }

    @Value("${cad.timeout:90}")
    public void setCadTimeout(String cadTimeout) {
        setCadTimeoutValue(cadTimeout);
    }

    public static void setCadTimeoutValue(String cadTimeout) {
        ConfigConstants.cadTimeout = cadTimeout;
    }


    public static int getCadThread() {
        return cadThread;
    }

    @Value("${cad.thread:5}")
    public void setCadThread(int cadThread) {
        setCadThreadValue(cadThread);
    }

    public static void setCadThreadValue(int cadThread) {
        ConfigConstants.cadThread = cadThread;
    }

    /**
     * 以下为pdf转换模块设置
     */
    public static int getPdfTimeout() {
        return pdfTimeout;
    }

    @Value("${pdf.timeout:90}")
    public void setPdfTimeout(int pdfTimeout) {
        setPdfTimeoutValue(pdfTimeout);
    }

    public static void setPdfTimeoutValue(int pdfTimeout) {
        ConfigConstants.pdfTimeout = pdfTimeout;
    }


    public static int getPdfTimeout80() {
        return pdfTimeout80;
    }

    @Value("${pdf.timeout80:180}")
    public void setPdfTimeout80(int pdfTimeout80) {
        setPdfTimeout80Value(pdfTimeout80);
    }

    public static void setPdfTimeout80Value(int pdfTimeout80) {
        ConfigConstants.pdfTimeout80 = pdfTimeout80;
    }



    public static int getPdfTimeout200() {
        return pdfTimeout200;
    }

    @Value("${pdf.timeout200:300}")
    public void setPdfTimeout200(int pdfTimeout200) {
        setPdfTimeout200Value(pdfTimeout200);
    }

    public static void setPdfTimeout200Value(int pdfTimeout200) {
        ConfigConstants.pdfTimeout200 = pdfTimeout200;
    }


    public static int getPdfThread() {
        return pdfThread;
    }

    @Value("${pdf.thread:5}")
    public void setPdfThread(int pdfThread) {
        setPdfThreadValue(pdfThread);
    }

    public static void setPdfThreadValue(int pdfThread) {
        ConfigConstants.pdfThread = pdfThread;
    }

    /**
     * 以下为OFFICE转换模块设置
     */

    public static String getOfficePageRange() {
        return officePageRange;
    }

    @Value("${office.pagerange:false}")
    public void setOfficePageRange(String officePageRange) {
        setOfficePageRangeValue(officePageRange);
    }

    public static void setOfficePageRangeValue(String officePageRange) {
        ConfigConstants.officePageRange = officePageRange;
    }

    public static String getOfficeWatermark() {
        return officeWatermark;
    }

    @Value("${office.watermark:false}")
    public void setOfficeWatermark(String officeWatermark) {
        setOfficeWatermarkValue(officeWatermark);
    }

    public static void setOfficeWatermarkValue(String officeWatermark) {
        ConfigConstants.officeWatermark = officeWatermark;
    }

    public static String getOfficeQuality() {
        return officeQuality;
    }

    @Value("${office.quality:80}")
    public void setOfficeQuality(String officeQuality) {
        setOfficeQualityValue(officeQuality);
    }

    public static void setOfficeQualityValue(String officeQuality) {
        ConfigConstants.officeQuality = officeQuality;
    }

    public static String getOfficeMaxImageResolution() {
        return officeMaxImageResolution;
    }

    @Value("${office.maximageresolution:150}")
    public void setOfficeMaxImageResolution(String officeMaxImageResolution) {
        setOfficeMaxImageResolutionValue(officeMaxImageResolution);
    }

    public static void setOfficeMaxImageResolutionValue(String officeMaxImageResolution) {
        ConfigConstants.officeMaxImageResolution = officeMaxImageResolution;
    }

    public static Boolean getOfficeExportBookmarks() {
        return officeExportBookmarks;
    }

    @Value("${office.exportbookmarks:true}")
    public void setOfficeExportBookmarks(Boolean officeExportBookmarks) {
        setOfficeExportBookmarksValue(officeExportBookmarks);
    }

    public static void setOfficeExportBookmarksValue(Boolean officeExportBookmarks) {
        ConfigConstants.officeExportBookmarks = officeExportBookmarks;
    }

    public static Boolean getOfficeExportNotes() {
        return officeExportNotes;
    }

    @Value("${office.exportnotes:true}")
    public void setExportNotes(Boolean officeExportNotes) {
        setOfficeExportNotesValue(officeExportNotes);
    }

    public static void setOfficeExportNotesValue(Boolean officeExportNotes) {
        ConfigConstants.officeExportNotes = officeExportNotes;
    }

    public static Boolean getOfficeDocumentOpenPasswords() {
        return officeDocumentOpenPasswords;
    }

    @Value("${office.documentopenpasswords:true}")
    public void setDocumentOpenPasswords(Boolean officeDocumentOpenPasswords) {
        setOfficeDocumentOpenPasswordsValue(officeDocumentOpenPasswords);
    }

    public static void setOfficeDocumentOpenPasswordsValue(Boolean officeDocumentOpenPasswords) {
        ConfigConstants.officeDocumentOpenPasswords = officeDocumentOpenPasswords;
    }

    /**
     * 以下为首页显示
     */

    public static String getBeian() {
        return beian;
    }

    @Value("${beian:default}")
    public void setBeian(String beian) {
        setBeianValue(beian);
    }

    public static void setBeianValue(String beian) {
        ConfigConstants.beian = beian;
    }


    public static String getHomePageNumber() {
        return homePageNumber;
    }

    @Value("${home.pagenumber:1}")
    public void setHomePageNumber(String homePageNumber) {
        setHomePageNumberValue(homePageNumber);
    }

    public static void setHomePageNumberValue(String homePageNumber) {
        ConfigConstants.homePageNumber = homePageNumber;
    }

    public static String getHomePagination() {
        return homePagination;
    }

    @Value("${home.pagination:true}")
    public void setHomePagination(String homePagination) {
        setHomePaginationValue(homePagination);
    }

    public static void setHomePaginationValue(String homePagination) {
        ConfigConstants.homePagination = homePagination;
    }

    public static String getHomePageSize() {
        return homePageSize;
    }

    @Value("${home.pagesize:15}")
    public void setHomePageSize(String homePageSize) {
        setHomePageSizeValue(homePageSize);
    }

    public static void setHomePageSizeValue(String homePageSize) {
        ConfigConstants.homePageSize = homePageSize;
    }

    public static String getHomeSearch() {
        return homeSearch;
    }

    @Value("${home.search:1}")
    public void setHomeSearch(String homeSearch) {
        setHomeSearchValue(homeSearch);
    }

    public static void setHomeSearchValue(String homeSearch) {
        ConfigConstants.homeSearch = homeSearch;
    }

    // ==================== 用户行为监控配置 ====================

    public static Boolean isBehaviorMonitorEnabled() {
        return behaviorMonitorEnabled;
    }

    @Value("${behavior.monitor.enabled:false}")
    public void setBehaviorMonitorEnabled(String behaviorMonitorEnabled) {
        setBehaviorMonitorEnabledValue(Boolean.parseBoolean(behaviorMonitorEnabled));
    }

    public static void setBehaviorMonitorEnabledValue(Boolean behaviorMonitorEnabled) {
        ConfigConstants.behaviorMonitorEnabled = behaviorMonitorEnabled;
    }

    public static Integer getBehaviorMonitorTimeWindow() {
        return behaviorMonitorTimeWindow;
    }

    @Value("${behavior.monitor.time.window:10}")
    public void setBehaviorMonitorTimeWindow(String behaviorMonitorTimeWindow) {
        setBehaviorMonitorTimeWindowValue(Integer.parseInt(behaviorMonitorTimeWindow));
    }

    public static void setBehaviorMonitorTimeWindowValue(Integer behaviorMonitorTimeWindow) {
        ConfigConstants.behaviorMonitorTimeWindow = behaviorMonitorTimeWindow;
    }

    public static Integer getBehaviorMonitorMaxRequests() {
        return behaviorMonitorMaxRequests;
    }

    @Value("${behavior.monitor.max.requests:50}")
    public void setBehaviorMonitorMaxRequests(String behaviorMonitorMaxRequests) {
        setBehaviorMonitorMaxRequestsValue(Integer.parseInt(behaviorMonitorMaxRequests));
    }

    public static void setBehaviorMonitorMaxRequestsValue(Integer behaviorMonitorMaxRequests) {
        ConfigConstants.behaviorMonitorMaxRequests = behaviorMonitorMaxRequests;
    }

    public static Integer getBehaviorMonitorDailyMaxRequests() {
        return behaviorMonitorDailyMaxRequests;
    }

    @Value("${behavior.monitor.daily.max.requests:500}")
    public void setBehaviorMonitorDailyMaxRequests(String behaviorMonitorDailyMaxRequests) {
        setBehaviorMonitorDailyMaxRequestsValue(Integer.parseInt(behaviorMonitorDailyMaxRequests));
    }

    public static void setBehaviorMonitorDailyMaxRequestsValue(Integer behaviorMonitorDailyMaxRequests) {
        ConfigConstants.behaviorMonitorDailyMaxRequests = behaviorMonitorDailyMaxRequests;
    }

    public static String getBehaviorMonitorEmailHost() {
        return behaviorMonitorEmailHost;
    }

    @Value("${behavior.monitor.email.host:smtp.qq.com}")
    public void setBehaviorMonitorEmailHost(String behaviorMonitorEmailHost) {
        setBehaviorMonitorEmailHostValue(behaviorMonitorEmailHost);
    }

    public static void setBehaviorMonitorEmailHostValue(String behaviorMonitorEmailHost) {
        ConfigConstants.behaviorMonitorEmailHost = behaviorMonitorEmailHost;
    }

    public static Integer getBehaviorMonitorEmailPort() {
        return behaviorMonitorEmailPort;
    }

    @Value("${behavior.monitor.email.port:587}")
    public void setBehaviorMonitorEmailPort(String behaviorMonitorEmailPort) {
        setBehaviorMonitorEmailPortValue(Integer.parseInt(behaviorMonitorEmailPort));
    }

    public static void setBehaviorMonitorEmailPortValue(Integer behaviorMonitorEmailPort) {
        ConfigConstants.behaviorMonitorEmailPort = behaviorMonitorEmailPort;
    }

    public static String getBehaviorMonitorEmailUsername() {
        return behaviorMonitorEmailUsername;
    }

    @Value("${behavior.monitor.email.username:}")
    public void setBehaviorMonitorEmailUsername(String behaviorMonitorEmailUsername) {
        setBehaviorMonitorEmailUsernameValue(behaviorMonitorEmailUsername);
    }

    public static void setBehaviorMonitorEmailUsernameValue(String behaviorMonitorEmailUsername) {
        ConfigConstants.behaviorMonitorEmailUsername = behaviorMonitorEmailUsername;
    }

    public static String getBehaviorMonitorEmailPassword() {
        return behaviorMonitorEmailPassword;
    }

    @Value("${behavior.monitor.email.password:}")
    public void setBehaviorMonitorEmailPassword(String behaviorMonitorEmailPassword) {
        setBehaviorMonitorEmailPasswordValue(behaviorMonitorEmailPassword);
    }

    public static void setBehaviorMonitorEmailPasswordValue(String behaviorMonitorEmailPassword) {
        ConfigConstants.behaviorMonitorEmailPassword = behaviorMonitorEmailPassword;
    }

    public static String getBehaviorMonitorEmailFrom() {
        return behaviorMonitorEmailFrom;
    }

    @Value("${behavior.monitor.email.from:}")
    public void setBehaviorMonitorEmailFrom(String behaviorMonitorEmailFrom) {
        setBehaviorMonitorEmailFromValue(behaviorMonitorEmailFrom);
    }

    public static void setBehaviorMonitorEmailFromValue(String behaviorMonitorEmailFrom) {
        ConfigConstants.behaviorMonitorEmailFrom = behaviorMonitorEmailFrom;
    }

    public static String getBehaviorMonitorEmailTo() {
        return behaviorMonitorEmailTo;
    }

    @Value("${behavior.monitor.email.to:}")
    public void setBehaviorMonitorEmailTo(String behaviorMonitorEmailTo) {
        setBehaviorMonitorEmailToValue(behaviorMonitorEmailTo);
    }

    public static void setBehaviorMonitorEmailToValue(String behaviorMonitorEmailTo) {
        ConfigConstants.behaviorMonitorEmailTo = behaviorMonitorEmailTo;
    }

    public static String getBehaviorMonitorEmailSubject() {
        return behaviorMonitorEmailSubject;
    }

    @Value("${behavior.monitor.email.subject:用户行为异常}")
    public void setBehaviorMonitorEmailSubject(String behaviorMonitorEmailSubject) {
        setBehaviorMonitorEmailSubjectValue(behaviorMonitorEmailSubject);
    }

    public static void setBehaviorMonitorEmailSubjectValue(String behaviorMonitorEmailSubject) {
        ConfigConstants.behaviorMonitorEmailSubject = behaviorMonitorEmailSubject;
    }

    public static String getBehaviorMonitorEmailContent() {
        return behaviorMonitorEmailContent;
    }

    @Value("${behavior.monitor.email.content:IP地址%s在过去%d分钟内访问了%d次系统，超出正常范围，请保持关注。}")
    public void setBehaviorMonitorEmailContent(String behaviorMonitorEmailContent) {
        setBehaviorMonitorEmailContentValue(behaviorMonitorEmailContent);
    }

    public static void setBehaviorMonitorEmailContentValue(String behaviorMonitorEmailContent) {
        ConfigConstants.behaviorMonitorEmailContent = behaviorMonitorEmailContent;
    }

}
