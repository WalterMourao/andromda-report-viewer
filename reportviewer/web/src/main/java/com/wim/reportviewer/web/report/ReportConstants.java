package com.wim.reportviewer.web.report;

public class ReportConstants {

	public static final String PDF_EXTENSION = ".pdf";
	
    public static final String JASPER_EXTENSION = ".jrxml";
    public static final String JASPER_COMPILED_EXTENSION = ".jasper";
    
    public static final String PROPERTY_VIEW_PATTERN = "view.pattern";
    public static final String PROPERTY_VIEW_TAG = "view.tag";
    
	public static final String REPORT_IMAGE_SERVLET = "/com/wim/reportviewer/web/report/loadImage/load-report-image.jsf";
	
	public static final String IMAGES_MAP = "IMAGES_MAP";
	
	public static final String LIST_ALL_REPORTS_JASPER ="listAllReportsJasper";
	
	public final static String MIME_TYPE_PDF = "application/pdf";
	
	public final static String MIME_TYPE_HTML = "text/html";
	
	public final static String MIME_TYPE_FILE_BINARY = "application/octet-stream";
		
	public static final String REPORTS_FOLDER_COMPILED = "/WEB-INF/rpt/bin";

    public static final String REPORTS_FOLDER_SOURCE = "/WEB-INF/rpt/src";
	
    public static final String REPORTS_BASE_DIR_VAR = "BaseDir";
    public static final String REPORTS_LOGO_PATH_VAR = "LogoPath";
    
	public static final String REPORTS_PAGE_PARAMETERS = "/com/wim/reportviewer/report-pages/";
}
