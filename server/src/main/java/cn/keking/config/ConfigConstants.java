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

    /**
     * 以下为用户行为分析设置
     */

    private static Boolean userBehaviorAnalysisEnabled;
    private static int userBehaviorAnalysisPeriodMinutes;
    private static int userBehaviorAnalysisThresholdPerPeriod;
    private static int userBehaviorAnalysisDailyThreshold;
    private static String userBehaviorAnalysisSmtpHost;
    private static int userBehaviorAnalysisSmtpPort;
    private static String userBehaviorAnalysisSmtpUsername;
    private static String userBehaviorAnalysisSmtpPassword;
    private static String userBehaviorAnalysisSmtpFrom;
    private static String userBehaviorAnalysisSmtpTo;
    private static String userBehaviorAnalysisRedisHost;
    private static int userBehaviorAnalysisRedisPort;
    private static String userBehaviorAnalysisRedisPassword;
    private static int userBehaviorAnalysisRedisDatabase;

    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_ENABLED = "false";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_PERIOD_MINUTES = "5";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_THRESHOLD_PER_PERIOD = "100";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_DAILY_THRESHOLD = "1000";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_SMTP_HOST = "";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_SMTP_PORT = "587";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_SMTP_USERNAME = "";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_SMTP_PASSWORD = "";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_SMTP_FROM = "";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_SMTP_TO = "";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_REDIS_HOST = "localhost";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_REDIS_PORT = "6379";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_REDIS_PASSWORD = "";
    public static final String DEFAULT_USER_BEHAVIOR_ANALYSIS_REDIS_DATABASE = "0";

    public static Boolean isUserBehaviorAnalysisEnabled() {
        return userBehaviorAnalysisEnabled;
    }

    @Value("${user.behavior.analysis.enabled:false}")
    public void setUserBehaviorAnalysisEnabled(String userBehaviorAnalysisEnabled) {
        setUserBehaviorAnalysisEnabledValue(Boolean.parseBoolean(userBehaviorAnalysisEnabled));
    }

    public static void setUserBehaviorAnalysisEnabledValue(Boolean userBehaviorAnalysisEnabled) {
        ConfigConstants.userBehaviorAnalysisEnabled = userBehaviorAnalysisEnabled;
    }

    public static int getUserBehaviorAnalysisPeriodMinutes() {
        return userBehaviorAnalysisPeriodMinutes;
    }

    @Value("${user.behavior.analysis.period.minutes:5}")
    public void setUserBehaviorAnalysisPeriodMinutes(int userBehaviorAnalysisPeriodMinutes) {
        setUserBehaviorAnalysisPeriodMinutesValue(userBehaviorAnalysisPeriodMinutes);
    }

    public static void setUserBehaviorAnalysisPeriodMinutesValue(int userBehaviorAnalysisPeriodMinutes) {
        ConfigConstants.userBehaviorAnalysisPeriodMinutes = userBehaviorAnalysisPeriodMinutes;
    }

    public static int getUserBehaviorAnalysisThresholdPerPeriod() {
        return userBehaviorAnalysisThresholdPerPeriod;
    }

    @Value("${user.behavior.analysis.threshold.per.period:100}")
    public void setUserBehaviorAnalysisThresholdPerPeriod(int userBehaviorAnalysisThresholdPerPeriod) {
        setUserBehaviorAnalysisThresholdPerPeriodValue(userBehaviorAnalysisThresholdPerPeriod);
    }

    public static void setUserBehaviorAnalysisThresholdPerPeriodValue(int userBehaviorAnalysisThresholdPerPeriod) {
        ConfigConstants.userBehaviorAnalysisThresholdPerPeriod = userBehaviorAnalysisThresholdPerPeriod;
    }

    public static int getUserBehaviorAnalysisDailyThreshold() {
        return userBehaviorAnalysisDailyThreshold;
    }

    @Value("${user.behavior.analysis.daily.threshold:1000}")
    public void setUserBehaviorAnalysisDailyThreshold(int userBehaviorAnalysisDailyThreshold) {
        setUserBehaviorAnalysisDailyThresholdValue(userBehaviorAnalysisDailyThreshold);
    }

    public static void setUserBehaviorAnalysisDailyThresholdValue(int userBehaviorAnalysisDailyThreshold) {
        ConfigConstants.userBehaviorAnalysisDailyThreshold = userBehaviorAnalysisDailyThreshold;
    }

    public static String getUserBehaviorAnalysisSmtpHost() {
        return userBehaviorAnalysisSmtpHost;
    }

    @Value("${user.behavior.analysis.smtp.host:}")
    public void setUserBehaviorAnalysisSmtpHost(String userBehaviorAnalysisSmtpHost) {
        setUserBehaviorAnalysisSmtpHostValue(userBehaviorAnalysisSmtpHost);
    }

    public static void setUserBehaviorAnalysisSmtpHostValue(String userBehaviorAnalysisSmtpHost) {
        ConfigConstants.userBehaviorAnalysisSmtpHost = userBehaviorAnalysisSmtpHost;
    }

    public static int getUserBehaviorAnalysisSmtpPort() {
        return userBehaviorAnalysisSmtpPort;
    }

    @Value("${user.behavior.analysis.smtp.port:587}")
    public void setUserBehaviorAnalysisSmtpPort(int userBehaviorAnalysisSmtpPort) {
        setUserBehaviorAnalysisSmtpPortValue(userBehaviorAnalysisSmtpPort);
    }

    public static void setUserBehaviorAnalysisSmtpPortValue(int userBehaviorAnalysisSmtpPort) {
        ConfigConstants.userBehaviorAnalysisSmtpPort = userBehaviorAnalysisSmtpPort;
    }

    public static String getUserBehaviorAnalysisSmtpUsername() {
        return userBehaviorAnalysisSmtpUsername;
    }

    @Value("${user.behavior.analysis.smtp.username:}")
    public void setUserBehaviorAnalysisSmtpUsername(String userBehaviorAnalysisSmtpUsername) {
        setUserBehaviorAnalysisSmtpUsernameValue(userBehaviorAnalysisSmtpUsername);
    }

    public static void setUserBehaviorAnalysisSmtpUsernameValue(String userBehaviorAnalysisSmtpUsername) {
        ConfigConstants.userBehaviorAnalysisSmtpUsername = userBehaviorAnalysisSmtpUsername;
    }

    public static String getUserBehaviorAnalysisSmtpPassword() {
        return userBehaviorAnalysisSmtpPassword;
    }

    @Value("${user.behavior.analysis.smtp.password:}")
    public void setUserBehaviorAnalysisSmtpPassword(String userBehaviorAnalysisSmtpPassword) {
        setUserBehaviorAnalysisSmtpPasswordValue(userBehaviorAnalysisSmtpPassword);
    }

    public static void setUserBehaviorAnalysisSmtpPasswordValue(String userBehaviorAnalysisSmtpPassword) {
        ConfigConstants.userBehaviorAnalysisSmtpPassword = userBehaviorAnalysisSmtpPassword;
    }

    public static String getUserBehaviorAnalysisSmtpFrom() {
        return userBehaviorAnalysisSmtpFrom;
    }

    @Value("${user.behavior.analysis.smtp.from:}")
    public void setUserBehaviorAnalysisSmtpFrom(String userBehaviorAnalysisSmtpFrom) {
        setUserBehaviorAnalysisSmtpFromValue(userBehaviorAnalysisSmtpFrom);
    }

    public static void setUserBehaviorAnalysisSmtpFromValue(String userBehaviorAnalysisSmtpFrom) {
        ConfigConstants.userBehaviorAnalysisSmtpFrom = userBehaviorAnalysisSmtpFrom;
    }

    public static String getUserBehaviorAnalysisSmtpTo() {
        return userBehaviorAnalysisSmtpTo;
    }

    @Value("${user.behavior.analysis.smtp.to:}")
    public void setUserBehaviorAnalysisSmtpTo(String userBehaviorAnalysisSmtpTo) {
        setUserBehaviorAnalysisSmtpToValue(userBehaviorAnalysisSmtpTo);
    }

    public static void setUserBehaviorAnalysisSmtpToValue(String userBehaviorAnalysisSmtpTo) {
        ConfigConstants.userBehaviorAnalysisSmtpTo = userBehaviorAnalysisSmtpTo;
    }

    public static String getUserBehaviorAnalysisRedisHost() {
        return userBehaviorAnalysisRedisHost;
    }

    @Value("${user.behavior.analysis.redis.host:localhost}")
    public void setUserBehaviorAnalysisRedisHost(String userBehaviorAnalysisRedisHost) {
        setUserBehaviorAnalysisRedisHostValue(userBehaviorAnalysisRedisHost);
    }

    public static void setUserBehaviorAnalysisRedisHostValue(String userBehaviorAnalysisRedisHost) {
        ConfigConstants.userBehaviorAnalysisRedisHost = userBehaviorAnalysisRedisHost;
    }

    public static int getUserBehaviorAnalysisRedisPort() {
        return userBehaviorAnalysisRedisPort;
    }

    @Value("${user.behavior.analysis.redis.port:6379}")
    public void setUserBehaviorAnalysisRedisPort(int userBehaviorAnalysisRedisPort) {
        setUserBehaviorAnalysisRedisPortValue(userBehaviorAnalysisRedisPort);
    }

    public static void setUserBehaviorAnalysisRedisPortValue(int userBehaviorAnalysisRedisPort) {
        ConfigConstants.userBehaviorAnalysisRedisPort = userBehaviorAnalysisRedisPort;
    }

    public static String getUserBehaviorAnalysisRedisPassword() {
        return userBehaviorAnalysisRedisPassword;
    }

    @Value("${user.behavior.analysis.redis.password:}")
    public void setUserBehaviorAnalysisRedisPassword(String userBehaviorAnalysisRedisPassword) {
        setUserBehaviorAnalysisRedisPasswordValue(userBehaviorAnalysisRedisPassword);
    }

    public static void setUserBehaviorAnalysisRedisPasswordValue(String userBehaviorAnalysisRedisPassword) {
        ConfigConstants.userBehaviorAnalysisRedisPassword = userBehaviorAnalysisRedisPassword;
    }

    public static int getUserBehaviorAnalysisRedisDatabase() {
        return userBehaviorAnalysisRedisDatabase;
    }

    @Value("${user.behavior.analysis.redis.database:0}")
    public void setUserBehaviorAnalysisRedisDatabase(int userBehaviorAnalysisRedisDatabase) {
        setUserBehaviorAnalysisRedisDatabaseValue(userBehaviorAnalysisRedisDatabase);
    }

    public static void setUserBehaviorAnalysisRedisDatabaseValue(int userBehaviorAnalysisRedisDatabase) {
        ConfigConstants.userBehaviorAnalysisRedisDatabase = userBehaviorAnalysisRedisDatabase;
    }

}
